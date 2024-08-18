package simpledb.execution;

import simpledb.common.Catalog;
import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.DbFileIterator;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * SeqScan is an implementation of a sequential scan access method that reads
 * each tuple of a table in no particular order (e.g., as they are laid out on
 * disk).
 */
public class SeqScan implements OpIterator {

    private static final long serialVersionUID = 1L;

    private DbFileIterator tableIterator;

    private String tableName;
    private int tableid;

    private TransactionId tid;
    private Catalog catalog;

    private String tableAlias;

    /**
     * Creates a sequential scan over the specified table as a part of the
     * specified transaction.
     *
     * @param tid        The transaction this scan is running as a part of.
     * @param tableid    the table to scan.
     * @param tableAlias the alias of this table (needed by the parser); the returned
     *                   tupleDesc should have fields with name tableAlias.fieldName
     *                   (note: this class is not responsible for handling a case where
     *                   tableAlias or fieldName are null. It shouldn't crash if they
     *                   are, but the resulting name can be null.fieldName,
     *                   tableAlias.null, or null.null).
     */
    public SeqScan(TransactionId tid, int tableid, String tableAlias) {
        // TODO: some code goes here
        this.catalog = Database.getCatalog();
        this.tid = tid;
        this.tableAlias = tableAlias;
        this.tableid = tableid;
        try {
            this.tableIterator = catalog.getTableById(tableid).getTableContent().iterator(tid);
            this.tableName = catalog.getTableName(tableid);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return return the table name of the table the operator scans. This should
     *         be the actual name of the table in the catalog of the database
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return Return the alias of the table this operator scans.
     */
    public String getAlias() {
        // TODO: some code goes here
        return tableAlias;
    }

    /**
     * Reset the tableid, and tableAlias of this operator.
     *
     * @param tableid    the table to scan.
     * @param tableAlias the alias of this table (needed by the parser); the returned
     *                   tupleDesc should have fields with name tableAlias.fieldName
     *                   (note: this class is not responsible for handling a case where
     *                   tableAlias or fieldName are null. It shouldn't crash if they
     *                   are, but the resulting name can be null.fieldName,
     *                   tableAlias.null, or null.null).
     */
    public void reset(int tableid, String tableAlias) {
        // TODO: some code goes here
        this.tableIterator = catalog.getTableById(tableid).getTableContent().iterator(tid);
        this.tableAlias = tableAlias;
    }

    public SeqScan(TransactionId tid, int tableId) {
        this(tid, tableId, Database.getCatalog().getTableName(tableId));
    }

    public void open() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        this.tableIterator.open();
    }

    /**
     * Returns the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor. This prefix
     * becomes useful when joining tables containing a field(s) with the same
     * name.  The alias and name should be separated with a "." character
     * (e.g., "alias.fieldName").
     *
     * @return the TupleDesc with field names from the underlying HeapFile,
     *         prefixed with the tableAlias string from the constructor.
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        TupleDesc td = catalog.getTableById(this.tableid).getTableSchema();
        int numFields = td.numFields();
        Type[] fieldType = new Type[numFields];
        String[] fieldNameAliased = new String[numFields];
        List<TupleDesc.TDItem> tdItemList = td.getTdItemList();
        for (int i = 0; i < tdItemList.size(); i++) {
            TupleDesc.TDItem tdItem = td.getTdItemList().get(i);
            fieldType[i] = tdItem.fieldType;
            fieldNameAliased[i] = tableAlias + "." + tdItem.fieldName;
        }
        TupleDesc res = new TupleDesc(fieldType, fieldNameAliased);
        return res;
    }

    public boolean hasNext() throws TransactionAbortedException, DbException {
        // TODO: some code goes here
        return this.tableIterator.hasNext();
    }

    public Tuple next() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        // TODO: some code goes here
        return this.tableIterator.next();
    }

    public void close() {
        // TODO: some code goes here
        this.tableIterator.close();
    }

    public void rewind() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // TODO: some code goes here
        this.tableIterator.rewind();
    }
}
