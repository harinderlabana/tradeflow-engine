package com.tradeflow.engine.util;

public class LinkedOrderQueue<E> {

    //Local Node class
    private static class Node<E> {
        E item;
        Node<E> next;

        Node(E item, Node<E> next) {
            this.item = item;
            this.next = next;
        }
    }

    //Set the head and tail equal to null
    private Node<E> head = null;
    private Node<E> tail = null;

    //Set the initial size of the queue
    private int size = 0;

    //Add to queue
    public synchronized void enqueue(E item) {
        //Initialize a new node with the item and pointing to null
        Node<E> newNode = new Node<>(item, null);

        //Check if the list is empty
        if (head == null) {
            head = newNode;
        } else {
            //If the list is not empty, point to the newNode
            tail.next = newNode;
        }
        tail = newNode;     //Set the tail to newNode if the list was empty
        size++;     //Increment the size of the linked list
    }

    public synchronized E dequeue() {
        //Check to see if the list is already empty
        if (head == null) {
            return null;
        }

        //Set result equal to the head item and then move head to the next node.
        E result = head.item;
        head = head.next;

        //If head is pointing to null after the removal, then point tail to null.
        if (head == null) {
            tail = null;
        }
        size--;     //Decrement the size of the linked list
        return result;  //Return the stored item value
    }

    //Check to see if the list is empty
    public boolean isEmpty() {
        return head == null;
    }

    //Returns the size of the list
    public int getSize() {
        return size;
    }
}