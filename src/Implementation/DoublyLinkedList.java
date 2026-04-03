package Implementation;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Generic doubly-linked list used as the replay action log.
 * Supports O(1) prepend/append and forward/backward iteration.
 *
 * @param <T> the type of element stored in each node
 */
public class DoublyLinkedList<T> implements Iterable<T> {

    private static class Node<T> {
        T       data;
        Node<T> prev;
        Node<T> next;
        Node(T data) { this.data = data; }
    }

    private Node<T> head;
    private Node<T> tail;
    private int     size;

    public DoublyLinkedList() { head = null; tail = null; size = 0; }

    /** Appends to tail. O(1) */
    public void addLast(T item) {
        Node<T> node = new Node<>(item);
        if (isEmpty()) { head = tail = node; }
        else { node.prev = tail; tail.next = node; tail = node; }
        size++;
    }

    /** Prepends to head. O(1) */
    public void addFirst(T item) {
        Node<T> node = new Node<>(item);
        if (isEmpty()) { head = tail = node; }
        else { node.next = head; head.prev = node; head = node; }
        size++;
    }

    /** Removes and returns the last element. O(1) */
    public T removeLast() {
        if (isEmpty()) throw new NoSuchElementException("List is empty");
        T data = tail.data;
        if (head == tail) { head = tail = null; }
        else { tail = tail.prev; tail.next = null; }
        size--;
        return data;
    }

    /** Removes and returns the first element. O(1) */
    public T removeFirst() {
        if (isEmpty()) throw new NoSuchElementException("List is empty");
        T data = head.data;
        if (head == tail) { head = tail = null; }
        else { head = head.next; head.prev = null; }
        size--;
        return data;
    }

    public T peekFirst() {
        if (isEmpty()) throw new NoSuchElementException("List is empty");
        return head.data;
    }

    public T peekLast() {
        if (isEmpty()) throw new NoSuchElementException("List is empty");
        return tail.data;
    }

    public int     size()    { return size;      }
    public boolean isEmpty() { return size == 0; }

    /** Forward iterator (head → tail). */
    @Override
    public Iterator<T> iterator() { return new ForwardIterator(); }

    /** Backward iterator (tail → head). */
    public Iterator<T> reverseIterator() { return new BackwardIterator(); }

    private class ForwardIterator implements Iterator<T> {
        private Node<T> current = head;
        @Override public boolean hasNext() { return current != null; }
        @Override public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            T data = current.data; current = current.next; return data;
        }
    }

    private class BackwardIterator implements Iterator<T> {
        private Node<T> current = tail;
        @Override public boolean hasNext() { return current != null; }
        @Override public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            T data = current.data; current = current.prev; return data;
        }
    }
}
