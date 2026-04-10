package World;

import Implementation.ArrayList;
import Implementation.AVLTree;
import Implementation.MaxHeap;
import Model.GameRecord;
import Model.Level;

public class GameStateController {

    public static final int INITIAL_LIVES = 3;

    private final MaxHeap<Integer>             leaderboard;
    private final AVLTree<Integer, GameRecord> scoreHistory;

    private GameMap currentMap;
    private Level   currentNode;
    private int     accumulatedScore;
    private int     lastPathScore;
    private int     lives;
    private boolean pathActive;

    public GameStateController() {
        leaderboard  = new MaxHeap<>();
        scoreHistory = new AVLTree<>();
        lives            = INITIAL_LIVES;
        accumulatedScore = 0;
        pathActive       = false;
    }

    public void generateNewMap() {
        currentMap = new GameMap();
        currentMap.generate();
        currentNode      = currentMap.getStartNode();
        accumulatedScore = 0;
        lives            = INITIAL_LIVES;
        pathActive       = false;
    }

    public GameMap getCurrentMap() { return currentMap; }

    public boolean moveToNode(Level node) {
        if (currentMap == null || node == null) return false;
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

    public void recordScore(int score, GameRecord record) {
        leaderboard.insert(score);
        scoreHistory.put(score, record);
    }



    public ArrayList<Integer>    getTopK(int k)               { return leaderboard.topK(k);              }
    public ArrayList<GameRecord> getScoreRange(int lo, int hi) { return scoreHistory.rangeQuery(lo, hi); }

    public Level   getCurrentNode()      { return currentNode;      }
    public int     getAccumulatedScore() { return accumulatedScore; }
    public int     getLastPathScore()    { return lastPathScore;    }
    public int     getLives()            { return lives;            }
}