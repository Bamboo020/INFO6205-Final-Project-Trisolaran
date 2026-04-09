package Interface;

import java.util.Iterator;

/**
 * ListInterface —— 自定义 List 接口
 * 替代 java.util.List，提供有序集合的基本操作
 */
public interface ListInterface<T> extends Iterable<T> {

    /** 在末尾添加元素 */
    void add(T element);

    /** 在指定位置插入元素 */
    void add(int index, T element);

    /** 获取指定位置的元素 */
    T get(int index);

    /** 替换指定位置的元素，返回旧值 */
    T set(int index, T element);

    /** 删除指定位置的元素，返回被删除的值 */
    T remove(int index);

    /** 删除第一个匹配的元素，返回是否成功 */
    boolean removeElement(Object element);

    /** 返回元素个数 */
    int size();

    /** 是否为空 */
    boolean isEmpty();

    /** 是否包含指定元素 */
    boolean contains(Object element);

    /** 清空所有元素 */
    void clear();

    /** 返回元素第一次出现的下标，不存在返回 -1 */
    int indexOf(Object element);

    /** 返回迭代器 */
    Iterator<T> iterator();
}