package Implementation;

import Interface.BinarySearchTreeInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * SkillTreeBST - Binary Search Tree implementation for the player's skill system.
 * Skills are ordered by level requirement (key). Players unlock skills as they
 * gain experience and level up.
 *
 * @param <K> key type (level requirement), must be Comparable
 * @param <V> value type (Skill object)
 */
public class SkillTreeBST<K extends Comparable<K>, V> implements BinarySearchTreeInterface<K, V> {

    /** BST Node. */
    private class BSTNode {
        K key;
        V value;
        BSTNode left, right;

        BSTNode(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private BSTNode root;
    private int count;

    public SkillTreeBST() {
        this.root = null;
        this.count = 0;
    }

    @Override
    public void insert(K key, V value) {
        root = insertRec(root, key, value);
    }

    private BSTNode insertRec(BSTNode node, K key, V value) {
        if (node == null) {
            count++;
            return new BSTNode(key, value);
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = insertRec(node.left, key, value);
        } else if (cmp > 0) {
            node.right = insertRec(node.right, key, value);
        } else {
            node.value = value; // Update existing
        }
        return node;
    }

    @Override
    public V search(K key) {
        BSTNode node = searchRec(root, key);
        return (node != null) ? node.value : null;
    }

    private BSTNode searchRec(BSTNode node, K key) {
        if (node == null) return null;
        int cmp = key.compareTo(node.key);
        if (cmp < 0) return searchRec(node.left, key);
        else if (cmp > 0) return searchRec(node.right, key);
        else return node;
    }

    @Override
    public boolean delete(K key) {
        int before = count;
        root = deleteRec(root, key);
        return count < before;
    }

    private BSTNode deleteRec(BSTNode node, K key) {
        if (node == null) return null;
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = deleteRec(node.left, key);
        } else if (cmp > 0) {
            node.right = deleteRec(node.right, key);
        } else {
            count--;
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            // Two children: replace with in-order successor
            BSTNode successor = findMin(node.right);
            node.key = successor.key;
            node.value = successor.value;
            count++; // will be decremented again in recursive delete
            node.right = deleteRec(node.right, successor.key);
        }
        return node;
    }

    private BSTNode findMin(BSTNode node) {
        while (node.left != null) node = node.left;
        return node;
    }

    @Override
    public List<V> inOrderTraversal() {
        List<V> result = new ArrayList<>();
        inOrderRec(root, result);
        return result;
    }

    private void inOrderRec(BSTNode node, List<V> result) {
        if (node == null) return;
        inOrderRec(node.left, result);
        result.add(node.value);
        inOrderRec(node.right, result);
    }

    @Override
    public List<V> getUpTo(K key) {
        List<V> result = new ArrayList<>();
        getUpToRec(root, key, result);
        return result;
    }

    private void getUpToRec(BSTNode node, K key, List<V> result) {
        if (node == null) return;
        getUpToRec(node.left, key, result);
        if (node.key.compareTo(key) <= 0) {
            result.add(node.value);
            getUpToRec(node.right, key, result);
        }
    }

    @Override
    public boolean contains(K key) {
        return search(key) != null;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public K getMin() {
        if (root == null) return null;
        return findMin(root).key;
    }

    @Override
    public K getMax() {
        if (root == null) return null;
        BSTNode node = root;
        while (node.right != null) node = node.right;
        return node.key;
    }
}
