package simpledb.execution;

import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.execution.Aggregator.Op;
import simpledb.storage.Field;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

import java.util.NoSuchElementException;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    private OpIterator child;
    private final int afield;
    private final int gfield;
    private final Aggregator.Op aop;

    private final TupleDesc td;

    private Aggregator aggregator;

    private OpIterator aggIterator;

    /**
     * Constructor.
     * <p>
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     *
     * @param child  The OpIterator that is feeding us tuples.
     * @param afield The column over which we are computing an aggregate.
     * @param gfield The column over which we are grouping the result, or -1 if
     *               there is no grouping
     * @param aop    The aggregation operator to use
     */
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
        // TODO: some code goes here
        this.child = child;
        this.afield = afield;
        this.gfield = gfield;
        this.aop = aop;
        this.td = getTupleDesc();
        Type aFieldType = this.child.getTupleDesc().getFieldType(afield);
        Type gFieldType = null;
        if (gfield != Aggregator.NO_GROUPING) {
            gFieldType = this.child.getTupleDesc().getFieldType(gfield);
        }

        switch(aFieldType) {
            case INT_TYPE -> this.aggregator = new IntegerAggregator(gfield, gFieldType, afield, aop);
            case STRING_TYPE -> this.aggregator = new StringAggregator(gfield, gFieldType, afield, aop);
            default -> throw new RuntimeException(String.format("invalid aggregate type: %s", aFieldType));
        }
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link Aggregator#NO_GROUPING}
     */
    public int groupField() {
        // TODO: some code goes here
        return this.gfield;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     */
    public String groupFieldName() {
        // TODO: some code goes here
        if (groupField() == Aggregator.NO_GROUPING) {
            return null;
        }
        return this.child.getTupleDesc().getFieldName(this.gfield);
    }

    /**
     * @return the aggregate field
     */
    public int aggregateField() {
        // TODO: some code goes here
        return this.afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     */
    public String aggregateFieldName() {
        // TODO: some code goes here
        return this.aop.toString() + " " + this.child.getTupleDesc().getFieldName(this.afield);
    }

    /**
     * @return return the aggregate operator
     */
    public Aggregator.Op aggregateOp() {
        // TODO: some code goes here
        return this.aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
            TransactionAbortedException {
        // TODO: some code goes here
        this.child.open();
        super.open();
        while (this.child.hasNext()) {
            this.aggregator.mergeTupleIntoGroup(this.child.next());
        }
        this.aggIterator = this.aggregator.iterator();
        this.aggIterator.open(); // Don't forget here
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // TODO: some code goes here
        if (this.aggIterator.hasNext()) {
            return this.aggIterator.next();
        }
        return null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        this.aggIterator.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * <p>
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     *
     * e.g. select SUM(column_a) from temp GROUP BY column_b, where the child tuple desc
     *      is (column_a, column_b, column_c, column_d), the output td should be
     *      (sum (column_a), column_b)
     *
     * e.g. select SUM(column_a) from temp, where the child tuple desc
     *      is (column_a, column_b, column_c, column_d), the output td should be
     *      (sum (column_a))
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        if (groupField() == Aggregator.NO_GROUPING) {
            return new TupleDesc(
                    new Type[]{this.child.getTupleDesc().getFieldType(afield)},
                    new String[]{aggregateFieldName()}
            );
        }
        return new TupleDesc(
                new Type[]{
                        this.child.getTupleDesc().getFieldType(gfield),
                        this.child.getTupleDesc().getFieldType(afield),
                },
                new String[]{
                        groupFieldName(),
                        aggregateFieldName()
                }
        );
    }

    public void close() {
        // TODO: some code goes here
        super.close();
        this.child.close();
    }

    @Override
    public OpIterator[] getChildren() {
        // TODO: some code goes here
        return new OpIterator[]{this.child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // TODO: some code goes here
        this.child = children[0];
    }

}
