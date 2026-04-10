package Implementation;

import Interface.StackInterface;

public class Stack<T> implements StackInterface<T> {

    private static final int DEFAULT_CAPACITY = 16;

    private Object[] data;
    private int top;

    public Stack() {
        data = new Object[DEFAULT_CAPACITY];
        top = 0;
    }

    @Override
    public boolean isEmpty() {
        return top == 0;
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public void push(T item) {
        if (top == data.length) {
            resize(data.length * 2);
        }
        data[top++] = item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) throw new java.util.EmptyStackException();
        T item = (T) data[--top];
        data[top] = null; // 帮助 GC
        if (top > 0 && top == data.length / 4 && data.length / 2 >= DEFAULT_CAPACITY) {
            resize(data.length / 2);
        }
        return item;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) return null;
        return (T) data[top - 1];
    }

    @Override
    public int size() {
        return top;
    }

    @Override
    public void clear() {
        for (int i = 0; i < top; i++) data[i] = null;
        top = 0;
    }

    private void resize(int newCapacity) {
        Object[] newData = new Object[newCapacity];
        System.arraycopy(data, 0, newData, 0, top);
        data = newData;
    }
}
