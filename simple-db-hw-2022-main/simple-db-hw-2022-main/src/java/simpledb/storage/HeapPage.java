package simpledb.storage;

import simpledb.common.Catalog;
import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.transaction.TransactionId;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Each instance of HeapPage stores data for one page of HeapFiles and
 * implements the Page interface that is used by BufferPool.
 *
 * @see HeapFile
 * @see BufferPool
 */
public class HeapPage implements Page {

    // Default Page Size
    final int DEFAULT_PAGE_SIZE = 4096;


    // Metadata about the page and table that it is associated with
    final HeapPageId pid;

    // Metadata about the page itself
    final TupleDesc td;
    final byte[] header;

    // Actual data
    final Tuple[] tuples;
    final int numSlots;

    byte[] oldData;
    private final Byte oldDataLock = (byte) 0;

    private List<TransactionId> dirtyTransactions = new ArrayList<>();

    /**
     * Create a HeapPage from a set of bytes of data read from disk.
     * The format of a HeapPage is a set of header bytes indicating
     * the slots of the page that are in use, some number of tuple slots.
     * Specifically, the number of tuples is equal to: <p>
     * floor((BufferPool.getPageSize()*8) / (tuple size * 8 + 1))
     * <p> where tuple size is the size of tuples in this
     * database table, which can be determined via {@link Catalog#getTupleDesc}.
     * The number of 8-bit header words is equal to:
     * <p>
     * ceiling(no. tuple slots / 8)
     * <p>
     *
     * @see Database#getCatalog
     * @see Catalog#getTupleDesc
     * @see BufferPool#getPageSize()
     */
    public HeapPage(HeapPageId id, byte[] data) throws IOException {
        this.pid = id;
        this.td = Database.getCatalog().getTupleDesc(id.getTableId());
        this.numSlots = getNumTuples();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        // allocate and read the header slots of this page
        header = new byte[getHeaderSize()];
        for (int i = 0; i < header.length; i++)
            header[i] = dis.readByte();

        tuples = new Tuple[numSlots];
        try {
            // allocate and read the actual records of this page
            for (int i = 0; i < tuples.length; i++)
                tuples[i] = readNextTuple(dis, i);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        dis.close();

        setBeforeImage();
    }

    /**
     * Retrieve the number of tuples on this page.
     *
     * @return the number of tuples on this page
     */
    private int getNumTuples() {
        // TODO: some code goes here
        /**
         * td.getSize() 获取的是每个tuple的byte size(是fixed size by design), +1 是因为每个slot需要在header中使用
         * 一个bit来标记该slot是否为空
         */
        return Math.floorDiv(BufferPool.getPageSize() * 8 , (td.getSize() * 8 + 1));
    }

    /**
     * Computes the number of bytes in the header of a page in a HeapFile with each tuple occupying tupleSize bytes
     *
     * @return the number of bytes in the header of a page in a HeapFile with each tuple occupying tupleSize bytes
     */
    private int getHeaderSize() {
        // TODO: some code goes here
        /**
         * 每个byte可以表示8个slot的状态，所以公式很显然
         */
        return (int) Math.ceil(((double) getNumTuples()) / 8);
    }

    /**
     * Return a view of this page before it was modified
     * -- used by recovery
     */
    public HeapPage getBeforeImage() {
        try {
            byte[] oldDataRef = null;
            synchronized (oldDataLock) {
                oldDataRef = oldData;
            }
            return new HeapPage(pid, oldDataRef);
        } catch (IOException e) {
            e.printStackTrace();
            //should never happen -- we parsed it OK before!
            System.exit(1);
        }
        return null;
    }

    public void setBeforeImage() {
        synchronized (oldDataLock) {
            oldData = getPageData().clone();
        }
    }

    /**
     * @return the PageId associated with this page.
     */
    public HeapPageId getId() {
        // TODO: some code goes here
        return pid;
    }

    /**
     * Suck up tuples from the source file.
     */
    private Tuple readNextTuple(DataInputStream dis, int slotId) throws NoSuchElementException {
        // if associated bit is not set, read forward to the next tuple, and
        // return null.
        if (!isSlotUsed(slotId)) {
            for (int i = 0; i < td.getSize(); i++) {
                try {
                    dis.readByte();
                } catch (IOException e) {
                    throw new NoSuchElementException("error reading empty tuple");
                }
            }
            return null;
        }

        // read fields in the tuple
        Tuple t = new Tuple(td);
        RecordId rid = new RecordId(pid, slotId);
        t.setRecordId(rid);
        try {
            for (int j = 0; j < td.numFields(); j++) {
                /**
                 * 遍历一个tuple(record)的所有字段，根据字段类型从数据流dis中读取相应bytes的数据, 并初始化这个字段
                 */
                Field f = td.getFieldType(j).parse(dis);
                t.setField(j, f);
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            throw new NoSuchElementException("parsing error!");
        }

        return t;
    }

    /**
     * Generates a byte array representing the contents of this page.
     * Used to serialize this page to disk.
     * <p>
     * The invariant here is that it should be possible to pass the byte
     * array generated by getPageData to the HeapPage constructor and
     * have it produce an identical HeapPage object.
     * 将当前的HeapPage对象的byte[] 数据, 按照header, tuples的顺序写入byte[]
     *
     * @return A byte array correspond to the bytes of this page.
     * @see #HeapPage
     */
    public byte[] getPageData() {
        int len = BufferPool.getPageSize();
        /**
         * Output stream here
         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
        DataOutputStream dos = new DataOutputStream(baos);

        // create the header of the page
        for (byte b : header) {
            try {
                dos.writeByte(b);
            } catch (IOException e) {
                // this really shouldn't happen
                e.printStackTrace();
            }
        }

        // create the tuples
        for (int i = 0; i < tuples.length; i++) {

            // empty slot, 用相应字节数的0表示
            if (!isSlotUsed(i)) {
                for (int j = 0; j < td.getSize(); j++) {
                    try {
                        dos.writeByte(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                continue;
            }

            // non-empty slot
            for (int j = 0; j < td.numFields(); j++) {
                Field f = tuples[i].getField(j);
                try {
                    f.serialize(dos);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // padding, 就是一个Page中剩下的内存都用0 填补
        int zerolen = BufferPool.getPageSize() - (header.length + td.getSize() * tuples.length); //- numSlots * td.getSize();
        byte[] zeroes = new byte[zerolen];
        try {
            dos.write(zeroes, 0, zerolen);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            /**
             * 确保所有的buffered bytes都被写入dos中
             */
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * 返回原始数据流
         */
        return baos.toByteArray();
    }

    /**
     * Static method to generate a byte array corresponding to an empty
     * HeapPage.
     * Used to add new, empty pages to the file. Passing the results of
     * this method to the HeapPage constructor will create a HeapPage with
     * no valid tuples in it.
     *
     * @return The returned ByteArray.
     */
    public static byte[] createEmptyPageData() {
        int len = BufferPool.getPageSize();
        return new byte[len]; //all 0
    }

    /**
     * Delete the specified tuple from the page; the corresponding header bit should be updated to reflect
     * that it is no longer stored on any page.
     *
     * @param t The tuple to delete
     * @throws DbException if this tuple is not on this page, or tuple slot is
     *                     already empty.
     */
    public void deleteTuple(Tuple t) throws DbException {
        // TODO: some code goes here
        // not necessary for lab1
        if (getNumUsedSlots() <= 0) {
            throw new DbException("No tuples in this page, cannot delete!");
        }

        RecordId recordId = t.getRecordId();
        if (recordId == null) {
            throw new DbException("RecordId doesn't exist!");
        }

        if (!recordId.getPageId().equals(this.pid)) {
            throw new DbException("Tuple doesn't belong to this page!");
        }

        int slotId = recordId.getTupleNumber();
        if (!isSlotUsed(slotId)) {
            throw new DbException("This slot is not used, tuple is not here!");

        }
        this.tuples[slotId] = null;
        markSlotUsed(slotId, false);

    }


    /**
     * Adds the specified tuple to the page;  the tuple should be updated to reflect
     * that it is now stored on this page.
     *
     * When a tuple is inserted into a page, the tuple may not have been stored on that page before.
     * It could have been either:
     *
     * 1. Newly created: In which case it does not yet have a RecordId.
     * 2. Moved from another page: If the tuple was previously stored on a different page or in a
     * different slot, its RecordId would point to that old page and slot.
     *
     * @param t The tuple to add.
     * @throws DbException if the page is full (no empty slots) or tupledesc
     *                     is mismatch.
     */
    public void insertTuple(Tuple t) throws DbException {
        // TODO: some code goes here
        // not necessary for lab1
        if (getNumUnusedSlots() <= 0) {
            throw new DbException("No empty space in the page, cannot insert tuples!");
        }

        if (!td.equals(t.getTupleDesc())) {
            throw new DbException("Tuple Desc is mismatch");
        }

        for (int i = 0; i < this.numSlots; i++) {
            if (!isSlotUsed(i)) {
                // Update the tuple's record Id so that it reflects its newest page
                t.setRecordId(new RecordId(this.pid, i));
                tuples[i] = t;
                markSlotUsed(i, true);
                return;
            }
        }
    }

    /**
     * Marks this page as dirty/not dirty and record that transaction
     * that did the dirtying
     */
    public void markDirty(boolean dirty, TransactionId tid) {
        // TODO: some code goes here
        // not necessary for lab1
        if (dirty) {
            dirtyTransactions.add(tid);
        } else {
            dirtyTransactions.remove(tid);
        }
    }

    /**
     * Returns the tid of the transaction that last dirtied this page, or null if the page is not dirty
     */
    public TransactionId isDirty() {
        // TODO: some code goes here
        // Not necessary for lab1
        int size = this.dirtyTransactions.size();
        return size == 0? null: this.dirtyTransactions.get(size - 1);
    }

    /**
     * Returns the number of unused (i.e., empty) slots on this page.
     */
    public int getNumUnusedSlots() {
        // TODO: some code goes here
        int usedSlots = 0;
        for (byte b: header) {
            for (int i = 0; i < 8; i++) {
                if ((b & 1) == 1) {
                    usedSlots++;
                }
                b >>>= 1;
            }
        }
        return getNumTuples() - usedSlots;
    }


    public int getNumUsedSlots() {
        int usedSlots = 0;
        for (byte b: header) {
            for (int i = 0; i < 8; i++) {
                if ((b & 1) == 1) {
                    usedSlots++;
                }
                b >>>= 1;
            }
        }
        return usedSlots;
    }

    /**
     * Returns true if associated slot on this page is filled.
     * @param i : slot id, each byte in the header contains 8 slot status bit
     */
    public boolean isSlotUsed(int i) {
        // TODO: some code goes here
        /**
         *  获取slot标记位在第几个 header byte中，比如0x 01100110 1(1)111111 11101101,
         *  如果 i = 15, 则 i 应该在第二个 byte 中的 从右往左数的第七位(括号位)
         */
        byte b = header[i / 8];
        /**
         * 使用位运算获取该 slot id 是否为空，为空则 == 0
         */
        return (1 & (b >>> (i % 8))) == 1;
    }

    /**
     * Abstraction to fill or clear a slot on this page.
     */
    private void markSlotUsed(int i, boolean value) {
        // TODO: some code goes here
        // not necessary for lab1
        if (value) {
            this.header[i / 8] |= 1 << (i % 8);
        } else {
            this.header[i / 8] &= ~(1 << (i % 8));
        }
    }

    /**
     * @return an iterator over all tuples on this page (calling remove on this iterator throws an UnsupportedOperationException)
     *         (note that this iterator shouldn't return tuples in empty slots!)
     */
    public Iterator<Tuple> iterator() {
        // TODO: some code goes here
        List<Tuple> tupleList = new ArrayList<>();
        for (int i = 0; i < getNumTuples(); i++) {
            if (isSlotUsed(i)) {
                tupleList.add(tuples[i]);
            }
        }
        return tupleList.iterator();
    }

}

