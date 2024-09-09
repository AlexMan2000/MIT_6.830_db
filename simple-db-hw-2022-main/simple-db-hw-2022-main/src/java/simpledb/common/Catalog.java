package simpledb.common;

import simpledb.storage.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Catalog keeps track of all available tables in the database and their
 * associated schemas.
 * For now, this is a stub catalog that must be populated with tables by a
 * user program before it can be used -- eventually, this should be converted
 * to a catalog that reads a catalog table from disk.
 *
 * @Threadsafe
 */
public class Catalog {

    private Map<Integer, Table> tables;

    /**
     * Constructor.
     * Creates a new, empty catalog.
     */
    public Catalog() {
        // TODO: some code goes here
        // For concurrency safety, preventing race conditions
        this.tables = new ConcurrentHashMap<>();
    }

    /**
     * Add a new table to the catalog.
     * This table's contents are stored in the specified DbFile.
     *
     * @param file      the contents of the table to add;  file.getId() is the identfier of
     *                  this file/tupledesc param for the calls getTupleDesc and getFile
     * @param name      the name of the table -- may be an empty string.  May not be null.  If a name
     *                  conflict exists, use the last table to be added as the table for a given name.
     * @param pkeyField the name of the primary key field
     */
    public void addTable(DbFile file, String name, String pkeyField) {
        // TODO: some code goes here
        if (name == null) {
            System.out.println("The name of the table cannot be null");
            return;
        }

        Table newTable =  new Table(file, pkeyField, name);
        int tableId = file.getId();
        // If id conflicts, id --> old Table(old DbFile) => id ---> new Table(old DbFile)
        if (tables.containsKey(tableId)) {
            tables.put(tableId, newTable);
            return;
        }


        // 文件是由tableid绝对定位的，而不是由name标识的
        // If name conflicts, overwrite old table with "name" with new table instance
        // 给老名字换新文件
        for (Map.Entry<Integer, Table> pair: tables.entrySet()) {
            Table table = pair.getValue();
            if (table.getTableName().equals(name)) {
                tables.remove(pair.getKey());
            }
        }

        // No conflicts happen
        tables.put(tableId, newTable);
    }

    public void addTable(DbFile file, String name) {
        addTable(file, name, "");
    }

    /**
     * Add a new table to the catalog.
     * This table has tuples formatted using the specified TupleDesc and its
     * contents are stored in the specified DbFile.
     *
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *             this file/tupledesc param for the calls getTupleDesc and getFile
     */
    public void addTable(DbFile file) {
        addTable(file, (UUID.randomUUID()).toString());
    }

    /**
     * Return the id of the table with  a specified name,
     *
     * @throws NoSuchElementException if the table doesn't exist
     */
    public int getTableId(String name) throws NoSuchElementException {
        // TODO: some code goes here
        for (Map.Entry<Integer, Table> pair: tables.entrySet()) {
            if (pair.getValue().getTableName().equals(name)) {
                return pair.getKey();
            }
        }

        // If not found
        throw new NoSuchElementException(String.format("No such table named: %s", name));
    }


    public DbFile getTableFile(int tableid) {
        try {
            Table tableById = getTableById(tableid);
            return tableById.getTableContent();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Returns the tuple descriptor (schema) of the specified table
     *
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *                function passed to addTable
     * @throws NoSuchElementException if the table doesn't exist
     */
    public TupleDesc getTupleDesc(int tableid) throws NoSuchElementException {
        // TODO: some code goes here
        return getTableById(tableid).getTableSchema();
    }

    /**
     * Get the Table object by tableid, throw error if there is no table with this id
     * @param tableid
     * @return
     * @throws NoSuchElementException
     */
    public Table getTableById(int tableid) throws NoSuchElementException {
        if (tables.containsKey(tableid)) {
            return tables.get(tableid);
        }

        // If not found
        throw new NoSuchElementException(String.format("No such table with id: %s", tableid));

    }

    /**
     * Get a table by name, throw NoSuchElementException if not found
     * @param name
     * @return
     * @throws NoSuchElementException
     */
    public Table getTableByName(String name) throws NoSuchElementException {
        for (Map.Entry<Integer, Table> pair: tables.entrySet()) {
            if (pair.getValue().getTableName().equals(name)) {
                return pair.getValue();
            }
        }
        // If not found
        throw new NoSuchElementException(String.format("No such table named: %s", name));
    }

    /**
     * Returns the DbFile that can be used to read the contents of the
     * specified table.
     *
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *                function passed to addTable
     */
    public DbFile getDatabaseFile(int tableid) throws NoSuchElementException {
        // TODO: some code goes here
        return getTableById(tableid).getTableContent();
    }

    public String getPrimaryKey(int tableid) {
        // TODO: some code goes here
        return getTableById(tableid).getTablePrimaryKey();
    }

    public Iterator<Integer> tableIdIterator() {
        // TODO: some code goes here
        return tables.keySet().iterator();
    }

    public String getTableName(int tableid) {
        // TODO: some code goes here
        return getTableById(tableid).getTableName();
    }

    /**
     * Delete all tables from the catalog
     */
    public void clear() {
        // TODO: some code goes here
        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();
        for (java.lang.reflect.Field field: fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(this);
                if (value instanceof Map) {
                    ((Map<?, ?>) value).clear();
                }
            } catch (IllegalAccessException e) {
                System.out.println("Error accessing field: " + e.getMessage());
            }
        }
    }

    /**
     * Reads the schema from a file and creates the appropriate tables in the database.
     *
     * @param catalogFile
     */
    public void loadSchema(String catalogFile) {
        String line = "";
        String baseFolder = new File(new File(catalogFile).getAbsolutePath()).getParent();
        try {
            BufferedReader br = new BufferedReader(new FileReader(catalogFile));

            while ((line = br.readLine()) != null) {
                //assume line is of the format name (field type, field type, ...)
                String name = line.substring(0, line.indexOf("(")).trim();
                //System.out.println("TABLE NAME: " + name);
                String fields = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                String[] els = fields.split(",");
                List<String> names = new ArrayList<>();
                List<Type> types = new ArrayList<>();
                String primaryKey = "";
                for (String e : els) {
                    String[] els2 = e.trim().split(" ");
                    names.add(els2[0].trim());
                    if (els2[1].trim().equalsIgnoreCase("int"))
                        types.add(Type.INT_TYPE);
                    else if (els2[1].trim().equalsIgnoreCase("string"))
                        types.add(Type.STRING_TYPE);
                    else {
                        System.out.println("Unknown type " + els2[1]);
                        System.exit(0);
                    }
                    if (els2.length == 3) {
                        if (els2[2].trim().equals("pk"))
                            primaryKey = els2[0].trim();
                        else {
                            System.out.println("Unknown annotation " + els2[2]);
                            System.exit(0);
                        }
                    }
                }
                Type[] typeAr = types.toArray(new Type[0]);
                String[] namesAr = names.toArray(new String[0]);
                TupleDesc t = new TupleDesc(typeAr, namesAr);
                HeapFile tabHf = new HeapFile(new File(baseFolder + "/" + name + ".dat"), t);
                addTable(tabHf, name, primaryKey);
                System.out.println("Added table : " + name + " with schema " + t);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Invalid catalog entry : " + line);
            System.exit(0);
        }
    }
}

