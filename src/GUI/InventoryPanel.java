package GUI;

import Model.GameEvent;
import Model.GameEvent.EventType;
import World.GameStateController;

import Implementation.ArrayList;
import Implementation.MergeSort;
import Interface.ListInterface;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Supplier;

/**
 * InventoryPanel — 背包面板。
 *
 * 重构后通过 GameEvent.Bus 自我刷新，外部无需调用任何方法。
 *
 * 关键变化：
 *   构造函数新增 {@code Supplier<ArrayList<Item>> itemSupplier} 参数。
 *   Main 传入 {@code this::buildInventoryItems}，当收到 INVENTORY_CHANGED
 *   事件时面板自动调用 Supplier 获取最新道具列表并重绘，无需外部推送。
 *
 * 订阅事件：
 *   INVENTORY_CHANGED → 调用 supplier 刷新道具列表
 *   MAP_GENERATED     → 清空背包（开始新地图时无道具）
 */
public class InventoryPanel extends VBox {

    private static final String BG      = "#13131f";
    private static final String BG_CARD = "#1e1e30";
    private static final String BG_GUIDE= "#1a1a2e";
    private static final String BORDER  = "#383860";
    private static final String RED     = "#ff6b81";
    private static final String GOLD    = "#ffd93d";
    private static final String GREEN   = "#6bcb77";
    private static final String BLUE    = "#4d96ff";
    private static final String PURPLE  = "#bb86fc";
    private static final String CYAN    = "#43cfff";
    private static final String ORANGE  = "#ff9f43";
    private static final String PINK    = "#ff78c4";
    private static final String TEXT    = "#e2e2f0";
    private static final String DIM     = "#b0b0cc";

    private static final String[][] ITEM_DEFS = {
            { "⚡", "Speed Boost", "1", CYAN,   "Move 2 cells per step", "Lasts 10 moves" },
            { "🧱", "Wall Pass",   "2", ORANGE, "Phase through one wall", "One-time use"  },
            { "⚔",  "Attack",      "3", PINK,   "Stun nearby enemies",   "Range: 5 cells" },
    };

    public static class Item {
        public final String name;
        public final String type;
        public final String rarity;
        public final int    quantity;

        public Item(String name, String type, String rarity, int quantity) {
            this.name = name; this.type = type;
            this.rarity = rarity; this.quantity = quantity;
        }
    }

    private enum SortKey { TYPE, RARITY, QUANTITY }

    private final GameStateController       gameState;
    private final Supplier<ArrayList<Item>> itemSupplier;
    private final VBox                      itemListBox;
    private final Label                     countLabel;

    private ArrayList<Item> items   = new ArrayList<>();
    private SortKey         sortKey = SortKey.TYPE;

    /**
     * @param gameState    游戏状态（面板本身暂不使用，保留供未来扩展）
     * @param itemSupplier 由 Main 提供的道具列表生产者，INVENTORY_CHANGED 时调用
     */
    public InventoryPanel(GameStateController gameState,
                          Supplier<ArrayList<Item>> itemSupplier) {
        this.gameState    = gameState;
        this.itemSupplier = itemSupplier;

        setSpacing(0);
        setStyle("-fx-background-color:" + BG + ";");

        VBox invHeader = buildSectionHeader("🎒  INVENTORY", "Collected items in current maze");
        HBox sortRow   = buildSortRow();

        countLabel = new Label("0 items");
        countLabel.setFont(Font.font("Arial", 11));
        countLabel.setTextFill(Color.web(DIM));
        countLabel.setPadding(new Insets(2, 10, 2, 10));

        itemListBox = new VBox(4);
        itemListBox.setPadding(new Insets(6, 8, 6, 8));

        ScrollPane invScroll = new ScrollPane(itemListBox);
        invScroll.setFitToWidth(true);
        invScroll.setPrefViewportHeight(120);
        invScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        invScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        invScroll.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");

        VBox guideHeader  = buildSectionHeader("📖  ITEM GUIDE", "Pick up ◆ in maze, press key to use");
        VBox guideContent = buildGuideContent();

        ScrollPane guideScroll = new ScrollPane(guideContent);
        guideScroll.setFitToWidth(true);
        guideScroll.setPrefViewportHeight(200);
        guideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        guideScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        guideScroll.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");

        getChildren().addAll(invHeader, sortRow, countLabel, invScroll, guideHeader, guideScroll);

        renderItems(); // 初始渲染（空列表）

        // ── 订阅事件 ────────────────────────────────────────────────────
        GameEvent.Bus bus = GameEvent.Bus.get();

        bus.subscribe(EventType.INVENTORY_CHANGED, e -> Platform.runLater(() -> {
            this.items = itemSupplier.get();
            renderItems();
        }));

        bus.subscribe(EventType.MAP_GENERATED, e -> Platform.runLater(() -> {
            this.items = new ArrayList<>();
            renderItems();
        }));
    }

    // ── Render ───────────────────────────────────────────────────────────

    private void renderItems() {
        itemListBox.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("No items yet.\nPick up ◆ in the maze!");
            empty.setFont(Font.font("Arial", 12));
            empty.setTextFill(Color.web(DIM));
            empty.setWrapText(true); empty.setPadding(new Insets(6));
            itemListBox.getChildren().add(empty);
            countLabel.setText("0 items");
            return;
        }
        ArrayList<Item> sorted = sortedItems();
        countLabel.setText(sorted.size() + " type" + (sorted.size() > 1 ? "s" : ""));
        for (Item item : sorted) itemListBox.getChildren().add(buildItemRow(item));
    }

    // ── Item Guide ───────────────────────────────────────────────────────

    private VBox buildGuideContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(6, 8, 8, 8));
        for (String[] def : ITEM_DEFS) {
            box.getChildren().add(buildGuideCard(def[0], def[1], def[2], def[3], def[4], def[5]));
        }
        box.getChildren().add(buildMapLegend());
        return box;
    }

    private VBox buildGuideCard(String icon, String name, String key,
                                String color, String desc1, String desc2) {
        VBox card = new VBox(2);
        card.setPadding(new Insets(6, 8, 6, 8));
        card.setStyle("-fx-background-color:" + BG_GUIDE
                + ";-fx-background-radius:5;"
                + ";-fx-border-color:" + color
                + ";-fx-border-width:0 0 0 3;-fx-border-radius:0 5 5 0;");

        HBox titleRow = new HBox(4);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon); iconLbl.setFont(Font.font(14));
        Label nameLbl = new Label(name);
        nameLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLbl.setTextFill(Color.web(color));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label keyBadge = new Label("[" + key + "]");
        keyBadge.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        keyBadge.setTextFill(Color.web("#0d0d1a"));
        keyBadge.setPadding(new Insets(1, 6, 1, 6));
        keyBadge.setStyle("-fx-background-color:" + color + ";-fx-background-radius:4;");
        titleRow.getChildren().addAll(iconLbl, nameLbl, sp, keyBadge);

        Label descLbl1 = new Label(desc1);
        descLbl1.setFont(Font.font("Arial", 11));
        descLbl1.setTextFill(Color.web(TEXT));
        Label descLbl2 = new Label(desc2);
        descLbl2.setFont(Font.font("Arial", 11));
        descLbl2.setTextFill(Color.web(DIM));

        card.getChildren().addAll(titleRow, descLbl1, descLbl2);
        return card;
    }

    private VBox buildMapLegend() {
        VBox legend = new VBox(4);
        legend.setPadding(new Insets(8, 0, 0, 0));
        Label title = new Label("── MAP SYMBOLS ──");
        title.setFont(Font.font("Arial", 11));
        title.setTextFill(Color.web(DIM));
        legend.getChildren().addAll(title,
                legendRow("◆", CYAN,   "Speed Boost item"),
                legendRow("◆", ORANGE, "Wall Pass item"),
                legendRow("◆", PINK,   "Attack item"),
                legendRow("●", GREEN,  "Player (you)"),
                legendRow("●", RED,    "Enemy"),
                legendRow("●", GOLD,   "Exit"));
        return legend;
    }

    private HBox legendRow(String symbol, String color, String label) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Label symLbl = new Label(symbol);
        symLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        symLbl.setTextFill(Color.web(color));
        symLbl.setMinWidth(16);
        Label txtLbl = new Label(label);
        txtLbl.setFont(Font.font("Arial", 11));
        txtLbl.setTextFill(Color.web(TEXT));
        row.getChildren().addAll(symLbl, txtLbl);
        return row;
    }

    // ── Inventory rows ───────────────────────────────────────────────────

    private ArrayList<Item> sortedItems() {
        ArrayList<Item> copy = new ArrayList<>();
        for (Item item : items) copy.add(item);
        return new MergeSort<Item>().sortList(copy, (a, b) -> {
            switch (sortKey) {
                case TYPE:     return a.type.compareTo(b.type);
                case RARITY:   return rarityRank(a.rarity) - rarityRank(b.rarity);
                case QUANTITY: return b.quantity - a.quantity;
                default:       return 0;
            }
        });
    }

    private int rarityRank(String rarity) {
        switch (rarity) {
            case "Epic": return 0;
            case "Rare": return 1;
            default:     return 2;
        }
    }

    private VBox buildItemRow(Item item) {
        VBox row = new VBox(2);
        row.setPadding(new Insets(5, 8, 5, 8));
        String borderColor = itemNameColor(item.name);
        row.setStyle("-fx-background-color:" + BG_CARD
                + ";-fx-background-radius:4;"
                + ";-fx-border-color:" + borderColor
                + ";-fx-border-width:0 0 0 3;-fx-border-radius:0;");

        HBox topLine = new HBox(6); topLine.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(itemIcon(item.name)); iconLbl.setFont(Font.font(13));
        Label nameLbl = new Label(item.name);
        nameLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLbl.setTextFill(Color.web(borderColor));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label qtyLbl = new Label("x" + item.quantity);
        qtyLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        qtyLbl.setTextFill(Color.web(GOLD));
        topLine.getChildren().addAll(iconLbl, nameLbl, spacer, qtyLbl);

        HBox botLine = new HBox(6); botLine.setAlignment(Pos.CENTER_LEFT);
        Label keyLbl = new Label(itemKeyhint(item.name));
        keyLbl.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        keyLbl.setTextFill(Color.web(GREEN));
        keyLbl.setPadding(new Insets(1, 4, 1, 4));
        keyLbl.setStyle("-fx-background-color:#1a3a2a;"
                + ";-fx-border-color:" + GREEN
                + ";-fx-border-width:1;-fx-border-radius:3;-fx-background-radius:3;");
        Label rarityLbl = new Label(item.rarity);
        rarityLbl.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        rarityLbl.setTextFill(Color.web(rarityColour(item.rarity)));
        rarityLbl.setPadding(new Insets(1, 4, 1, 4));
        rarityLbl.setStyle("-fx-background-color:transparent;"
                + ";-fx-border-color:" + rarityColour(item.rarity)
                + ";-fx-border-width:1;-fx-border-radius:3;");
        Label descLbl = new Label(itemShortDesc(item.name));
        descLbl.setFont(Font.font("Arial", 11));
        descLbl.setTextFill(Color.web(DIM));
        botLine.getChildren().addAll(keyLbl, rarityLbl);

        row.getChildren().addAll(topLine, botLine, descLbl);
        return row;
    }

    // ── UI builders ──────────────────────────────────────────────────────

    private VBox buildSectionHeader(String title, String subtitle) {
        VBox header = new VBox(2);
        header.setPadding(new Insets(8, 10, 6, 10));
        header.setStyle("-fx-background-color:" + BORDER + ";");
        Label t = new Label(title);
        t.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        t.setTextFill(Color.web(RED));
        Label s = new Label(subtitle);
        s.setFont(Font.font("Arial", 11));
        s.setTextFill(Color.web(DIM));
        header.getChildren().addAll(t, s);
        return header;
    }

    private HBox buildSortRow() {
        HBox row = new HBox(6);
        row.setPadding(new Insets(6, 10, 4, 10));
        row.setAlignment(Pos.CENTER_LEFT);
        Label sortLbl = new Label("Sort:");
        sortLbl.setFont(Font.font("Arial", 11));
        sortLbl.setTextFill(Color.web(DIM));
        row.getChildren().addAll(sortLbl,
                sortBtn("Type", SortKey.TYPE),
                sortBtn("Rarity", SortKey.RARITY),
                sortBtn("Qty", SortKey.QUANTITY));
        return row;
    }

    private Button sortBtn(String label, SortKey key) {
        Button btn = new Button(label);
        btn.setFont(Font.font("Arial", 11)); btn.setPrefHeight(20);
        String n = "-fx-background-color:#0f3460;-fx-border-color:" + TEXT
                + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-cursor:hand;";
        String h = "-fx-background-color:" + TEXT + ";-fx-border-color:" + TEXT
                + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-cursor:hand;";
        btn.setTextFill(Color.web(TEXT)); btn.setStyle(n);
        btn.setOnAction(e -> { sortKey = key; renderItems(); });
        btn.setOnMouseEntered(e -> { btn.setStyle(h); btn.setTextFill(Color.web("#0d0d1a")); });
        btn.setOnMouseExited (e -> { btn.setStyle(n); btn.setTextFill(Color.web(TEXT));       });
        return btn;
    }

    // ── Lookup helpers ───────────────────────────────────────────────────

    private String rarityColour(String rarity) {
        switch (rarity) { case "Epic": return PURPLE; case "Rare": return BLUE; default: return DIM; }
    }
    private String itemIcon(String name) {
        if (name == null) return "✦";
        switch (name) { case "Speed Boost": return "⚡"; case "Wall Pass": return "🧱"; case "Attack": return "⚔"; default: return "✦"; }
    }
    private String itemKeyhint(String name) {
        if (name == null) return "";
        switch (name) { case "Speed Boost": return "Key [1]"; case "Wall Pass": return "Key [2]"; case "Attack": return "Key [3]"; default: return ""; }
    }
    private String itemNameColor(String name) {
        if (name == null) return TEXT;
        switch (name) { case "Speed Boost": return CYAN; case "Wall Pass": return ORANGE; case "Attack": return PINK; default: return TEXT; }
    }
    private String itemShortDesc(String name) {
        if (name == null) return "";
        switch (name) { case "Speed Boost": return "2x speed for 10 moves"; case "Wall Pass": return "Phase through 1 wall"; case "Attack": return "Stun enemies in range"; default: return ""; }
    }
}