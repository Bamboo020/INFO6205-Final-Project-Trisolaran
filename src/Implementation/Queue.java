package Implementation;

public class Queue<T> extends HeapPriorityQueue<Queue.QueueEntry<T>> {

    static class QueueEntry<T> implements Comparable<QueueEntry<T>> {
        final T value;
        final int order;

        QueueEntry(T value, int order) {
            this.value = value;
            this.order = order;
        }

        @Override
        public int compareTo(QueueEntry<T> other) {
            return Integer.compare(this.order, other.order);
        }
    }

    private int counter;

    public Queue() {
        super();
        this.counter = 0;
    }

    public void offer(T element) {
        enqueue(new QueueEntry<>(element, counter++));
    }

    public T poll() {
        return dequeue().value;
    }

    public T peekValue() {
        return peek().value;
    }

    public void add(T element) {
        offer(element);
    }
}