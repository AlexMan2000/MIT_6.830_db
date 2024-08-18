package simpledb.storage;

public class MRUCache extends RUCache{
    public MRUCache(int numPages) {
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
}
