package simpledb.endtests;

import simpledb.common.Database;
import simpledb.common.Type;
import simpledb.execution.SeqScan;
import simpledb.storage.HeapFile;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionId;

import java.io.File;

public class TestQuery1 {
    public static void main(String[] args) {


        Type[] types = new Type[]{Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE};
        String[] fieldNames = new String[]{"col1", "col2", "col3"};

        TupleDesc schema = new TupleDesc(types, fieldNames);
        HeapFile table1 = new HeapFile(new File("F:\\Study_Notes_Backup\\Computer_Science\\MIT_6830\\simple-db-hw-2022-main\\simple-db-hw-2022-main\\db_files\\table1.dat"), schema);


        Database.getCatalog().addTable(table1, "table1", "col1");

        TransactionId tid = new TransactionId();
        SeqScan f = new SeqScan(tid, table1.getId());

        try {
            f.open();
            while(f.hasNext()) {
                Tuple tup = f.next();
                System.out.println(tup);
            }
            f.close();
            Database.getBufferPool().transactionComplete(tid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
