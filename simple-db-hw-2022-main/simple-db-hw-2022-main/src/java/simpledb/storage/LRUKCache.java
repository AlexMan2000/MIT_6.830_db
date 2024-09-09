package simpledb.storage;

public class LRUKCache extends RUCache{

    public LRUKCache(int numPages) {
        super(numPages);
    }

    @Override
    public Page evictPage() {
        return null;
    }

    @Override
    public Page loadPage(PageId pid) {
        return null;
    }

    @Override
    public Page getPage(PageId pid) {
        return null;
    }

    @Override
    public void printAllPages() {

    }

    @Override
    public void updatePage(PageId pid, Page page) {

    }

}
