package simpledb.storage;

public class ClockCache implements Cache{
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

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void clear() {

    }
}
