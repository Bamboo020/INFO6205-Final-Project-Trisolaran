package Implementation;

/**
 * Queue —— 基于 HeapPriorityQueue 的 FIFO 队列
 * 继承自 HeapPriorityQueue，通过插入顺序计数器实现先进先出
 *
 * 内部将元素包装为 QueueEntry，以插入顺序作为优先级，
 * 使得最早入队的元素（计数器最小）最先被取出，从而实现 FIFO 语义。
 */
public class Queue<T> extends HeapPriorityQueue<Queue.QueueEntry<T>> {

    /**
     * 队列条目：包装实际元素并附带插入顺序
     * 按 order 升序排列（最小 order 最先出队 = FIFO）
     */
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

    /** 入队（FIFO 语义） */
    public void offer(T element) {
        enqueue(new QueueEntry<>(element, counter++));
    }

    /** 出队（FIFO 语义），返回最早入队的元素 */
    public T poll() {
        return dequeue().value;
    }

    /** 查看队首元素（不移除） */
    public T peekValue() {
        return peek().value;
    }

    /** 添加元素（offer 的别名，方便使用） */
    public void add(T element) {
        offer(element);
    }
}