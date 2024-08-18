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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
    public Page readPage(PageId pid) {
        // TODO: some code goes here
        try {
            RandomAccessFile rfile = new RandomAccessFile(f, "r");
            int pageSize = BufferPool.getPageSize();
            byte[] buffer = new byte[pageSize];
            try {
                // 获取File中的Page的偏移量(in bytes)
                rfile.seek(pid.getPageNumber() * pageSize);
                // Read from the file a page to the buffer
                rfile.read(buffer);
                // Initialize a heapPage with the byte buffer
                HeapPageId heapPageId = new HeapPageId(pid.getTableId(), pid.getPageNumber());
                HeapPage heapPage = new HeapPage(heapPageId, buffer);
                rfile.close();
                return heapPage;
            } catch (IOException e) {
                // If rfile.seek() fails
                throw new IllegalArgumentException();
            }
        } catch(Exception e) {
            // If file opening fails
            throw new IllegalArgumentException();
        }
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // TODO: some code goes here
        return (int) f.length() / BufferPool.getPageSize();
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public List<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // TODO: some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // TODO: some code goes here
        return new HeapFileIterator(tid);
    }


    public class HeapFileIterator implements DbFileIterator {

        private int curPageNo;

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

        public HeapFileIterator(TransactionId tid) {
            this();
            this.tid = tid;
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            // load the starting page in
            curPageNo = 0;
            HeapPage currPage = (HeapPage) bufferPool.getPage(tid, new HeapPageId(getId(), curPageNo), Permissions.READ_ONLY);
            tupleIters.add(currPage);
            currTupleIter = currPage.iterator();
            open = true;
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (!open) {
                return false;
            }
            if (currTupleIter.hasNext()) {
                return true;
            }

            // Careful here
            if (curPageNo < numPages() - 1) {
                // Fetch the next page to iterate tuples
                curPageNo++;
                HeapPage currPage = (HeapPage) bufferPool.getPage(tid, new HeapPageId(getId(), curPageNo), Permissions.READ_ONLY);
                tupleIters.add(currPage);
                currTupleIter = currPage.iterator();
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
            currTupleIter = tupleIters.get(0).iterator();
            curPageNo = 0;
        }

        @Override
        public void close() {
            tupleIters.clear();
            open = false;
        }
    }

}

