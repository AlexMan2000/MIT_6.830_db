package simpledb.storage;

import java.io.Serializable;

/**
 * A RecordId is a reference to a specific tuple on a specific page of a
 * specific table.
 */
public class RecordId implements Serializable {

    private static final long serialVersionUID = 1L;


    /*
    *  Private member field
    * */

    private PageId pid;   // Page Num
    private int tupleno;  // Slot ID

    /**
     * Creates a new RecordId referring to the specified PageId and tuple
     * number.
     *
     * @param pid     the pageid of the page on which the tuple resides
     * @param tupleno the tuple number within the page.
     */
    public RecordId(PageId pid, int tupleno) {
        // TODO: some code goes here
        this.pid = pid;
        this.tupleno = tupleno;
    }

    /**
     * @return the tuple number this RecordId references.
     */
    public int getTupleNumber() {
        // TODO: some code goes here
        return this.tupleno;
    }

    /**
     * @return the page id this RecordId references.
     */
    public PageId getPageId() {
        // TODO: some code goes here
        return this.pid;
    }

    /**
     * Two RecordId objects are considered equal if they represent the same
     * tuple.
     *
     * @return True if this and o represent the same tuple
     */
    @Override
    public boolean equals(Object o) {
        // TODO: some code goes here
        if (o == null) {
            return false;
        }

        if (o.getClass() != getClass()) {
            return false;
        }

        RecordId oRid = (RecordId) o;
        return pid.equals(oRid.pid) && tupleno == oRid.tupleno;
    }

    /**
     * You should implement the hashCode() so that two equal RecordId instances
     * (with respect to equals()) have the same hashCode().
     *
     * @return An int that is the same for equal RecordId objects.
     */
    @Override
    public int hashCode() {
        // TODO: some code goes here
        return pid.hashCode() * 31 + Integer.hashCode(tupleno);
    }

}
