package Interface;

import java.util.Iterator;

/**
 * SetInterface —— 自定义 Set 接口
 * 替代 java.util.Set，提供不重复集合的基本操作
 */
public interface SetInterface<T> extends Iterable<T> {

    /** 添加元素，若已存在返回 false */
    boolean add(T element);

    /** 删除元素，返回是否成功 */
    boolean remove(Object element);

    /** 是否包含指定元素 */
    boolean contains(Object element);

    /** 返回元素个数 */
    int size();

    /** 是否为空 */
    boolean isEmpty();

    /** 清空所有元素 */
    void clear();

    /** 返回迭代器 */
    Iterator<T> iterator();
}