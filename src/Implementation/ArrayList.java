package Implementation;

import Interface.ListInterface;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ArrayList —— 基于动态数组的 ListInterface 实现
 * 替代 java.util.ArrayList
 * 初始容量 10，空间不足时扩容为 1.5 倍
 */
public class ArrayList<T> implements ListInterface<T> {

    private static final int DEFAULT_CAPACITY = 10;

    private Object[] data;
    private int size;

    public ArrayList() {
        this.data = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }

    public ArrayList(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Negative capacity");
        this.data = new Object[initialCapacity];
        this.size = 0;
    }

    // -------- ListInterface 实现 --------

    @Override
    public void add(T element) {
        ensureCapacity(size + 1);
        data[size++] = element;
    }

    @Override
    public void add(int index, T element) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException("Index: " + index);
        ensureCapacity(size + 1);
        System.arraycopy(data, index, data, index + 1, size - index);
        data[index] = element;
        size++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndex(index);
        return (T) data[index];
    }

    @Override
    @SuppressWarnings("unchecked")
    public T set(int index, T element) {
        checkIndex(index);
        T old = (T) data[index];
        data[index] = element;
        return old;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(int index) {
        checkIndex(index);
        T old = (T) data[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(data, index + 1, data, index, moved);
        }
        data[--size] = null;
        return old;
    }

    @Override
    public boolean removeElement(Object element) {
        int idx = indexOf(element);
        if (idx < 0) return false;
        remove(idx);
        return true;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object element) {
        return indexOf(element) >= 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) data[i] = null;
        size = 0;
    }

    @Override
    public int indexOf(Object element) {
        if (element == null) {
            for (int i = 0; i < size; i++)
                if (data[i] == null) return i;
        } else {
            for (int i = 0; i < size; i++)
                if (element.equals(data[i])) return i;
        }
        return -1;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayListIterator();
    }

    // -------- 额外公开方法 --------

    /** 返回子列表（浅拷贝），范围 [fromIndex, toIndex) */
    @SuppressWarnings("unchecked")
    public ArrayList<T> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex)
            throw new IndexOutOfBoundsException();
        ArrayList<T> sub = new ArrayList<>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            sub.add((T) data[i]);
        }
        return sub;
    }

    // -------- 私有辅助 --------

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > data.length) {
            int newCapacity = data.length + (data.length >> 1); // 1.5x
            if (newCapacity < minCapacity) newCapacity = minCapacity;
            Object[] newData = new Object[newCapacity];
            System.arraycopy(data, 0, newData, 0, size);
            data = newData;
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    // -------- 迭代器 --------

    private class ArrayListIterator implements Iterator<T> {
        private int cursor = 0;

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            return (T) data[cursor++];
        }
    }
}