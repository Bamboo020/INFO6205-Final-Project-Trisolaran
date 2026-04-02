package Implementation;

import Interface.HeapInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 最小堆 —— 数组（ArrayList）实现
 * 支持 O(log n) 的 insert / popMin / decreaseKey
 * 使用 HashMap 记录每个元素的堆下标以实现 O(log n) decreaseKey
 *
 * 注意：要求元素唯一（适用于 A* 等路径寻找场景）
 */
public class MinHeap<T extends Comparable<T>> implements HeapInterface<T> {

    private final List<T> heap;           // 从下标 0 开始存储
    private final Map<T, Integer> indexMap; // 元素 -> 堆下标

    public MinHeap() {
        heap = new ArrayList<>();
        indexMap = new HashMap<>();
    }

    // -------- 公开 API --------

    /** 插入元素，O(log n) */
    @Override
    public void insert(T item) {
        heap.add(item);
        int idx = heap.size() - 1;
        indexMap.put(item, idx);
        siftUp(idx);
    }

    /** 查看堆顶最小元素，O(1) */
    @Override
    public T peekMin() {
        if (isEmpty()) throw new NoSuchElementException("堆为空");
        return heap.get(0);
    }

    /** 弹出并返回最小元素，O(log n) */
    @Override
    public T popMin() {
        if (isEmpty()) throw new NoSuchElementException("堆为空");
        T min = heap.get(0);
        int last = heap.size() - 1;
        swap(0, last);
        heap.remove(last);
        indexMap.remove(min);
        if (!isEmpty()) siftDown(0);
        return min;
    }

    /**
     * 将堆中已存在的元素替换为更小的新值，O(log n)
     * item     ：旧值（必须已在堆中）
     * newKey   ：新值（必须 <= 旧值）
     */
    @Override
    public void decreaseKey(T item, T newKey) {
        Integer idx = indexMap.get(item);
        if (idx == null) throw new NoSuchElementException("元素不在堆中: " + item);
        if (newKey.compareTo(item) > 0) throw new IllegalArgumentException("新键值必须 <= 旧键值");
        indexMap.remove(item);
        heap.set(idx, newKey);
        indexMap.put(newKey, idx);
        siftUp(idx);
    }

    /** 堆是否为空 */
    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /** 堆中元素个数 */
    @Override
    public int size() {
        return heap.size();
    }

    // -------- 私有辅助 --------

    private int parent(int i) { return (i - 1) / 2; }
    private int left(int i)   { return 2 * i + 1; }
    private int right(int i)  { return 2 * i + 2; }

    private void swap(int i, int j) {
        T a = heap.get(i);
        T b = heap.get(j);
        heap.set(i, b);
        heap.set(j, a);
        indexMap.put(b, i);
        indexMap.put(a, j);
    }

    /** 上浮：插入 / decreaseKey 后恢复堆序 */
    private void siftUp(int i) {
        while (i > 0) {
            int p = parent(i);
            if (heap.get(i).compareTo(heap.get(p)) < 0) {
                swap(i, p);
                i = p;
            } else {
                break;
            }
        }
    }

    /** 下沉：弹出堆顶后恢复堆序 */
    private void siftDown(int i) {
        int size = heap.size();
        while (true) {
            int smallest = i;
            int l = left(i);
            int r = right(i);
            if (l < size && heap.get(l).compareTo(heap.get(smallest)) < 0) smallest = l;
            if (r < size && heap.get(r).compareTo(heap.get(smallest)) < 0) smallest = r;
            if (smallest != i) {
                swap(i, smallest);
                i = smallest;
            } else {
                break;
            }
        }
    }
}