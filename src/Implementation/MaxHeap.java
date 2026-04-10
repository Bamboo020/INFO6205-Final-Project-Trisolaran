package Implementation;

/**
 * Array-based generic Max-Heap.
 * Used by the leaderboard to maintain and display the top-K scores.
 * Internal storage uses a plain Object array – 1-indexed.
 *
 * @param <T> element type, must be Comparable
 */
@SuppressWarnings("unchecked")
public class MaxHeap<T extends Comparable<T>> {

    private static final int DEFAULT_CAPACITY = 16;

    private Object[] heap;   // 1-indexed; heap[0] unused
    private int size;

    public MaxHeap() {
        heap = new Object[DEFAULT_CAPACITY + 1];
        size = 0;
    }

    /** Inserts an item. O(log n) */
    public void insert(T item) {
        if (item == null) throw new IllegalArgumentException("Null items not allowed");
        if (size + 1 == heap.length) resize(heap.length * 2);
        heap[++size] = item;
        heapifyUp(size);
    }

    /** Removes and returns the maximum element. O(log n) */
    public T extractMax() {
        if (isEmpty()) throw new java.util.NoSuchElementException("Heap is empty");
        T max = (T) heap[1];
        swap(1, size);
        heap[size--] = null;
        heapifyDown(1);
        return max;
    }

    /** Returns the maximum element without removing it. O(1) */
    /**
     * Returns a snapshot list of the top-{@code k} elements in descending order.
     * Heap state is fully restored after the call. O(k log n)
     */
    public ArrayList<T> topK(int k) {
        k = Math.min(k, size);
        ArrayList<T> extracted = new ArrayList<>(k);
        ArrayList<T> result    = new ArrayList<>(k);

        for (int i = 0; i < k; i++) extracted.add(extractMax());
        for (T item : extracted) { result.add(item); insert(item); }

        return result;
    }

    public int     size()    { return size;      }
    public boolean isEmpty() { return size == 0; }

    private void heapifyUp(int i) {
        while (i > 1) {
            int parent = i / 2;
            if (compare(i, parent) > 0) { swap(i, parent); i = parent; }
            else break;
        }
    }

    private void heapifyDown(int i) {
        while (true) {
            int largest = i;
            int left = 2 * i, right = 2 * i + 1;
            if (left  <= size && compare(left,  largest) > 0) largest = left;
            if (right <= size && compare(right, largest) > 0) largest = right;
            if (largest != i) { swap(i, largest); i = largest; }
            else break;
        }
    }

    private int compare(int a, int b) { return ((T) heap[a]).compareTo((T) heap[b]); }

    private void swap(int a, int b) {
        Object tmp = heap[a]; heap[a] = heap[b]; heap[b] = tmp;
    }

    private void resize(int newCapacity) {
        Object[] newHeap = new Object[newCapacity];
        System.arraycopy(heap, 0, newHeap, 0, size + 1);
        heap = newHeap;
    }
}
