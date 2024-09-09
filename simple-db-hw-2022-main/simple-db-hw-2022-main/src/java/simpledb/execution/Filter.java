package simpledb.execution;

import simpledb.common.DbException;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

import java.util.NoSuchElementException;

/**
 * Filter is an operator that implements a relational select.
 */
public class Filter extends Operator {

    private static final long serialVersionUID = 1L;


    private Predicate p;

    // 就是上游的数据流，适用于单表
    private OpIterator child;

    /**
     * Constructor accepts a predicate to apply and a child operator to read
     * tuples to filter from.
     *
     * @param p     The predicate to filter tuples with
     * @param child The child operator
     */
    public Filter(Predicate p, OpIterator child) {
        // TODO: some code goes here
        this.p = p;
        this.child = child;
    }

    public Predicate getPredicate() {
        // TODO: some code goes here
        return this.p;
    }

    /**
     * Get the output tupledesc of this operator, 对于Filter来说，input td和output td应该一致
     * 因为Filter不会在字段列上做过滤
     * @return
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        /**
         * 因为filter本质上只在行数上做缩减，而列数是不会改变的，所以直接将其上游的operator
         */
        return this.child.getTupleDesc();
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // TODO: some code goes here
        /**
         * 调用所有上游operator的open()方法, 确保其类中的open字段均被设置为true, 否则
         * 所有上游operator的hasNext()/next()都无法被调用，也就无法获取数据
         */
        this.child.open();
        /**
         * 调用本类的open
         */
        super.open(); // 由于继承关系，调用父类的open保证本类中的this.open = true
    }

    public void close() {
        // TODO: some code goes here
        // 调用本类的close()保证this.open = false;
        super.close();
        /**
         * 调用所有上游operator的close()方法, 确保上游不再有数据产生
         */
        this.child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        this.child.rewind(); // 只要使上游重新iterate, 在本operator中就可以rewinc()
    }

    /**
     * AbstractDbIterator.readNext implementation. Iterates over tuples from the
     * child operator, applying the predicate to them and returning those that
     * pass the predicate (i.e. for which the Predicate.filter() returns true.)
     *
     * @return The next tuple that passes the filter, or null if there are no
     *         more tuples
     * @see Predicate#filter
     */
    protected Tuple fetchNext() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        // TODO: some code goes here
        while (child.hasNext()) {
            Tuple next = child.next();
            // Apply the Predicate on this tuple to decide whether to
            // pass it to downstream operators
            if (this.p.filter(next)) {
                return next;
            }
        }
        return null;
    }

    @Override
    public OpIterator[] getChildren() {
        // TODO: some code goes here
        return new OpIterator[]{this.child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // TODO: some code goes here
        if (this.child != children[0]) {
            this.child = children[0];
        }
    }

}
