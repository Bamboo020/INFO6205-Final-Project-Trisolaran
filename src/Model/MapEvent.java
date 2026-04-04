package Model;

/**
 * Represents a single game event dispatched through the level-map circular queue.
 * (Renamed from GameEvent to avoid conflict with Controller.GameEvent.)
 */
public class MapEvent {

    public enum EventType {
        ENEMY_MOVE,
        BUFF_TICK,
        ITEM_RESPAWN,
        ANIMATION_FRAME,
        LEVEL_COMPLETE,
        PLAYER_DAMAGED
    }

    private final EventType type;
    private final Object    payload;
    private final long      timestamp;

    public MapEvent(EventType type) { this(type, null); }

    public MapEvent(EventType type, Object payload) {
        this.type      = type;
        this.payload   = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public EventType getType()      { return type;      }
    public Object    getPayload()   { return payload;   }
    public long      getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("MapEvent{type=%s, payload=%s}", type, payload);
    }
}
