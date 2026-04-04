package Implementation;

import Interface.SetInterface;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * HashSet —— 基于自定义 HashMap 的 SetInterface 实现
 * 替代 java.util.HashSet
 * 内部使用 HashMap<Object> 存储，value 统一为 PRESENT 占位符
 * 额外维护一个 ArrayList 用于支持迭代
 */
public class HashSet<T> implements SetInterface<T> {

    private static final Object PRESENT = new Object();

    private final HashMap<Object> map;
    private final ArrayList<T> elements; // 维护元素列表以支持迭代

    public HashSet() {
        this.map = new HashMap<>();
        this.elements = new ArrayList<>();
    }

    @Override
    public boolean add(T element) {
        if (map.containsKeyObj(element)) {
            return false;
        }
        map.put(element, PRESENT);
        elements.add(element);
        return true;
    }

    @Override
    public boolean remove(Object element) {
        if (!map.containsKeyObj(element)) {
            return false;
        }
        map.remove(element);
        elements.removeElement(element);
        return true;
    }

    @Override
    public boolean contains(Object element) {
        return map.containsKeyObj(element);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.size() == 0;
    }

    @Override
    public void clear() {
        // 重新创建内部结构（HashMap 无 clear 方法）
        // 通过逐个 remove 实现清空
        ArrayList<T> copy = new ArrayList<>();
        for (T e : elements) {
            copy.add(e);
        }
        for (int i = 0; i < copy.size(); i++) {
            map.remove(copy.get(i));
        }
        elements.clear();
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }
}