package Implementation;

import Interface.SetInterface;

import java.util.Iterator;

public class HashSet<T> implements SetInterface<T> {

    private static final Object PRESENT = new Object();

    private final HashMap<Object> map;
    private final ArrayList<T> elements;

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