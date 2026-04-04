package Implementation;

import Interface.GraphInterface;

/**
 * 无向图 —— 邻接表实现
 * 顶点用自定义 HashMap 索引，每条边存储两次（双向）
 */
public class Graph<T> implements GraphInterface<T> {

    private final HashMap<java.util.List<T>> adjMap;
    private int edgeCount;

    public Graph() {
        adjMap = new HashMap<>();
        edgeCount = 0;
    }

    /** 添加顶点（若已存在则忽略） */
    @Override
    public void addVertex(T vertex) {
        adjMap.putIfAbsent(vertex, new java.util.LinkedList<>());
    }

    /** 添加无向边；若顶点不存在则自动添加 */
    @Override
    public void addEdge(T start, T end) {
        addVertex(start);
        addVertex(end);
        adjMap.get(start).add(end);
        adjMap.get(end).add(start);
        edgeCount++;
    }

    /** 删除无向边（两个方向都删除） */
    @Override
    public void removeEdge(T start, T end) {
        java.util.List<T> startList = adjMap.get(start);
        java.util.List<T> endList   = adjMap.get(end);
        if (startList != null && endList != null) {
            boolean removed = startList.remove(end);
            endList.remove(start);
            if (removed) edgeCount--;
        }
    }

    /** 返回与 start 相邻的所有顶点 */
    @Override
    public Iterable<T> adj(T start) {
        java.util.List<T> neighbors = adjMap.get(start);
        if (neighbors == null) return new java.util.ArrayList<>();
        return neighbors;
    }

    /** 判断顶点是否存在 */
    @Override
    public boolean hasVertex(T vertex) {
        return adjMap.containsKeyObj(vertex);
    }

    /** 顶点数 */
    @Override
    public int getVertex() {
        return adjMap.size();
    }

    /** 边数 */
    @Override
    public int getEdge() {
        return edgeCount;
    }
}
