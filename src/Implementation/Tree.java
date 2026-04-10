package Implementation;

public class Tree<T> {

    private static class Node<T> {
        T data;
        ArrayList<Node<T>> children;

        Node(T data) {
            this.data = data;
            this.children = new ArrayList<>();
        }
    }

    private Node<T> root;
    private int size;

    public Tree() {
        root = null;
        size = 0;
    }

    public Tree(T rootData) {
        root = new Node<>(rootData);
        size = 1;
    }

    public T getRoot() {
        return (root == null) ? null : root.data;
    }

    public void setRoot(T rootData) {
        root = new Node<>(rootData);
        size = 1;
    }

    public void addChild(T parent, T child) {
        if (root == null) {
            root = new Node<>(parent);
            size++;
        }
        Node<T> parentNode = findNode(root, parent);
        if (parentNode == null) {
            throw new IllegalArgumentException("Parent node not found: " + parent);
        }
        parentNode.children.add(new Node<>(child));
        size++;
    }

    public ArrayList<T> getChildren(T node) {
        Node<T> target = findNode(root, node);
        if (target == null) {
            throw new IllegalArgumentException("Node not found: " + node);
        }
        ArrayList<T> result = new ArrayList<>();
        for (Node<T> child : target.children) {
            result.add(child.data);
        }
        return result;
    }

    public ArrayList<T> preOrder() {
        ArrayList<T> result = new ArrayList<>();
        preOrderHelper(root, result);
        return result;
    }

    public int     size()    { return size;      }
    public boolean isEmpty() { return size == 0; }

    private void preOrderHelper(Node<T> node, ArrayList<T> result) {
        if (node == null) return;
        result.add(node.data);
        for (Node<T> child : node.children) preOrderHelper(child, result);
    }

    private Node<T> findNode(Node<T> current, T target) {
        if (current == null) return null;
        if (current.data.equals(target)) return current;
        for (Node<T> child : current.children) {
            Node<T> found = findNode(child, target);
            if (found != null) return found;
        }
        return null;
    }
}
