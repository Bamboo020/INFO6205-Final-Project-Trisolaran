package Implementation;

/**
 * Generic general tree for level progression.
 * Each node can have any number of children.
 * Supports pre-order DFS traversal for GUI display.
 *
 * @param <T> the type of data stored in each node
 */
public class Tree<T> {

    private static class Node<T> {
        T data;
        java.util.List<Node<T>> children;

        Node(T data) {
            this.data = data;
            this.children = new java.util.ArrayList<>();
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

    /** Returns the root value, or null if the tree is empty. O(1) */
    public T getRoot() {
        return (root == null) ? null : root.data;
    }

    /**
     * Sets the root. Replaces any existing root and its subtree.
     */
    public void setRoot(T rootData) {
        root = new Node<>(rootData);
        size = 1;
    }

    /**
     * Adds a child to the node whose data equals {@code parent}.
     * DFS to locate the parent. O(n)
     *
     * @throws IllegalArgumentException if parent is not found
     */
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

    /**
     * Returns the direct children of the node whose data equals {@code node}.
     * O(1) return; O(n) to find the node.
     *
     * @throws IllegalArgumentException if node is not found
     */
    public java.util.List<T> getChildren(T node) {
        Node<T> target = findNode(root, node);
        if (target == null) {
            throw new IllegalArgumentException("Node not found: " + node);
        }
        java.util.List<T> result = new java.util.ArrayList<>();
        for (Node<T> child : target.children) {
            result.add(child.data);
        }
        return result;
    }

    /**
     * Returns all values in pre-order (root → children left-to-right). O(n)
     */
    public java.util.List<T> preOrder() {
        java.util.List<T> result = new java.util.ArrayList<>();
        preOrderHelper(root, result);
        return result;
    }

    public int     size()    { return size;      }
    public boolean isEmpty() { return size == 0; }

    private void preOrderHelper(Node<T> node, java.util.List<T> result) {
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
