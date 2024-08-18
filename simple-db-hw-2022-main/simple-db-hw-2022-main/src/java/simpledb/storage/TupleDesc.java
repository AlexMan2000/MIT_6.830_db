package simpledb.storage;

import simpledb.common.Type;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {


    private List<TDItem> tdItemList = new ArrayList<>();

    /**
     * A help class to facilitate organizing the information of each field
     */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         */
        public final Type fieldType;

        /**
         * The name of the field
         */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (fieldName == null) {
                return 17;
            }
            for (char c: fieldName.toCharArray()) {
                hash  = 31 * hash + c;
            }
            return hash;
        }
    }

    /**
     * @return An iterator which iterates over all the field TDItems
     *         that are included in this TupleDesc
     */
    public Iterator<TDItem> iterator() {
        // TODO: some code goes here
        return tdItemList.iterator();
    }

    private static final long serialVersionUID = 1L;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr  array specifying the number of and types of fields in this
     *                TupleDesc. It must contain at least one entry.
     * @param fieldAr array specifying the names of the fields. Note that names may
     *                be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // TODO: some code goes here
        Type currType;
        String currName;
        TDItem tdItem;
        for (int i = 0; i < typeAr.length; i++) {
            currType = typeAr[i];
            currName = fieldAr[i];
            tdItem = new TDItem(currType, currName);
            tdItemList.add(tdItem);
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     *
     * @param typeAr array specifying the number of and types of fields in this
     *               TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        // TODO: some code goes here
        Type currType;
        TDItem tdItem;
        for (int i = 0; i < typeAr.length; i++) {
            currType = typeAr[i];
            tdItem = new TDItem(currType, null);
            tdItemList.add(tdItem);
        }
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        // TODO: some code goes here
        return tdItemList.size();
    }


    public List<TDItem> getTdItemList() {
        return tdItemList;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        // TODO: some code goes here
        return tdItemList.get(i).fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i The index of the field to get the type of. It must be a valid
     *          index.
     * @return the type of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        // TODO: some code goes here
        return tdItemList.get(i).fieldType;
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException if no field with a matching name is found.
     */
    public int indexForFieldName(String name) throws NoSuchElementException {
        // TODO: some code goes here
        if (name == null) {
            throw new NoSuchElementException("null is not a valid field name");
        }
        for (int i = 0; i < tdItemList.size(); i++) {
            TDItem currItem = tdItemList.get(i);
            if (currItem.fieldName == null) {
                continue;
            }
            if (currItem.fieldName.equals(name)) {
                return i;
            }
        }
        throw new NoSuchElementException("No Such FieldName");
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
        // TODO: some code goes here
        int sizeInBytes = 0;
        for (TDItem tdItem: tdItemList) {
            sizeInBytes += tdItem.fieldType.getLen();
        }
        return sizeInBytes;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     *
     * @param td1 The TupleDesc with the first fields of the new TupleDesc
     * @param td2 The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        // TODO: some code goes here
        List<TDItem> tdItems1 = td1.tdItemList;
        List<TDItem> tdItems2 = td2.tdItemList;
        Iterator<TDItem> tdItems1Iter = td1.iterator();
        Iterator<TDItem> tdItems2Iter = td2.iterator();

        int mergedLength = tdItems1.size() + tdItems2.size();

        Type[] mergedType = new Type[mergedLength];
        String[] mergedField = new String[mergedLength];

        int i = 0;
        while(tdItems1Iter.hasNext()) {
            TDItem tdItem = tdItems1Iter.next();
            mergedType[i] = tdItem.fieldType;
            mergedField[i] = tdItem.fieldName;
            i++;
        }

        while(tdItems2Iter.hasNext()) {
            TDItem tdItem = tdItems2Iter.next();
            mergedType[i] = tdItem.fieldType;
            mergedField[i] = tdItem.fieldName;
            i++;
        }

        return new TupleDesc(mergedType, mergedField);
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i.
     *
     * @param o the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {
        // TODO: some code goes here
        if (o == null) {
            return false;
        }

        if (o.getClass() != getClass()) {
            return false;
        }

        if (this == o) {
            return true;
        }

        TupleDesc other = (TupleDesc) o;

        if (numFields() != other.numFields()) {
            return false;
        }

        TDItem thisItem;
        TDItem otherItem;

        Iterator<TDItem> thisIter = iterator();
        Iterator<TDItem> otherIter = other.iterator();

        while (thisIter.hasNext() && otherIter.hasNext()) {
            thisItem = thisIter.next();
            otherItem = otherIter.next();
            if (!thisItem.fieldType.equals(otherItem.fieldType)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        int hash = 0;
        for (TDItem tdItem: tdItemList) {
            hash = 31 * hash + tdItem.hashCode();
        }
        return hash;
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     *
     * @return String describing this descriptor.
     */
    public String toString() {
        // TODO: some code goes here
        StringBuilder sb = new StringBuilder();
        while(iterator().hasNext()) {
            TDItem currItem = iterator().next();
            // Is the last item?
            if (iterator().hasNext()) {
                sb.append(currItem.fieldType+"("+currItem.fieldName+"), ");
            } else {
                sb.append(currItem.fieldType+"("+currItem.fieldName+")");
            }

        }

        return sb.toString();
    }
}
