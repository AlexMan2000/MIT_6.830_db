package simpledb.storage;

public class Table {

    // 一张表对应一个DbFile文件接口，
//    private TupleDesc tableSchema;
    private DbFile tableContent;  // A list of tuples stored on some pages
    private String tablePrimaryKey;
    private String tableName;

    public Table(DbFile tableContent, String tablePrimaryKey, String tableName) {
        this.tableContent = tableContent;
        this.tableName = tableName;
        this.tablePrimaryKey = tablePrimaryKey;
    }




    public TupleDesc getTableSchema() {
        return tableContent.getTupleDesc();
    }

    public DbFile getTableContent() {
        return tableContent;
    }

    public void setTableContent(DbFile tableContent) {
        this.tableContent = tableContent;
    }

    public String getTablePrimaryKey() {
        return tablePrimaryKey;
    }

    public void setTablePrimaryKey(String tablePrimaryKey) {
        this.tablePrimaryKey = tablePrimaryKey;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
