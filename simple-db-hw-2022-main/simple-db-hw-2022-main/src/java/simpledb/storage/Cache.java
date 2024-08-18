package simpledb.storage;

public interface Cache {

    /**
     * Evict an LRU
     * @return
     */
    Page evictPage();

    Page loadPage(PageId pid);

    Page getPage(PageId pid);

    boolean isFull();


    boolean isEmpty();

    void clear();

}
