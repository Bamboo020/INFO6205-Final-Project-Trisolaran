package Implementation;

import Interface.StackInterface;

/**
 * 栈 —— 数组实现，支持自动扩容
 * 初始容量 16，满时容量翻倍；缩容：元素数量 < 容量/4 时缩减为容量/2
 */
public class Stack<T> implements StackInterface<T> {

    private static final int DEFAULT_CAPACITY = 16;

    private Object[] data;
    private int top; // 下一个可写入位置的下标

    public Stack() {
        data = new Object[DEFAULT_CAPACITY];
        top = 0;
    }

    /** 栈为空返回 true */
    @Override
    public boolean isEmpty() {
        return top == 0;
    }

    /** 数组实现自动扩容，永远不会"满"，始终返回 false */
    @Override
    public boolean isFull() {
        return false;
    }

    /** 压栈，必要时自动扩容 */
    @Override
    public void push(T item) {
        if (top == data.length) {
            resize(data.length * 2);
        }
        data[top++] = item;
    }

    /** 弹栈并返回栈顶元素；栈空时抛出异常 */
    @Override
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) throw new java.util.EmptyStackException();
        T item = (T) data[--top];
        data[top] = null; // 帮助 GC
        // 缩容：元素数量不足容量 1/4 且容量大于默认值时缩减
        if (top > 0 && top == data.length / 4 && data.length / 2 >= DEFAULT_CAPACITY) {
            resize(data.length / 2);
        }
        return item;
    }

    /** 查看栈顶元素但不弹出；栈空返回 null */
    @Override
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) return null;
        return (T) data[top - 1];
    }

    /** 返回当前元素个数 */
    @Override
    public int size() {
        return top;
    }

    /** 清空栈 */
    @Override
    public void clear() {
        for (int i = 0; i < top; i++) data[i] = null;
        top = 0;
    }

    // -------- 私有辅助 --------

    private void resize(int newCapacity) {
        Object[] newData = new Object[newCapacity];
        System.arraycopy(data, 0, newData, 0, top);
        data = newData;
    }
}
