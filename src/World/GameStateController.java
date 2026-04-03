package World;

import Implementation.AVLTree;
import Implementation.DoublyLinkedList;
import Implementation.MaxHeap;
import Model.Action;
import Model.GameRecord;
import Model.Level;
import Model.MapEvent;

import java.util.List;

/**
 * Central game-state controller for the level-selection map.
 *
 * Scoring rules:
 *   EASY   completed → +20 pts
 *   MEDIUM completed → +30 pts
 *   HARD   completed → +50 pts
 *   Path complete (leaf reached) → submit accumulated score to leaderboard
 *   Lives exhausted before finishing → lose ALL accumulated score
 *
 * Data structures:
 *   - Circular-array MapEvent queue (FR-02-2)
 *   - MaxHeap<Integer>             leaderboard  (FR-05-1)
 *   - AVLTree<Integer, GameRecord> scoreHistory (FR-05-2)
 *   - DoublyLinkedList<Action>     replayLog    (FR-05-3)
 */
public class GameStateController {

    public static final int INITIAL_LIVES = 3;

    // ── Circular-array MapEvent queue ──
    private MapEvent[] queueData;
    private int        queueHead, queueTail, queueCount;

    // ── ADTs ──
    private final MaxHeap<Integer>             leaderboard;
    private final AVLTree<Integer, GameRecord> scoreHistory;
    private final DoublyLinkedList<Action>     replayLog;

    // ── Map & path state ──
    private GameMap currentMap;
    private Level   currentNode;
    private int     accumulatedScore;
    private int     lives;
    private boolean pathActive;

    public GameStateController() {
        queueData  = new MapEvent[16];
        queueHead  = queueTail = queueCount = 0;

        leaderboard  = new MaxHeap<>();
        scoreHistory = new AVLTree<>();
        replayLog    = new DoublyLinkedList<>();

        lives            = INITIAL_LIVES;
        accumulatedScore = 0;
        pathActive       = false;
    }

    // ──── Map generation ────

    public void generateNewMap() {
        currentMap = new GameMap();
        currentMap.generate();
        currentNode      = currentMap.getStartNode();
        accumulatedScore = 0;
        lives            = INITIAL_LIVES;
        pathActive       = false;
        clearReplayLog();
    }

    public GameMap getCurrentMap() { return currentMap; }

    // ──── Player navigation ────

    /**
     * Attempts to move the player to {@code node} (must be a direct child of current).
     * @return true if the move was valid
     */
    public boolean moveToNode(Level node) {
        if (currentMap == null || node == null) return false;
        List<Level> available = currentMap.getChildren(currentNode);
        if (!available.contains(node)) return false;

        currentNode = node;
        pathActive  = true;
        enqueueEvent(new MapEvent(MapEvent.EventType.ANIMATION_FRAME, node));
        return true;
    }

    /**
     * Called when the player successfully clears the maze for currentNode.
     * If the node is a leaf, the path is complete and score is submitted.
     */
    public void onNodeCompleted() {
        if (currentNode == null || currentNode.isStart()) return;
        currentNode.setCompleted(true);
        accumulatedScore += currentNode.getScoreValue();
        logAction(new Action(Action.ActionType.IDLE, "completed:" + currentNode));

        if (currentMap.isLeaf(currentNode)) submitPathScore();
    }

    /**
     * Called when the player fails a maze attempt (loses one life).
     * @return true if all lives were exhausted (game-over condition)
     */
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

    // ──── Path completion & scoring ────

    private void submitPathScore() {
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

    // ──── Leaderboard & history ────

    public void recordScore(int score, GameRecord record) {
        leaderboard.insert(score);
        scoreHistory.put(score, record);
    }

    public void recordScore(int score) {
        recordScore(score, new GameRecord("Player", score, 0, 0L));
    }

    public List<Integer>    getTopK(int k)               { return leaderboard.topK(k);              }
    public List<GameRecord> getScoreRange(int lo, int hi) { return scoreHistory.rangeQuery(lo, hi); }

    // ──── MapEvent queue ────

    public void enqueueEvent(MapEvent e) {
        if (e == null) return;
        if (queueCount == queueData.length) {
            MapEvent[] enlarged = new MapEvent[queueData.length * 2];
            for (int i = 0; i < queueCount; i++)
                enlarged[i] = queueData[(queueHead + i) % queueData.length];
            queueData = enlarged; queueHead = 0; queueTail = queueCount;
        }
        queueData[queueTail] = e;
        queueTail = (queueTail + 1) % queueData.length;
        queueCount++;
    }

    public MapEvent processNextEvent() {
        if (queueCount == 0) return null;
        MapEvent e = queueData[queueHead];
        queueData[queueHead] = null;
        queueHead = (queueHead + 1) % queueData.length;
        queueCount--;
        return e;
    }

    public boolean hasEvents() { return queueCount > 0; }

    // ──── Replay log ────

    public void logAction(Action a) { if (a != null) replayLog.addLast(a); }
    public DoublyLinkedList<Action> getReplayLog() { return replayLog; }
    public void clearReplayLog() { while (!replayLog.isEmpty()) replayLog.removeFirst(); }

    // ──── Getters ────

    public Level   getCurrentNode()      { return currentNode;       }
    public int     getAccumulatedScore() { return accumulatedScore;  }
    public int     getLives()            { return lives;             }
    public boolean isPathActive()        { return pathActive;        }
}
