package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import javax.xml.crypto.Data;
import java.io.*;
import java.nio.Buffer;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see HeapPage#HeapPage
 */
public class HeapFile implements DbFile {


    File f;  // Handler, lazy readPage, don't read in the constructor
    TupleDesc td;

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap
     *          file.
     */
    public HeapFile(File f, TupleDesc td) {
        // TODO: some code goes here
        this.f = f;
        this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // TODO: some code goes here
        return f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // TODO: some code goes here
        int count = 17;
        int result = 31 * td.hashCode() + count;
        result = 31 * result + f.getAbsolutePath().hashCode();
        return result;
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        return td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) throws IllegalArgumentException {
        // TODO: some code goes here
        try {
            /**
             * 因为我们要读取某一页的内容，而一个文件里面存放的是多个页的bytes数据，所以我们需要一种
             * 跳跃式的文件读取机制，比如这里的RandomAccessFile使得我们可以按照4096 bytes的步长
             * 来快速索引到不同Page的起始地址。
             */
            RandomAccessFile rfile = new RandomAccessFile(f, "r");
            int pageSize = BufferPool.getPageSize();
            byte[] buffer = new byte[pageSize];
            try {
                // 获取File中的Page的偏移量(in bytes)
                rfile.seek(pid.getPageNumber() * pageSize);
                // Read from the file a page to the buffer, here read() won't throw any error because it can short read,
                // so even if we reach the end of the file, there is no exception thrown
                // But readFully will force the file cursor to move past the EOF and throw IOException
                rfile.readFully(buffer);
                // Initialize a heapPage with the byte buffer
                HeapPageId heapPageId = new HeapPageId(pid.getTableId(), pid.getPageNumber());
                HeapPage heapPage = new HeapPage(heapPageId, buffer);
                rfile.close();
                return heapPage;
            } catch (EOFException e) {
                // If before readFully() reads the required number of bytes the EOF is reached,
                // then it will throw an EOFException.
                throw new IllegalArgumentException(e.getMessage());
            }
        } catch(Exception e) {
            // If file opening fails
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    // see DbFile.java for javadocs

    /**
     * 把一页数据的内容写到文件对应的偏移位置
     * @param page The page to write.  page.getId().pageno() specifies the offset into the file where the page should be written.
     * @throws IOException
     */
    public void writePage(Page page) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
        RandomAccessFile rf;
        try {
            rf = new RandomAccessFile(f, "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
        rf.seek((long) page.getId().getPageNumber() * BufferPool.getPageSize());
        rf.write(page.getPageData());
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // TODO: some code goes here
        return (int) Math.ceil(f.length() * 1.0 / BufferPool.getPageSize());
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        // not necessary for lab1
        HeapPage page;
        for (int pgNo = 0; ; pgNo++) {
            HeapPageId pageId = new HeapPageId(getId(), pgNo);
            try {
                page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
                if (page.getNumUnusedSlots() == 0) {
                    continue;
                }
            } catch (IllegalArgumentException e) {
                // if the current pageNo exceeds the max number of pages of current file, will create new page
                page = new HeapPage(pageId, HeapPage.createEmptyPageData());
                // write the new page to disk
                writePage(page);
                // now this page can be loaded from disk to buffer pool, then from buffer pool to here, page variable
                // is pointing to the memory address, not the disk file
                page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
            }
            break;
        }
        // update the page in the buffer pool since page is a reference, wait until being evicted and flushed to disk
        page.insertTuple(t);

        return Collections.singletonList(page);
    }

    // see DbFile.java for javadocs
    public List<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // TODO: some code goes here
        // not necessary for lab1
        HeapPage page;
        RecordId recordId = t.getRecordId();
        if (getId() != recordId.getPageId().getTableId()) {
            // tuple doesn't belong to this table file
            throw new DbException(String.format("tableId not equals %d != %d", getId(), recordId.getPageId().getTableId()));
        }
        HeapPageId pageId = new HeapPageId(getId(), recordId.getPageId().getPageNumber());

        page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
        page.deleteTuple(t);
        return Collections.singletonList(page);

    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // TODO: some code goes here
        return new HeapFileIterator(tid);
    }


    public class HeapFileIterator implements DbFileIterator {

        private int curIterIdx;

        private Iterator<Tuple> currTupleIter;


        private List<HeapPage> tupleIters; // Used for quick rewind() operation


        private BufferPool bufferPool;

        private TransactionId tid;
        private boolean open;


        public HeapFileIterator() {
            this.tupleIters = new ArrayList<>();
            this.bufferPool = Database.getBufferPool();
            this.open = false;

        }


        public List<HeapPage> getIters() {
            return tupleIters;
        }
        public HeapFileIterator(TransactionId tid) {
            this();
            this.tid = tid;
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            // load the starting page in
            int idx = 0;
            HeapPage currPage;

            /**
             * 找出所有非空的page
             */
            while (idx < numPages()) {
                currPage = (HeapPage) bufferPool.getPage(tid, new HeapPageId(getId(), idx), Permissions.READ_ONLY);
                currTupleIter = currPage.iterator();
                if (currTupleIter.hasNext()) {
                    tupleIters.add(currPage);
                }
                idx++;
            }

            // Initialize
            curIterIdx = 0;
            if (tupleIters.size() == 0) {
                currTupleIter = null;
            } else {
                currTupleIter = tupleIters.get(curIterIdx).iterator();
            }
            open = true;
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (!open) {
                return false;
            }

            if (tupleIters.size() == 0) {
                return false;
            }

            if (currTupleIter.hasNext()) {
                return true;
            }

            if (curIterIdx + 1 < tupleIters.size()) {
                currTupleIter = tupleIters.get(curIterIdx + 1).iterator();
                curIterIdx += 1;
                return currTupleIter.hasNext();
            }

            return false;
        }


        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (hasNext()) {
                return currTupleIter.next();
            }
            // No more elements
            throw new NoSuchElementException();
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            curIterIdx = 0;
            currTupleIter = tupleIters.get(curIterIdx).iterator();

        }

        @Override
        public void close() {
            tupleIters.clear();
            open = false;
        }
    }

}

