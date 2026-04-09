package Model;

/**
 * GameEvent - Represents a timed event in the game, processed by the PriorityQueue.
 * Events with lower tick values are processed first (higher priority).
 *
 * 已修改：
 *   - SKILL_EXPIRE 重命名为 ITEM_EXPIRE（道具效果到期）
 *   - 新增 ITEM_PICKUP 事件类型
 */
public class GameEvent implements Comparable<GameEvent> {

    public enum EventType {
        ENEMY_MOVE,         // Enemy patrol moves
        ITEM_EXPIRE,        // A temporary item buff expires (was SKILL_EXPIRE)
        GAME_WIN,           // Player reaches the exit
        HINT_EXPIRE         // Path hint disappears
    }

    private final EventType type;
    private final int tick;        // Game tick when event fires
    private final String data;     // Optional payload (cell key, message, etc.)

    public GameEvent(EventType type, int tick, String data) {
        this.type = type;
        this.tick = tick;
        this.data = data;
    }

    public EventType getType() { return type; }
    public int getTick() { return tick; }
    public String getData() { return data; }

    @Override
    public int compareTo(GameEvent other) {
        int cmp = Integer.compare(this.tick, other.tick);
        if (cmp != 0) return cmp;
        return Integer.compare(this.type.ordinal(), other.type.ordinal());
    }

    @Override
    public String toString() {
        return "Event{" + type + " @tick=" + tick + ", data='" + data + "'}";
    }
}