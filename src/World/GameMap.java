package World;

import Implementation.ArrayList;
import Implementation.HashMap;
import Implementation.Tree;
import Model.Level;

import java.util.Random;

/**
 * Randomly generated level-selection map.
 *
 * Structure:
 *   - START node at the root
 *   - 3-4 initial branches from START, each 3-6 nodes long
 *   - 25% fork chance per non-terminal node (only when >= 4 nodes remain)
 *
 * Uses Tree<Level> (Implementation.Tree) as the backing structure.
 */
public class GameMap {

    // Canvas layout constants (used by LevelPanel)
    public static final double CANVAS_W    = 560;
    public static final double CANVAS_H    = 560;
    public static final double NODE_RADIUS = 18;
    public static final double Y_STEP      = 78;
    public static final double Y_START     = 36;

    private Tree<Level>      tree;
    private Level            startNode;
    private HashMap<Level>   parentMap;
    private HashMap<double[]> positions;
    private int              nextId;
    private final Random     random;

    public GameMap() {
        parentMap = new HashMap<>();
        positions = new HashMap<>();
        random    = new Random();
    }

    /**
     * Generates a new random map and computes rendering positions.
     * Safe to call multiple times.
     */
    public void generate() {
        parentMap = new HashMap<>();
        positions = new HashMap<>();
        nextId    = 1;
        tree      = new Tree<>();
        startNode = new Level();            // START node (id=0)
        tree.setRoot(startNode);

        int N = 3 + random.nextInt(2);      // 3 or 4 initial paths
        for (int i = 0; i < N; i++) {
            int pathLen = 3 + random.nextInt(4);   // 3–6 nodes per path
            buildChain(startNode, pathLen);
        }

        computePositions();
    }

    private void buildChain(Level parent, int remaining) {
        if (remaining <= 0) return;

        Level node = new Level(nextId++, randomDifficulty());
        tree.addChild(parent, node);
        parentMap.put(node, parent);

        if (remaining > 1) {
            boolean canFork = (remaining >= 4);
            if (canFork && random.nextDouble() < 0.25) {
                buildChain(node, remaining - 1);
                buildChain(node, remaining - 1);
            } else {
                buildChain(node, remaining - 1);
            }
        }
    }

    private Level.Difficulty randomDifficulty() {
        switch (random.nextInt(3)) {
            case 0:  return Level.Difficulty.EASY;
            case 1:  return Level.Difficulty.MEDIUM;
            default: return Level.Difficulty.HARD;
        }
    }

    // ──── Position computation ────

    private void computePositions() {
        assignPosition(startNode, 0, CANVAS_W, 0);
    }

    private void assignPosition(Level node, double left, double right, int depth) {
        double cx = (left + right) / 2.0;
        double cy = Y_START + depth * Y_STEP;
        positions.put(node, new double[]{cx, cy});

        ArrayList<Level> children = safeGetChildren(node);
        if (children.isEmpty()) return;

        double sliceW = (right - left) / children.size();
        for (int i = 0; i < children.size(); i++) {
            assignPosition(children.get(i),
                    left + i * sliceW, left + (i + 1) * sliceW, depth + 1);
        }
    }

    // ──── Public API ────

    public Level getStartNode() { return startNode; }

    public ArrayList<Level> getChildren(Level node) { return safeGetChildren(node); }

    public Level getParent(Level node) { return (Level) parentMap.get(node); }

    public double[] getPosition(Level node) { return (double[]) positions.get(node); }

    public ArrayList<Level> getAllNodes() { return tree.preOrder(); }

    public boolean isLeaf(Level node) { return safeGetChildren(node).isEmpty(); }

    private ArrayList<Level> safeGetChildren(Level node) {
        try { return tree.getChildren(node); }
        catch (Exception e) { return new ArrayList<>(); }
    }
}
