package Model;

/**
 * Represents a single player action appended to the replay doubly-linked list.
 */
public class Action {

    public enum ActionType {
        MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT,
        USE_ITEM, ACTIVATE_BUFF, IDLE
    }

    private final ActionType type;
    private final Object     detail;
    private final long       timestamp;

    public Action(ActionType type) { this(type, null); }

    public Action(ActionType type, Object detail) {
        this.type      = type;
        this.detail    = detail;
        this.timestamp = System.currentTimeMillis();
    }

    public ActionType getType()      { return type;      }
    public Object     getDetail()    { return detail;    }
    public long       getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("Action{type=%s, detail=%s}", type, detail);
    }
}
