package Interface;

public interface GraphInterFace<T> {
    public void addVertex(T vertex);
    public void addEdge(T start, T end);
    public void removeEdge(T start, T end);
    Iterable<T> adj(T  start);
    public boolean hasVertex(T vertex);
    public int getV();
    public int getE();
}
