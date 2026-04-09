package Implementation;

import Interface.HashMapInterface;

/**
 * HashMap —— 独立链地址法（Separate Chaining）
 * 初始容量 16，负载因子 0.75，超过阈值时容量翻倍扩容
 * T 为值类型，键类型为 Object（依照接口定义）
 */
public class HashMap<T> implements HashMapInterface<T> {

    private static final int    DEFAULT_CAPACITY   = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;

    /** 链表节点 */
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
    private int        threshold; // size 超过此值时扩容

    @SuppressWarnings("unchecked")
    public HashMap() {
        buckets   = new Entry[DEFAULT_CAPACITY];
        size      = 0;
        threshold = (int) (DEFAULT_CAPACITY * DEFAULT_LOAD_FACTOR);
    }

    // -------- 公开 API --------

    /**
     * 插入或更新键值对
     * 若 key 已存在则覆盖旧 value
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(Object key, Object value) {
        int idx = indexFor(key);
        // 搜索是否已存在该 key
        for (Entry<T> e = buckets[idx]; e != null; e = e.next) {
            if (keysEqual(e.key, key)) {
                e.value = (T) value;
                return;
            }
        }
        // 头插法
        buckets[idx] = new Entry<>(key, (T) value, buckets[idx]);
        size++;
        if (size > threshold) resize();
    }

    /**
     * 删除并返回 key 对应的 value；key 不存在时返回 null
     */
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

    /**
     * 返回 key 对应的 value；key 不存在时返回 null
     */
    @Override
    public T get(Object key) {
        int idx = indexFor(key);
        for (Entry<T> e = buckets[idx]; e != null; e = e.next) {
            if (keysEqual(e.key, key)) return e.value;
        }
        return null;
    }

    /**
     * 若 key 不存在，则插入键值对并返回 null；
     * 若 key 已存在，则返回已有的 value，不做更新
     */
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

    /**
     * 判断 key（Object 类型）是否存在
     */
    public boolean containsKeyObj(Object key) {
        int idx = indexFor(key);
        for (Entry<T> e = buckets[idx]; e != null; e = e.next) {
            if (keysEqual(e.key, key)) return true;
        }
        return false;
    }

    /** 返回键值对数量 */
    @Override
    public int size() {
        return size;
    }

    /**
     * 扩容：容量翻倍，所有键值对重新哈希
     * 由 put() 在超过负载因子时自动调用，也可手动调用
     */
    @Override
    @SuppressWarnings("unchecked")
    public void resize() {
        int newCapacity = buckets.length * 2;
        Entry<T>[] newBuckets = new Entry[newCapacity];
        // 重新哈希
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

    // -------- 私有辅助 --------

    /** 计算 key 在当前桶数组中的下标 */
    private int indexFor(Object key) {
        return indexFor(key, buckets.length);
    }

    /** 计算 key 在指定容量桶数组中的下标 */
    private int indexFor(Object key, int capacity) {
        if (key == null) return 0;
        int h = key.hashCode();
        // 扰动：高位混入低位，减少碰撞
        h ^= (h >>> 16);
        return (capacity - 1) & (h < 0 ? -h : h);
    }

    /** 键相等判断：支持 null 键 */
    private boolean keysEqual(Object a, Object b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}