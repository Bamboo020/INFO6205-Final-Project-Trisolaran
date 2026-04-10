package Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * GameEvent — 游戏事件载体 + 内嵌全局事件总线。
 *
 * ═══════════════════════════════════════════════════════════
 *  架构规则（统一通信方式）
 * ═══════════════════════════════════════════════════════════
 *  ✓ 所有跨组件通信一律通过 GameEvent.Bus.get() 发布/订阅
 *  ✓ 逻辑层只 publish，不直接调用 UI 方法
 *  ✓ UI 面板只 subscribe，在构造函数里注册一次，自动响应
 *  ✗ 禁止在面板外调用 panel.refresh() / panel.setXxx()
 *
 *  发布示例（逻辑层）：
 *    GameEvent.Bus.get().publish(EventType.LIVES_CHANGED, "2");
 *    GameEvent.Bus.get().publish(EventType.MAP_GENERATED);
 *
 *  订阅示例（面板构造函数）：
 *    GameEvent.Bus.get().subscribe(EventType.LIVES_CHANGED,
 *        e -> Platform.runLater(this::refreshLives));
 * ═══════════════════════════════════════════════════════════
 */
public class GameEvent implements Comparable<GameEvent> {

    // ------------------------------------------------------------------ //
    //  Event types
    // ------------------------------------------------------------------ //

    public enum EventType {

        /* ── 游戏流程 ──────────────────────────────────────────────── */
        /** 新地图已生成。data = "" */
        MAP_GENERATED,
        /** 玩家进入关卡节点。data = node.toString() */
        NODE_ENTERED,
        /** 当前迷宫通关。data = String.valueOf(pathScore)，叶节点时 pathScore > 0 */
        NODE_COMPLETED,
        /** 当前迷宫失败（被抓 / 强制失败）。data = "" */
        NODE_FAILED,
        /** 生命耗尽，游戏结束。data = "" */
        GAME_OVER,

        /* ── 迷宫内 ────────────────────────────────────────────────── */
        /** 迷宫初始化完成，开始渲染。data = "" */
        MAZE_STARTED,
        /** 玩家/敌人移动后需重绘画布。data = "" */
        MAZE_RENDER_NEEDED,
        /** 玩家拾取道具。data = Item.ItemType.name() */
        ITEM_PICKUP,
        /** 道具 buff 到期。data = Item.ItemType.name() */
        ITEM_EXPIRE,

        /* ── 玩家状态 ───────────────────────────────────────────────── */
        /** 生命值变化。data = String.valueOf(newLives) */
        LIVES_CHANGED,
        /** 累计分数变化。data = String.valueOf(newScore) */
        SCORE_CHANGED,
        /**
         * 激活 buff 列表变化。
         * data = 逗号分隔的 buff 显示名，空串表示无 buff。
         * 示例："⚡Speed(5),🧱WallPass"
         */
        BUFF_CHANGED,
        /** 背包内容变化（拾取/使用道具后）。data = "" */
        INVENTORY_CHANGED,

        /* ── UI 辅助 ────────────────────────────────────────────────── */
        /** 底部状态栏消息。data = 待显示文字 */
        STATUS_UPDATE,
        /** 路径提示消失。data = "" */
        HINT_EXPIRE,

        /* ── 认证 ────────────────────────────────────────────────────── */
        /** 登录成功。data = username */
        PLAYER_LOGGED_IN,
        /** 登出。data = "" */
        PLAYER_LOGGED_OUT,

        /* ── 敌人（优先队列驱动时使用）────────────────────────────── */
        /** 敌人移动。data = String.valueOf(enemyId) */
        ENEMY_MOVE,
    }

    // ------------------------------------------------------------------ //
    //  Fields
    // ------------------------------------------------------------------ //

    private final EventType type;
    private final int       tick;   // 游戏帧号；敌人/道具事件使用，其余填 0
    private final String    data;   // 可选载荷

    // ------------------------------------------------------------------ //
    //  Constructors
    // ------------------------------------------------------------------ //

    public GameEvent(EventType type, int tick, String data) {
        this.type = type;
        this.tick = tick;
        this.data = data != null ? data : "";
    }

    /** 便捷构造：tick = 0 */
    public GameEvent(EventType type, String data) { this(type, 0, data); }

    /** 便捷构造：tick = 0，data = "" */
    public GameEvent(EventType type) { this(type, 0, ""); }

    // ------------------------------------------------------------------ //
    //  Accessors
    // ------------------------------------------------------------------ //

    public EventType getType() { return type; }
    public int       getTick() { return tick; }
    public String    getData() { return data; }

    // ------------------------------------------------------------------ //
    //  Comparable（按 tick 排序，供优先队列使用）
    // ------------------------------------------------------------------ //

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

    // ================================================================== //
    //  静态内部类：Bus（全局单例事件总线）
    // ================================================================== //

    /**
     * Bus — 发布 / 订阅事件总线，以 GameEvent.Bus.get() 访问。
     *
     * <pre>
     * // 订阅（组件构造函数中注册一次）
     * GameEvent.Bus.get().subscribe(EventType.LIVES_CHANGED,
     *     e -> Platform.runLater(this::refreshLives));
     *
     * // 发布（逻辑层，无需知道谁在监听）
     * GameEvent.Bus.get().publish(EventType.LIVES_CHANGED, String.valueOf(lives));
     * GameEvent.Bus.get().publish(EventType.MAP_GENERATED);            // 无 data
     * GameEvent.Bus.get().publish(EventType.STATUS_UPDATE, "Cleared!"); // 带 data
     * </pre>
     *
     * 线程安全说明：
     *   publish() 可在任意线程调用；
     *   订阅者若需操作 JavaFX 控件，自行在 handler 内包裹 Platform.runLater()。
     */
    public static final class Bus {

        // ── 单例 ────────────────────────────────────────────────────────
        private static final Bus INSTANCE = new Bus();
        public static Bus get() { return INSTANCE; }
        private Bus() {}

        // ── 监听器注册表 ────────────────────────────────────────────────
        private final EnumMap<EventType, List<Consumer<GameEvent>>> registry =
                new EnumMap<>(EventType.class);

        // ── 订阅 ────────────────────────────────────────────────────────

        /**
         * 注册监听器。同一 type 可注册多个，按注册顺序依次触发。
         */
        public void subscribe(EventType type, Consumer<GameEvent> handler) {
            registry.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
        }

        // ── 发布 ────────────────────────────────────────────────────────

        /** 发布完整 GameEvent 对象。 */
        public void publish(GameEvent event) {
            List<Consumer<GameEvent>> handlers = registry.get(event.getType());
            if (handlers == null) return;
            // 遍历副本，防止 handler 内部 subscribe 导致并发修改
            for (Consumer<GameEvent> h : new ArrayList<>(handlers)) {
                h.accept(event);
            }
        }

        /** 便捷发布：无 data。 */
        public void publish(EventType type) {
            publish(new GameEvent(type));
        }

        /** 便捷发布：带 data。 */
        public void publish(EventType type, String data) {
            publish(new GameEvent(type, data));
        }

        /** 便捷发布：带 tick + data（敌人/道具优先队列使用）。 */
        public void publish(EventType type, int tick, String data) {
            publish(new GameEvent(type, tick, data));
        }

        // ── 工具方法 ────────────────────────────────────────────────────

        /** 清除所有监听器（测试 / 场景重置使用）。 */
        public void clear() { registry.clear(); }

        /** 清除指定类型的监听器。 */
        public void clear(EventType type) { registry.remove(type); }
    }
}