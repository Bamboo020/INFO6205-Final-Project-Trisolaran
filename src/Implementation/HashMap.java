package Implementation;

import Interface.HashMapInterface;

public class HashMap<T> implements HashMapInterface<T> {

    private static final int    DEFAULT_CAPACITY   = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;

    private static class Entry<V> {
        Object key;
        V      value;
        Entry<V> next;

        Entry(Object key, V value, Entry<V> next) {
            this.key   = key;
            this.value = value;
            this.next  = next;
        }
    }

    private Entry<T>[] buckets;
    private int        size;
    private int        threshold;

    @SuppressWarnings("unchecked")
    public HashMap() {
        buckets   = new Entry[DEFAULT_CAPACITY];
        size      = 0;
        threshold = (int) (DEFAULT_CAPACITY * DEFAULT_LOAD_FACTOR);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(Object key, Object value) {
        int idx = indexFor(key);
        for (Entry<T> e = buckets[idx]; e != null; e = e.next) {
            if (keysEqual(e.key, key)) {
                e.value = (T) value;
                return;
            }
        }
        buckets[idx] = new Entry<>(key, (T) value, buckets[idx]);
        size++;
        if (size > threshold) resize();
    }

    @Override
    public T remove(Object key) {
        int idx = indexFor(key);
        Entry<T> prev = null;
        Entry<T> curr = buckets[idx];
        while (curr != null) {
            if (keysEqual(curr.key, key)) {
                if (prev == null) buckets[idx] = curr.next;
                else             prev.next     = curr.next;
                size--;
                return curr.value;
            }
            prev = curr;
            curr = curr.next;
        }
        return null;
    }

    @Override
    public T get(Object key) {
        int idx = indexFor(key);
        for (Entry<T> e = buckets[idx]; e != null; e = e.next) {
            if (keysEqual(e.key, key)) return e.value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public T putIfAbsent(Object key, T value) {
        int idx = indexFor(key);
        for (Entry<T> e = buckets[idx]; e != null; e = e.next) {
            if (keysEqual(e.key, key)) return e.value;
        }
        buckets[idx] = new Entry<>(key, value, buckets[idx]);
        size++;
        if (size > threshold) resize();
        return null;
    }

    public boolean containsKeyObj(Object key) {
        int idx = indexFor(key);
        for (Entry<T> e = buckets[idx]; e != null; e = e.next) {
            if (keysEqual(e.key, key)) return true;
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void resize() {
        int newCapacity = buckets.length * 2;
        Entry<T>[] newBuckets = new Entry[newCapacity];
        for (Entry<T> head : buckets) {
            Entry<T> curr = head;
            while (curr != null) {
                Entry<T> next = curr.next;
                int newIdx = indexFor(curr.key, newCapacity);
                curr.next = newBuckets[newIdx];
                newBuckets[newIdx] = curr;
                curr = next;
            }
        }
        buckets   = newBuckets;
        threshold = (int) (newCapacity * DEFAULT_LOAD_FACTOR);
    }

    private int indexFor(Object key) {
        return indexFor(key, buckets.length);
    }

    private int indexFor(Object key, int capacity) {
        if (key == null) return 0;
        int h = key.hashCode();
        h ^= (h >>> 16);
        return (capacity - 1) & (h < 0 ? -h : h);
    }

    private boolean keysEqual(Object a, Object b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}