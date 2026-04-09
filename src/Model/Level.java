package Model;

/**
 * Represents a single node in the level-selection map.
 * Difficulty is fixed at map-generation time.
 *
 * Scoring: EASY = 20 pts | MEDIUM = 30 pts | HARD = 50 pts
 * The special START node (levelId = 0) has null difficulty and 0 score.
 */
public class Level {

    public enum Difficulty { EASY, MEDIUM, HARD }

    private final int        levelId;
    private final Difficulty difficulty;
    private boolean          completed;

    /** Creates the special START node. */
    public Level() {
        this.levelId    = 0;
        this.difficulty = null;
        this.completed  = false;
    }

    /** Creates a regular level node. */
    public Level(int levelId, Difficulty difficulty) {
        this.levelId    = levelId;
        this.difficulty = difficulty;
        this.completed  = false;
    }

    public boolean isStart() { return levelId == 0; }

    public int getScoreValue() {
        if (difficulty == null) return 0;
        switch (difficulty) {
            case EASY:   return 20;
            case MEDIUM: return 30;
            case HARD:   return 50;
            default:     return 0;
        }
    }

    public int        getLevelId()    { return levelId;    }
    public Difficulty getDifficulty() { return difficulty; }
    public boolean    isCompleted()   { return completed;  }
    public void       setCompleted(boolean b) { this.completed = b; }

    @Override
    public String toString() {
        return isStart() ? "START" : String.format("L%d[%s]", levelId, difficulty);
    }
}
