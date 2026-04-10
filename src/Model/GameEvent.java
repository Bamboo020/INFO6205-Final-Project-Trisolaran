package Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

public class GameEvent implements Comparable<GameEvent> {

    public enum EventType {
        MAP_GENERATED,
        NODE_ENTERED,
        NODE_COMPLETED,
        NODE_FAILED,
        GAME_OVER,
        MAZE_STARTED,
        MAZE_RENDER_NEEDED,
        ITEM_PICKUP,
        ITEM_EXPIRE,
        LIVES_CHANGED,
        SCORE_CHANGED,
        BUFF_CHANGED,
        INVENTORY_CHANGED,
        STATUS_UPDATE,
        HINT_EXPIRE,
        PLAYER_LOGGED_IN,
        PLAYER_LOGGED_OUT,
        ENEMY_MOVE,
    }
    private final EventType type;
    private final int       tick;
    private final String    data;

    public GameEvent(EventType type, int tick, String data) {
        this.type = type;
        this.tick = tick;
        this.data = data != null ? data : "";
    }

    public GameEvent(EventType type, String data) { this(type, 0, data); }

    public GameEvent(EventType type) { this(type, 0, ""); }

    public EventType getType() { return type; }
    public int       getTick() { return tick; }
    public String    getData() { return data; }

    @Override
    public int compareTo(GameEvent other) {
        int cmp = Integer.compare(this.tick, other.tick);
        return cmp != 0 ? cmp : Integer.compare(this.type.ordinal(), other.type.ordinal());
    }

    @Override
    public String toString() {
        return "GameEvent{" + type + " @tick=" + tick
                + (data.isEmpty() ? "" : ", data='" + data + "'") + "}";
    }

    public static final class Bus {

        private static final Bus INSTANCE = new Bus();
        public static Bus get() { return INSTANCE; }
        private Bus() {}

        private final EnumMap<EventType, List<Consumer<GameEvent>>> registry =
                new EnumMap<>(EventType.class);

        public void subscribe(EventType type, Consumer<GameEvent> handler) {
            registry.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
        }

        public void publish(GameEvent event) {
            List<Consumer<GameEvent>> handlers = registry.get(event.getType());
            if (handlers == null) return;
            for (Consumer<GameEvent> h : new ArrayList<>(handlers)) {
                h.accept(event);
            }
        }

        public void publish(EventType type) {
            publish(new GameEvent(type));
        }

        public void publish(EventType type, String data) {
            publish(new GameEvent(type, data));
        }

        public void publish(EventType type, int tick, String data) {
            publish(new GameEvent(type, tick, data));
        }

        public void clear() { registry.clear(); }

        public void clear(EventType type) { registry.remove(type); }
    }
}