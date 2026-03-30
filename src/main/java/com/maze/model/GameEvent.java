package com.maze.model;

/**
 * GameEvent - Represents a timed event in the game, processed by the PriorityQueue.
 * Events with lower tick values are processed first (higher priority).
 */
public class GameEvent implements Comparable<GameEvent> {

    public enum EventType {
        COLLECT_COIN,       // Player collects a coin
        ENEMY_MOVE,         // Enemy patrol moves
        SKILL_EXPIRE,       // A temporary skill buff expires
        DOOR_OPEN,          // A locked door opens
        LEVEL_UP,           // Player levels up
        FOG_REVEAL,         // Reveal fog around player
        TRAP_TRIGGER,       // Player triggers a trap
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

    public GameEvent(EventType type, int tick) {
        this(type, tick, "");
    }

    public EventType getType() { return type; }
    public int getTick() { return tick; }
    public String getData() { return data; }

    @Override
    public int compareTo(GameEvent other) {
        // Lower tick = higher priority (processed sooner)
        int cmp = Integer.compare(this.tick, other.tick);
        if (cmp != 0) return cmp;
        // Tie-break by event type ordinal
        return Integer.compare(this.type.ordinal(), other.type.ordinal());
    }

    @Override
    public String toString() {
        return "Event{" + type + " @tick=" + tick + ", data='" + data + "'}";
    }
}
