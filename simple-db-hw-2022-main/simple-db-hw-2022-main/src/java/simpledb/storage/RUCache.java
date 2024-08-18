package simpledb.storage;


/**
 * Doubly linked list impl of RU cache(LRU/MRU)
 * Every time a transaction request a page to load, we will insert
 * a node at the front/back of the DLL, whenever we want to evict a
 * page, we delete the node from the back/front of the DLL
 */
public abstract class RUCache implements Cache{

    // Sentinel node
    protected Node front;
    protected Node end;

    protected int capacity;
    protected int size;

    // Every Node is a mapping from pageId to the page.
    protected class Node {
        protected Node prev;
        protected Node next;

        //
        protected PageId pid;
        protected Page page;

        public Node (Node prev, Node next, PageId pid, Page page) {
            this.prev = prev;
            this.next = next;
            this.pid = pid;
            this.page = page;
        }

    }

    // init
    public RUCache(int numPages) {

        this.front = new Node(null, null, null, null);
        this.end = new Node(null, null, null, null);
        this.front.next = this.end;
        this.end.prev = front;
        this.capacity = numPages;
        this.size = 0;
    }


    @Override
    public abstract Page evictPage();

    @Override
    public abstract Page loadPage(PageId pid);

    @Override
    public abstract Page getPage(PageId pid);

    @Override
    public boolean isFull() {
        return this.size == this.capacity;
    }

    @Override
    public boolean isEmpty() {return this.size == 0;}


    @Override
    public void clear() {
        Node nextNode = front.next;
        front.next = null;
        nextNode.prev = null;
        this.size = 0;
    }

}
