package simpledb.storage;

import simpledb.common.Catalog;
import simpledb.common.Database;

public class LRUCache extends RUCache{

    public LRUCache(int numpages) {
        super(numpages);
    }

    @Override
    public synchronized Page evictPage() {
        // Delete the last node
        Node last = this.end.prev;
        Node last2 = this.end.prev.prev;
        last.prev = null;
        last.next = null;
        last2.next = this.end;
        this.end.prev = last2;
        this.size--;
        return last.page;
    }


    /**
     * Allocate a page into the buffer
     * @param pid
     */
    @Override
    public synchronized Page loadPage(PageId pid) {
        if (isFull()) {
            // If cache is full, evict first
            evictPage();
        }
        // Get the tableid associated to this page that you want to read
        int tableId = pid.getTableId();

        // Find the singleton Catalog that maintains a mapping from tableid to Table
        Catalog catalog = Database.getCatalog();
        Table table = catalog.getTableById(tableId);
        DbFile dbFile = table.getTableContent();


        Page page = dbFile.readPage(pid); // Read the byte content from the disk

        // Insert at the front, representing more recent load.
        Node newNode = new Node(this.front, this.front.next, page.getId(), page);
        this.front.next.prev = newNode;
        this.front.next = newNode;
        this.size++;

        return page;
    }


    /**
     * Look up if there is a page hit in the cache, if yes, return the page
     * If not, return null
     * @param pid
     * @return
     */
    @Override
    public synchronized Page getPage(PageId pid) {
        Node curr = this.front.next;
        while (curr != this.end) {
            PageId pageId = curr.page.getId();
            if (pageId.equals(pid)) {
                // Cache hit, move this node to the front
                moveNodeToFront(curr);
                return curr.page;
            }
            curr = curr.next;
        }
        return null;
    }


    public void moveNodeToFront(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;

        node.prev = this.front;
        node.next = this.front.next;
        this.front.next.prev = node;
        this.front.next = node;
    }
}
