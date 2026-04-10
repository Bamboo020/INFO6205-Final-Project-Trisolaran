package Model;

public class GameRecord {

    private final String playerName;
    private final int    score;
    private final int    levelId;
    private final long   completionTimeMs;

    public GameRecord(String playerName, int score, int levelId, long completionTimeMs) {
        this.playerName       = playerName;
        this.score            = score;
        this.levelId          = levelId;
        this.completionTimeMs = completionTimeMs;
    }

    public String getPlayerName()       { return playerName;       }
    public int    getScore()            { return score;            }
    public int    getLevelId()          { return levelId;          }
    public long   getCompletionTimeMs() { return completionTimeMs; }

    @Override
    public String toString() {
        return String.format("GameRecord{player='%s', score=%d, level=%d, time=%dms}",
                playerName, score, levelId, completionTimeMs);
    }
}
