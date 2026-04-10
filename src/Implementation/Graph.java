package Implementation;

import Interface.GraphInterface;

public class Graph<T> implements GraphInterface<T> {

    private final HashMap<ArrayList<T>> adjMap;
    private int edgeCount;

    public Graph() {
        adjMap = new HashMap<>();
        edgeCount = 0;
    }

    @Override
    public void addVertex(T vertex) {
        adjMap.putIfAbsent(vertex, new ArrayList<>());
    }

    @Override
    public void addEdge(T start, T end) {
        addVertex(start);
        addVertex(end);
        adjMap.get(start).add(end);
        adjMap.get(end).add(start);
        edgeCount++;
    }

    @Override
    public void removeEdge(T start, T end) {
        ArrayList<T> startList = (ArrayList<T>) adjMap.get(start);
        ArrayList<T> endList   = (ArrayList<T>) adjMap.get(end);
        if (startList != null && endList != null) {
            boolean removed = startList.removeElement(end);
            endList.removeElement(start);
            if (removed) edgeCount--;
        }
    }

    @Override
    public Iterable<T> adj(T start) {
        ArrayList<T> neighbors = (ArrayList<T>) adjMap.get(start);
        if (neighbors == null) return new ArrayList<>();
        return neighbors;
    }
}
