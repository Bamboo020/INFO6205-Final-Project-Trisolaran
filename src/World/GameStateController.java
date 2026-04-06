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
 *
 * Leaderboard sync:
 *   MaxHeap and AVLTree are rebuilt from the DB callback whenever
 *   a query is made, so results always reflect the latest MySQL data.
 */
public class GameStateController {

    public static final int INITIAL_LIVES = 3;

    // ------------------------------------------------------------------ //
    //  DB sync callback – implemented by Main, called before each query
    // ------------------------------------------------------------------ //

    /** Implement this to reload scores from MySQL into MaxHeap + AVLTree. */
    public interface DBSyncCallback {
        void reload(GameStateController gsc);
    }

    private DBSyncCallback dbSyncCallback = null;

    public void setDbSyncCallback(DBSyncCallback cb) {
        this.dbSyncCallback = cb;
    }

    /**
     * Clears and reloads MaxHeap + AVLTree from MySQL via the callback.
     * Called automatically before every leaderboard query.
     */
    public void syncFromDB() {
        if (dbSyncCallback == null) return;
        // Clear both ADTs and reset counter
        leaderboard  = new MaxHeap<>();
        scoreHistory = new AVLTree<>();
        insertCounter = 0;
        // Reload from MySQL
        dbSyncCallback.reload(this);
    }

    // ------------------------------------------------------------------ //
    //  ADTs
    // ------------------------------------------------------------------ //

    private MaxHeap<Integer>             leaderboard;
    private AVLTree<Long, GameRecord>    scoreHistory;
    private int                          insertCounter = 0;  // ensures unique AVLTree keys

    // ------------------------------------------------------------------ //
    //  State
    // ------------------------------------------------------------------ //

    private GameMap currentMap;
    private Level   currentNode;
    private int     accumulatedScore;
    private int     lastPathScore;
    private int     lives;
    private boolean pathActive;
    private String  currentUsername = "Player";   // updated after login

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
        // Only allow moving from START or from a fully completed node
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
                    new GameRecord(currentUsername, accumulatedScore, levelId, 0L));
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
        // Use composite key: score * 1_000_000 + counter to allow duplicate scores
        long key = (long) score * 1_000_000L + insertCounter++;
        scoreHistory.put(key, record);
    }

    public void recordScore(int score) {
        int levelId = (currentNode != null) ? currentNode.getLevelId() : 0;
        recordScore(score, new GameRecord(currentUsername, score, levelId, 0L));
    }

    /**
     * Returns top-K scores.
     * Syncs from MySQL first so results always match the database.
     */
    public ArrayList<Integer> getTopK(int k) {
        syncFromDB();
        return leaderboard.topK(k);
    }

    /**
     * Returns all records whose score falls in [lo, hi].
     * Syncs from MySQL first so results always match the database.
     */
    public ArrayList<GameRecord> getScoreRange(int lo, int hi) {
        syncFromDB();
        // Range: all keys whose score part falls in [lo, hi]
        long keyLo = (long) lo * 1_000_000L;
        long keyHi = (long) hi * 1_000_000L + 999_999L;
        return scoreHistory.rangeQuery(keyLo, keyHi);
    }

    // ------------------------------------------------------------------ //
    //  State getters / setters
    // ------------------------------------------------------------------ //

    public Level   getCurrentNode()      { return currentNode;      }
    public int     getAccumulatedScore() { return accumulatedScore; }
    public int     getLastPathScore()    { return lastPathScore;    }
    public int     getLives()            { return lives;            }
    public boolean isPathActive()        { return pathActive;       }

    /** Call this right after login so scores are recorded with the real username. */
    public void setCurrentUsername(String username) {
        if (username != null && !username.isBlank()) {
            this.currentUsername = username;
        }
    }
}