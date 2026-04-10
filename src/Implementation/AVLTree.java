package Implementation;

public class AVLTree<K extends Comparable<K>, V> {

    class Node {
        K key; V value;
        Node left, right;
        int height;
        Node(K key, V value) { this.key = key; this.value = value; this.height = 1; }
    }

    Node root;
    private int size;

    public void put(K key, V value) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");
        root = put(root, key, value);
    }

    public V get(K key) {
        Node n = root;
        while (n != null) {
            int cmp = key.compareTo(n.key);
            if      (cmp < 0) n = n.left;
            else if (cmp > 0) n = n.right;
            else              return n.value;
        }
        return null;
    }

    public void delete(K key) {
        if (get(key) != null) { root = delete(root, key); size--; }
    }

    public ArrayList<V> rangeQuery(K lo, K hi) {
        ArrayList<V> result = new ArrayList<>();
        rangeQuery(root, lo, hi, result);
        return result;
    }

    public int balanceFactor(Node n) {
        if (n == null) return 0;
        return height(n.left) - height(n.right);
    }

    public int     size()    { return size;      }
    public boolean isEmpty() { return size == 0; }

    Node rotateLeft(Node x) {
        Node y = x.right; Node b = y.left;
        y.left = x; x.right = b;
        updateHeight(x); updateHeight(y);
        return y;
    }

    Node rotateRight(Node y) {
        Node x = y.left; Node b = x.right;
        x.right = y; y.left = b;
        updateHeight(y); updateHeight(x);
        return x;
    }

    private int height(Node n) { return (n == null) ? 0 : n.height; }

    private void updateHeight(Node n) {
        n.height = 1 + Math.max(height(n.left), height(n.right));
    }

    private Node balance(Node n) {
        updateHeight(n);
        int bf = balanceFactor(n);
        if (bf > 1) {
            if (balanceFactor(n.left) < 0) n.left = rotateLeft(n.left);
            return rotateRight(n);
        }
        if (bf < -1) {
            if (balanceFactor(n.right) > 0) n.right = rotateRight(n.right);
            return rotateLeft(n);
        }
        return n;
    }

    private Node put(Node n, K key, V value) {
        if (n == null) { size++; return new Node(key, value); }
        int cmp = key.compareTo(n.key);
        if      (cmp < 0) n.left  = put(n.left,  key, value);
        else if (cmp > 0) n.right = put(n.right, key, value);
        else              n.value = value;
        return balance(n);
    }

    private Node delete(Node n, K key) {
        if (n == null) return null;
        int cmp = key.compareTo(n.key);
        if      (cmp < 0) n.left  = delete(n.left,  key);
        else if (cmp > 0) n.right = delete(n.right, key);
        else {
            if (n.left  == null) return n.right;
            if (n.right == null) return n.left;
            Node successor = minNode(n.right);
            n.key = successor.key; n.value = successor.value;
            n.right = deleteMin(n.right);
        }
        return balance(n);
    }

    private Node minNode(Node n) {
        while (n.left != null) n = n.left;
        return n;
    }

    private Node deleteMin(Node n) {
        if (n.left == null) return n.right;
        n.left = deleteMin(n.left);
        return balance(n);
    }

    private void rangeQuery(Node n, K lo, K hi, ArrayList<V> result) {
        if (n == null) return;
        int cmpLo = lo.compareTo(n.key);
        int cmpHi = hi.compareTo(n.key);
        if (cmpLo < 0)             rangeQuery(n.left,  lo, hi, result);
        if (cmpLo <= 0 && cmpHi >= 0) result.add(n.value);
        if (cmpHi > 0)             rangeQuery(n.right, lo, hi, result);
    }
}
