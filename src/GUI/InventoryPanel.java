package GUI;

import World.GameStateController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * C9 – Inventory panel (bottom section of right sidebar).
 *
 * 已修改：
 *   - 新增 ITEM GUIDE 面板，始终显示三种道具的说明
 *   - 每个道具卡片包含：图标、名称、快捷键徽章、功能描述
 *   - 底部 MAP SYMBOLS 图例解释迷宫中各符号含义
 *   - Inventory 区域显示已拾取道具的数量与短描述
 */
public class InventoryPanel extends VBox {

    private static final String BG      = "#16213e";
    private static final String BG_CARD = "#0f1e3a";
    private static final String BG_GUIDE= "#111a33";
    private static final String BORDER  = "#0f3460";
    private static final String RED     = "#e94560";
    private static final String GOLD    = "#f4d35e";
    private static final String GREEN   = "#57cc99";
    private static final String BLUE    = "#4a90d9";
    private static final String PURPLE  = "#9b59b6";
    private static final String CYAN    = "#00e5ff";
    private static final String ORANGE  = "#ff9800";
    private static final String PINK    = "#ff4081";
    private static final String TEXT    = "#a8dadc";
    private static final String DIM     = "#555555";

    // ──── 道具定义（图标、名称、按键、颜色、描述行1、描述行2）────
    private static final String[][] ITEM_DEFS = {
            { "\u26A1", "Speed Boost",  "1", CYAN,   "Move 2 cells per step", "Lasts 10 moves" },
            { "\uD83E\uDDF1", "Wall Pass", "2", ORANGE, "Phase through one wall", "One-time use"   },
            { "\u2694",  "Attack",       "3", PINK,   "Stun nearby enemies",   "Range: 5 cells" },
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

    private final GameStateController gameState;
    private final VBox                itemListBox;
    private final Label               countLabel;

    private List<Item> items   = new ArrayList<>();
    private SortKey    sortKey = SortKey.TYPE;

    public InventoryPanel(GameStateController gameState) {
        this.gameState = gameState;

        setSpacing(0);
        setStyle("-fx-background-color: " + BG + ";");

        // ── 上半部分：Inventory（已拾取道具）──
        VBox invHeader = buildHeader("\uD83C\uDF92  INVENTORY", "Collected items in current maze");
        HBox sortRow = buildSortRow();

        countLabel = new Label("0 items");
        countLabel.setFont(Font.font("Monospaced", 10));
        countLabel.setTextFill(Color.web(DIM));
        countLabel.setPadding(new Insets(2, 10, 2, 10));

        itemListBox = new VBox(4);
        itemListBox.setPadding(new Insets(6, 8, 6, 8));

        ScrollPane invScroll = new ScrollPane(itemListBox);
        invScroll.setFitToWidth(true);
        invScroll.setPrefViewportHeight(120);
        invScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        invScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        invScroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");

        // ── 下半部分：Item Guide（道具说明，始终可见）──
        VBox guideHeader = buildHeader("\uD83D\uDCD6  ITEM GUIDE",
                "Pick up \u25C6 in maze, press key to use");
        VBox guideContent = buildGuideContent();

        ScrollPane guideScroll = new ScrollPane(guideContent);
        guideScroll.setFitToWidth(true);
        guideScroll.setPrefViewportHeight(200);
        guideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        guideScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        guideScroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");

        getChildren().addAll(
                invHeader, sortRow, countLabel, invScroll,
                guideHeader, guideScroll
        );
        refresh();
    }

    public void setItems(List<Item> items) {
        this.items = items != null ? items : new ArrayList<>();
        refresh();
    }

    public void refresh() {
        itemListBox.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("No items yet.\nPick up \u25C6 in the maze!");
            empty.setFont(Font.font("Monospaced", 11));
            empty.setTextFill(Color.web(DIM));
            empty.setWrapText(true); empty.setPadding(new Insets(6));
            itemListBox.getChildren().add(empty);
            countLabel.setText("0 items");
            return;
        }
        List<Item> sorted = sortedItems();
        countLabel.setText(sorted.size() + " type" + (sorted.size() > 1 ? "s" : ""));
        for (Item item : sorted) itemListBox.getChildren().add(buildItemRow(item));
    }

    // ────────────────────────────────────────
    //  Item Guide 道具说明面板
    // ────────────────────────────────────────

    /** 构建道具说明内容：三张卡片 + 底部图例 */
    private VBox buildGuideContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(6, 8, 8, 8));

        for (String[] def : ITEM_DEFS) {
            box.getChildren().add(
                    buildGuideCard(def[0], def[1], def[2], def[3], def[4], def[5]));
        }

        // 底部图例
        box.getChildren().add(buildMapLegend());

        return box;
    }

    /**
     * 单个道具说明卡片。
     * ┌──────────────────────────┐
     * │ ⚡ Speed Boost       [1] │
     * │ Move 2 cells per step    │
     * │ Lasts 10 moves           │
     * └──────────────────────────┘
     */
    private VBox buildGuideCard(String icon, String name, String key,
                                String color, String desc1, String desc2) {
        VBox card = new VBox(2);
        card.setPadding(new Insets(6, 8, 6, 8));
        card.setStyle("-fx-background-color: " + BG_GUIDE + ";"
                + "-fx-background-radius: 5;"
                + "-fx-border-color: " + color + ";"
                + "-fx-border-width: 0 0 0 3; -fx-border-radius: 0 5 5 0;");

        // 第一行：图标 + 名称 + 快捷键徽章
        HBox titleRow = new HBox(4);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(14));

        Label nameLbl = new Label(name);
        nameLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        nameLbl.setTextFill(Color.web(color));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // 快捷键徽章（深色背景，道具色文字）
        Label keyBadge = new Label("[" + key + "]");
        keyBadge.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        keyBadge.setTextFill(Color.web("#0d0d1a"));
        keyBadge.setPadding(new Insets(1, 6, 1, 6));
        keyBadge.setStyle("-fx-background-color: " + color + ";"
                + "-fx-background-radius: 4;");

        titleRow.getChildren().addAll(iconLbl, nameLbl, sp, keyBadge);

        // 第二行：功能描述
        Label descLbl1 = new Label(desc1);
        descLbl1.setFont(Font.font("Monospaced", 10));
        descLbl1.setTextFill(Color.web(TEXT));

        // 第三行：附加说明
        Label descLbl2 = new Label(desc2);
        descLbl2.setFont(Font.font("Monospaced", 9));
        descLbl2.setTextFill(Color.web(DIM));

        card.getChildren().addAll(titleRow, descLbl1, descLbl2);
        return card;
    }

    /** 底部图例：解释迷宫中各种符号的含义 */
    private VBox buildMapLegend() {
        VBox legend = new VBox(4);
        legend.setPadding(new Insets(8, 0, 0, 0));

        Label title = new Label("\u2500\u2500 MAP SYMBOLS \u2500\u2500");
        title.setFont(Font.font("Monospaced", 9));
        title.setTextFill(Color.web(DIM));

        HBox row1 = legendRow("\u25C6", CYAN,   "Speed Boost item");
        HBox row2 = legendRow("\u25C6", ORANGE, "Wall Pass item");
        HBox row3 = legendRow("\u25C6", PINK,   "Attack item");
        HBox row4 = legendRow("\u25CF", GREEN,  "Player (you)");
        HBox row5 = legendRow("\u25CF", RED,    "Enemy");
        HBox row6 = legendRow("\u25CF", GOLD,   "Exit");

        legend.getChildren().addAll(title, row1, row2, row3, row4, row5, row6);
        return legend;
    }

    /** 图例的一行：色块符号 + 文字 */
    private HBox legendRow(String symbol, String color, String label) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label symLbl = new Label(symbol);
        symLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        symLbl.setTextFill(Color.web(color));
        symLbl.setMinWidth(16);

        Label txtLbl = new Label(label);
        txtLbl.setFont(Font.font("Monospaced", 10));
        txtLbl.setTextFill(Color.web(TEXT));

        row.getChildren().addAll(symLbl, txtLbl);
        return row;
    }

    // ────────────────────────────────────────
    //  Inventory 已拾取道具行
    // ────────────────────────────────────────

    private List<Item> sortedItems() {
        List<Item> copy = new ArrayList<>(items);
        copy.sort((a, b) -> {
            switch (sortKey) {
                case TYPE:     return a.type.compareTo(b.type);
                case RARITY:   return rarityRank(a.rarity) - rarityRank(b.rarity);
                case QUANTITY: return b.quantity - a.quantity;
                default:       return 0;
            }
        });
        return copy;
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
        row.setStyle("-fx-background-color: " + BG_CARD + ";"
                + "-fx-background-radius: 4;"
                + "-fx-border-color: " + borderColor + ";"
                + "-fx-border-width: 0 0 0 3; -fx-border-radius: 0;");

        HBox topLine = new HBox(6); topLine.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(itemIcon(item.name)); iconLbl.setFont(Font.font(13));
        Label nameLbl = new Label(item.name);
        nameLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        nameLbl.setTextFill(Color.web(borderColor));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label qtyLbl = new Label("x" + item.quantity);
        qtyLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        qtyLbl.setTextFill(Color.web(GOLD));
        topLine.getChildren().addAll(iconLbl, nameLbl, spacer, qtyLbl);

        HBox botLine = new HBox(6); botLine.setAlignment(Pos.CENTER_LEFT);

        Label keyLbl = new Label(itemKeyhint(item.name));
        keyLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        keyLbl.setTextFill(Color.web(GREEN));
        keyLbl.setPadding(new Insets(1, 4, 1, 4));
        keyLbl.setStyle("-fx-background-color: #1a3a2a;"
                + "-fx-border-color: " + GREEN + ";"
                + "-fx-border-width: 1; -fx-border-radius: 3;"
                + "-fx-background-radius: 3;");

        Label rarityLbl = new Label(item.rarity);
        rarityLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        rarityLbl.setTextFill(Color.web(rarityColour(item.rarity)));
        rarityLbl.setPadding(new Insets(1, 4, 1, 4));
        rarityLbl.setStyle("-fx-background-color: transparent;"
                + "-fx-border-color: " + rarityColour(item.rarity) + ";"
                + "-fx-border-width: 1; -fx-border-radius: 3;");

        Label descLbl = new Label(itemShortDesc(item.name));
        descLbl.setFont(Font.font("Monospaced", 9));
        descLbl.setTextFill(Color.web(DIM));

        botLine.getChildren().addAll(keyLbl, rarityLbl);

        row.getChildren().addAll(topLine, botLine, descLbl);
        return row;
    }

    // ────────────────────────────────────────
    //  UI builders
    // ────────────────────────────────────────

    private VBox buildHeader(String title, String subtitle) {
        VBox header = new VBox(2);
        header.setPadding(new Insets(8, 10, 6, 10));
        header.setStyle("-fx-background-color: " + BORDER + ";");
        Label t = new Label(title);
        t.setFont(Font.font("Monospaced", FontWeight.BOLD, 12)); t.setTextFill(Color.web(RED));
        Label s = new Label(subtitle);
        s.setFont(Font.font("Monospaced", 9)); s.setTextFill(Color.web(DIM));
        header.getChildren().addAll(t, s);
        return header;
    }

    private HBox buildSortRow() {
        HBox row = new HBox(6);
        row.setPadding(new Insets(6, 10, 4, 10));
        row.setAlignment(Pos.CENTER_LEFT);
        Label sortLbl = new Label("Sort:");
        sortLbl.setFont(Font.font("Monospaced", 10)); sortLbl.setTextFill(Color.web(DIM));
        row.getChildren().addAll(sortLbl, sortBtn("Type", SortKey.TYPE),
                sortBtn("Rarity", SortKey.RARITY), sortBtn("Qty", SortKey.QUANTITY));
        return row;
    }

    private Button sortBtn(String label, SortKey key) {
        Button btn = new Button(label);
        btn.setFont(Font.font("Monospaced", 10)); btn.setPrefHeight(20);
        String col  = TEXT;
        String base = "-fx-background-color:#0f3460;-fx-border-color:" + col
                + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-cursor:hand;";
        String act  = "-fx-background-color:" + col + ";-fx-border-color:" + col
                + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-cursor:hand;";
        btn.setTextFill(Color.web(col)); btn.setStyle(base);
        btn.setOnAction(e -> { sortKey = key; refresh(); });
        btn.setOnMouseEntered(e -> { btn.setStyle(act);  btn.setTextFill(Color.web("#0d0d1a")); });
        btn.setOnMouseExited(e  -> { btn.setStyle(base); btn.setTextFill(Color.web(col));       });
        return btn;
    }

    // ────────────────────────────────────────
    //  Lookup helpers
    // ────────────────────────────────────────

    private String rarityColour(String rarity) {
        switch (rarity) {
            case "Epic": return PURPLE;
            case "Rare": return BLUE;
            default:     return DIM;
        }
    }

    private String itemIcon(String name) {
        if (name == null) return "\u2726";
        switch (name) {
            case "Speed Boost": return "\u26A1";
            case "Wall Pass":   return "\uD83E\uDDF1";
            case "Attack":      return "\u2694";
            default:            return "\u2726";
        }
    }

    private String itemKeyhint(String name) {
        if (name == null) return "";
        switch (name) {
            case "Speed Boost": return "Key [1]";
            case "Wall Pass":   return "Key [2]";
            case "Attack":      return "Key [3]";
            default:            return "";
        }
    }

    private String itemNameColor(String name) {
        if (name == null) return TEXT;
        switch (name) {
            case "Speed Boost": return CYAN;
            case "Wall Pass":   return ORANGE;
            case "Attack":      return PINK;
            default:            return TEXT;
        }
    }

    private String itemShortDesc(String name) {
        if (name == null) return "";
        switch (name) {
            case "Speed Boost": return "2x speed for 10 moves";
            case "Wall Pass":   return "Phase through 1 wall";
            case "Attack":      return "Stun enemies in range";
            default:            return "";
        }
    }
}