package World;

import Implementation.ArrayList;
import Implementation.AVLTree;
import Implementation.MaxHeap;
import Model.GameRecord;
import Model.Level;

/**
 * Central controller for game state.
 *
 * Scoring:
 *   EASY   → +20 pts
 *   MEDIUM → +30 pts
 *   HARD   → +50 pts
 *   Path complete (leaf reached) → submit to leaderboard
 *   Lives hit 0                  → lose ALL accumulated score
 */
public class GameStateController {

    public static final int INITIAL_LIVES = 3;

    // ------------------------------------------------------------------ //
    //  ADTs
    // ------------------------------------------------------------------ //

    private final MaxHeap<Integer>             leaderboard;
    private final AVLTree<Integer, GameRecord> scoreHistory;

    // ------------------------------------------------------------------ //
    //  State
    // ------------------------------------------------------------------ //

    private GameMap currentMap;
    private Level   currentNode;
    private int     accumulatedScore;
    private int     lastPathScore;
    private int     lives;
    private boolean pathActive;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public GameStateController() {
        leaderboard  = new MaxHeap<>();
        scoreHistory = new AVLTree<>();
        lives            = INITIAL_LIVES;
        accumulatedScore = 0;
        pathActive       = false;
    }

    // ------------------------------------------------------------------ //
    //  Map generation
    // ------------------------------------------------------------------ //

    public void generateNewMap() {
        currentMap = new GameMap();
        currentMap.generate();
        currentNode      = currentMap.getStartNode();
        accumulatedScore = 0;
        lives            = INITIAL_LIVES;
        pathActive       = false;
    }

    public GameMap getCurrentMap() { return currentMap; }

    // ------------------------------------------------------------------ //
    //  Player navigation
    // ------------------------------------------------------------------ //

    public boolean moveToNode(Level node) {
        if (currentMap == null || node == null) return false;
        // 只有从 START 出发，或当前节点已完成，才能进入子节点
        if (!currentNode.isStart() && !currentNode.isCompleted()) return false;
        ArrayList<Level> available = currentMap.getChildren(currentNode);
        if (!available.contains(node)) return false;
        currentNode = node;
        pathActive  = true;
        return true;
    }

    public void onNodeCompleted() {
        if (currentNode == null || currentNode.isStart()) return;
        currentNode.setCompleted(true);
        accumulatedScore += currentNode.getScoreValue();
        if (currentMap.isLeaf(currentNode)) {
            submitPathScore();
        }
    }

    public boolean onNodeFailed() {
        lives--;
        if (lives <= 0) {
            accumulatedScore = 0;
            pathActive       = false;
            lives            = INITIAL_LIVES;
            resetToStart();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ //
    //  Path scoring
    // ------------------------------------------------------------------ //

    private void submitPathScore() {
        lastPathScore = accumulatedScore;
        if (accumulatedScore > 0) {
            int levelId = currentNode != null ? currentNode.getLevelId() : 0;
            recordScore(accumulatedScore,
                    new GameRecord("Player", accumulatedScore, levelId, 0L));
        }
        accumulatedScore = 0;
        pathActive       = false;
        resetToStart();
    }

    private void resetToStart() {
        if (currentMap != null) currentNode = currentMap.getStartNode();
    }

    // ------------------------------------------------------------------ //
    //  Leaderboard & history
    // ------------------------------------------------------------------ //

    public void recordScore(int score, GameRecord record) {
        leaderboard.insert(score);
        scoreHistory.put(score, record);
    }



    public ArrayList<Integer>    getTopK(int k)               { return leaderboard.topK(k);              }
    public ArrayList<GameRecord> getScoreRange(int lo, int hi) { return scoreHistory.rangeQuery(lo, hi); }

    // ------------------------------------------------------------------ //
    //  State getters
    // ------------------------------------------------------------------ //

    public Level   getCurrentNode()      { return currentNode;      }
    public int     getAccumulatedScore() { return accumulatedScore; }
    public int     getLastPathScore()    { return lastPathScore;    }
    public int     getLives()            { return lives;            }
}