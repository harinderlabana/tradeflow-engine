package com.tradeflow.engine.util;

/**
 * TRADEFLOW ENGINE - CUSTOM DATA STRUCTURE
 * ----------------------------------------
 * A thread-safe, custom implementation of a Singly Linked Queue.
 * * System Design Note: Why build a custom queue instead of using standard java.util Lists?
 * 1. Array-based lists require contiguous memory and trigger costly O(N) array-copy
 * resizing operations when they fill up.
 * 2. In High-Frequency Trading, an unpredictable O(N) resize event during a market
 * spike will lock the ingestion thread and drop WebSocket frames.
 * 3. This Linked structure guarantees strict O(1) constant-time insertions, no matter
 * how large the queue grows, ensuring deterministic performance.
 */
public class LinkedOrderQueue<E> {

    /**
     * Inner Node Class
     * Holds the data payload and a pointer to the next node in memory.
     */
    private static class Node<E> {
        E item;
        Node<E> next;

        Node(E item, Node<E> next) {
            this.item = item;
            this.next = next;
        }
    }

    // Pointers to maintain O(1) access to both ends of the queue
    private Node<E> head = null;
    private Node<E> tail = null;

    // Internal size tracker to avoid O(N) traversal counting
    private int size = 0;

    /**
     * ENQUEUE (PRODUCER OPERATION)
     * ----------------------------
     * Appends an item to the tail of the queue.
     * Time Complexity: O(1) Constant Time.
     * * Concurrency: The 'synchronized' keyword ensures that if the WebSocket thread
     * is enqueuing while the Database worker thread is dequeuing, they do not
     * corrupt the memory pointers (Race Condition prevention).
     */
    public synchronized void enqueue(E item) {
        // Initialize a new node with the item, pointing to null (as it is the new tail)
        Node<E> newNode = new Node<>(item, null);

        if (head == null) {
            // First item in an empty queue becomes both head and tail
            head = newNode;
        } else {
            // Existing tail points forward to the new node
            tail.next = newNode;
        }

        // Advance the tail pointer to the newly inserted node
        tail = newNode;
        size++;
    }

    /**
     * DEQUEUE (CONSUMER OPERATION)
     * ----------------------------
     * Removes and returns the item at the head of the queue.
     * Time Complexity: O(1) Constant Time.
     */
    public synchronized E dequeue() {
        // Fast-fail if the queue is empty
        if (head == null) {
            return null;
        }

        // Capture the data payload from the current head
        E result = head.item;

        // Sever the old head and move the head pointer to the next node.
        // The old head is now abandoned in memory and will be cleaned up by the Java Garbage Collector.
        head = head.next;

        // If we just removed the absolute last item, the tail must also be reset to null
        if (head == null) {
            tail = null;
        }

        size--;
        return result;
    }

    /**
     * State Checks
     * Time Complexity: O(1)
     */
    public boolean isEmpty() {
        return head == null;
    }

    /**
     * TELEMETRY HOOK
     * Returns the cached size variable.
     * Because we track size during enqueue/dequeue, we avoid having to traverse
     * the linked list with a while-loop (which would be O(N) time).
     */
    public int getSize() {
        return size;
    }
}