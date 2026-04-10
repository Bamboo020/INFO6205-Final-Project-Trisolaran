package Implementation;

import Interface.HeapInterface;

public class MinHeap<T extends Comparable<T>> implements HeapInterface<T> {

    private final ArrayList<T> heap;             // 从下标 0 开始存储
    private final HashMap<Integer> indexMap;     // 元素 -> 堆下标

    public MinHeap() {
        heap = new ArrayList<>();
        indexMap = new HashMap<>();
    }

    @Override
    public void insert(T item) {
        heap.add(item);
        int idx = heap.size() - 1;
        indexMap.put(item, idx);
        siftUp(idx);
    }

    @Override
    public T popMin() {
        if (isEmpty()) throw new java.util.NoSuchElementException("堆为空");
        T min = heap.get(0);
        int last = heap.size() - 1;
        swap(0, last);
        heap.remove(last);
        indexMap.remove(min);
        if (!isEmpty()) siftDown(0);
        return min;
    }

    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    @Override
    public int size() {
        return heap.size();
    }

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