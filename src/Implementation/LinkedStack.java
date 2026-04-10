package Implementation;

import Interface.StackInterface;

import java.util.NoSuchElementException;

public class LinkedStack<T> implements StackInterface<T> {
    private static class Node<T> {
        T data;
        Node<T> next;

        Node(T data, Node<T> next) {
            this.data = data;
            this.next = next;
        }
    }

    private Node<T> top;
    private int count;

    public LinkedStack() {
        this.top = null;
        this.count = 0;
    }

    @Override
    public void push(T element) {
        top = new Node<>(element, top);
        count++;
    }

    @Override
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }
        T data = top.data;
        top = top.next;
        count--;
        return data;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }
        return top.data;
    }

    @Override
    public boolean isEmpty() {
        return top == null;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public void clear() {
        top = null;
        count = 0;
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Stack[top -> ");
        Node<T> current = top;
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) sb.append(" -> ");
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}
