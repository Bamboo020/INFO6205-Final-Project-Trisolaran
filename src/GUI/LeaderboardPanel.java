package GUI;

import Model.GameEvent;
import Model.GameEvent.EventType;
import Model.GameRecord;
import World.DBManager;
import World.GameStateController;

import Implementation.ArrayList;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * LeaderboardPanel — 右侧排行榜面板。
 *
 * 重构后通过 GameEvent.Bus 自我刷新，外部无需调用任何方法。
 * 订阅事件：NODE_COMPLETED（路径完成后刷新榜单）/ PLAYER_LOGGED_IN（重载历史）
 */
public class LeaderboardPanel extends VBox {

    private static final String BG        = "#13131f";
    private static final String BG_CARD   = "#1e1e30";
    private static final String BG_HEADER = "#252540";
    private static final String BORDER    = "#383860";
    private static final String TEXT      = "#e2e2f0";
    private static final String DIM       = "#b0b0cc";
    private static final String RED       = "#ff6b81";
    private static final String GOLD      = "#ffd93d";
    private static final String SILVER    = "#c0c0d0";
    private static final String BRONZE    = "#d4956a";
    private static final String GREEN     = "#6bcb77";
    private static final String BLUE      = "#4d96ff";
    private static final String CYAN      = "#43cfff";

    private final GameStateController gameState;
    private final DBManager           db;

    private final VBox      topListBox;
    private final VBox      rangeResultBox;
    private final TextField loField;
    private final TextField hiField;
    private final TextField rankField;
    private final Label     rankResultLabel;
    private final Label     rangeCountLabel;

    public LeaderboardPanel(GameStateController gameState, DBManager db) {
        this.gameState = gameState;
        this.db        = db;

        setPrefWidth(220);
        setSpacing(0);
        setStyle("-fx-background-color:" + BG
                + ";-fx-border-color:" + BORDER
                + ";-fx-border-width:0 0 0 2;");

        topListBox = new VBox(4);
        topListBox.setPadding(new Insets(8, 10, 8, 10));
        topListBox.setStyle("-fx-background-color:" + BG + ";");

        ScrollPane topScroll = new ScrollPane(topListBox);
        topScroll.setFitToWidth(true);
        topScroll.setPrefViewportHeight(230);
        topScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        topScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        topScroll.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");

        rankField = styledField("Rank  e.g. 3");
        Button rankBtn = styledBtn("Find", CYAN);
        rankBtn.setOnAction(e -> runRankQuery());
        HBox rankRow = new HBox(6, rankField, rankBtn);
        rankRow.setPadding(new Insets(0, 10, 6, 10));

        rankResultLabel = new Label("");
        rankResultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        rankResultLabel.setTextFill(Color.web(GOLD));
        rankResultLabel.setPadding(new Insets(0, 10, 6, 10));
        rankResultLabel.setWrapText(true);

        loField = styledField("Min");
        hiField = styledField("Max");
        Button queryBtn = styledBtn("Search", GREEN);
        queryBtn.setOnAction(e -> runRangeQuery());

        HBox inputRow = new HBox(6, loField, hiField);
        inputRow.setPadding(new Insets(0, 10, 4, 10));
        HBox btnRow = new HBox();
        btnRow.setPadding(new Insets(0, 10, 4, 10));
        btnRow.getChildren().add(queryBtn);

        rangeCountLabel = new Label("");
        rangeCountLabel.setFont(Font.font("Arial", 12));
        rangeCountLabel.setTextFill(Color.web(DIM));
        rangeCountLabel.setPadding(new Insets(0, 10, 2, 10));

        rangeResultBox = new VBox(4);
        rangeResultBox.setPadding(new Insets(4, 10, 8, 10));
        ScrollPane rangeScroll = new ScrollPane(rangeResultBox);
        rangeScroll.setFitToWidth(true);
        rangeScroll.setPrefViewportHeight(150);
        rangeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rangeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rangeScroll.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");

        getChildren().addAll(
                sectionHeader("TOP SCORES",  "DB · ORDER BY score DESC"),
                topScroll,
                sectionHeader("RANK QUERY",  "MaxHeap · topK(rank)"),
                rankRow, rankResultLabel,
                sectionHeader("RANGE QUERY", "AVLTree · rangeQuery(lo, hi)"),
                inputRow, btnRow, rangeCountLabel, rangeScroll
        );

        // 初始加载
        refreshTopList();

        // ── 订阅事件 ────────────────────────────────────────────────────
        GameEvent.Bus bus = GameEvent.Bus.get();

        // 路径完成（叶节点）才会有新分数入库，刷新榜单
        bus.subscribe(EventType.NODE_COMPLETED,
                e -> Platform.runLater(this::refreshTopList));

        // 用户登录后重新拉取历史分数
        bus.subscribe(EventType.PLAYER_LOGGED_IN,
                e -> Platform.runLater(this::refreshTopList));
    }

    // ── Top-10 ───────────────────────────────────────────────────────────

    private void refreshTopList() {
        topListBox.getChildren().clear();

        ArrayList<DBManager.ScoreRecord> rows = db.loadTopScores(10);

        if (rows.isEmpty()) {
            ArrayList<Integer> top = gameState.getTopK(10);
            if (top.isEmpty()) {
                Label empty = new Label("No scores yet.\nFinish a path to appear here!");
                empty.setFont(Font.font("Arial", 12));
                empty.setTextFill(Color.web(DIM));
                empty.setWrapText(true);
                topListBox.getChildren().add(empty);
                return;
            }
            for (int i = 0; i < top.size(); i++) {
                topListBox.getChildren().add(buildTopRow(i + 1, "—", top.get(i)));
            }
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            DBManager.ScoreRecord r = rows.get(i);
            topListBox.getChildren().add(buildTopRow(i + 1, r.username, r.score));
        }
    }

    private HBox buildTopRow(int rank, String username, int score) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 8, 5, 8));

        String accent, medal;
        if      (rank == 1) { medal = "🥇"; accent = GOLD;   }
        else if (rank == 2) { medal = "🥈"; accent = SILVER; }
        else if (rank == 3) { medal = "🥉"; accent = BRONZE; }
        else                { medal = "";   accent = DIM;    }

        row.setStyle("-fx-background-color:" + (rank <= 3 ? "#252540" : BG_CARD) + ";"
                + "-fx-background-radius:5;"
                + "-fx-border-color:" + (rank <= 3 ? accent : BORDER) + ";"
                + "-fx-border-width:0 0 0 3;-fx-border-radius:0 5 5 0;");

        Label numLbl = new Label(rank <= 3 ? medal : String.format("%2d.", rank));
        numLbl.setFont(Font.font("Arial", FontWeight.BOLD, rank <= 3 ? 15 : 12));
        numLbl.setTextFill(Color.web(accent));
        numLbl.setMinWidth(28);

        Label nameLbl = new Label(username);
        nameLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLbl.setTextFill(Color.web(rank <= 3 ? TEXT : DIM));
        nameLbl.setMaxWidth(80);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label scoreLbl = new Label(String.format("%,d", score));
        scoreLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        scoreLbl.setTextFill(Color.web(rank <= 3 ? accent : GREEN));

        row.getChildren().addAll(numLbl, nameLbl, sp, scoreLbl);
        return row;
    }

    // ── Rank query ───────────────────────────────────────────────────────

    private void runRankQuery() {
        rankResultLabel.setText("");
        int rank;
        try { rank = Integer.parseInt(rankField.getText().trim()); }
        catch (NumberFormatException ex) {
            rankResultLabel.setText("Enter a valid number.");
            rankResultLabel.setTextFill(Color.web(RED)); return;
        }
        if (rank <= 0) {
            rankResultLabel.setText("Rank must be ≥ 1.");
            rankResultLabel.setTextFill(Color.web(RED)); return;
        }
        ArrayList<Integer> top = gameState.getTopK(rank);
        if (top.size() < rank) {
            rankResultLabel.setText("Only " + top.size() + " score(s) so far.");
            rankResultLabel.setTextFill(Color.web(DIM)); return;
        }
        int s = top.get(rank - 1);
        rankResultLabel.setText("Rank #" + rank + "  →  " + String.format("%,d pts", s));
        rankResultLabel.setTextFill(Color.web(GOLD));
    }

    // ── Range query ──────────────────────────────────────────────────────

    private void runRangeQuery() {
        rangeResultBox.getChildren().clear();
        rangeCountLabel.setText("");
        int lo, hi;
        try {
            lo = Integer.parseInt(loField.getText().trim());
            hi = Integer.parseInt(hiField.getText().trim());
        } catch (NumberFormatException ex) {
            rangeCountLabel.setText("Enter valid integers.");
            rangeCountLabel.setTextFill(Color.web(RED)); return;
        }
        if (lo > hi) {
            rangeCountLabel.setText("Min must be ≤ Max.");
            rangeCountLabel.setTextFill(Color.web(RED)); return;
        }
        ArrayList<GameRecord> results = gameState.getScoreRange(lo, hi);
        if (results.isEmpty()) {
            rangeCountLabel.setText("No records in [" + lo + ", " + hi + "]");
            rangeCountLabel.setTextFill(Color.web(DIM)); return;
        }
        rangeCountLabel.setText("Found " + results.size() + " record(s)  [" + lo + ", " + hi + "]");
        rangeCountLabel.setTextFill(Color.web(GREEN));
        for (GameRecord rec : results) rangeResultBox.getChildren().add(buildRangeRow(rec));
    }

    private HBox buildRangeRow(GameRecord rec) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 8, 4, 8));
        row.setStyle("-fx-background-color:" + BG_CARD + ";-fx-background-radius:4;");

        Label nameLbl = new Label(rec.getPlayerName());
        nameLbl.setFont(Font.font("Arial", 12));
        nameLbl.setTextFill(Color.web(TEXT));
        nameLbl.setPrefWidth(60);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label scoreLbl = new Label(String.format("%,d pts", rec.getScore()));
        scoreLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        scoreLbl.setTextFill(Color.web(GREEN));

        Label lvLbl = new Label("Lv" + rec.getLevelId());
        lvLbl.setFont(Font.font("Arial", 11));
        lvLbl.setTextFill(Color.web(BLUE));

        row.getChildren().addAll(nameLbl, sp, scoreLbl, lvLbl);
        return row;
    }

    // ── UI helpers ───────────────────────────────────────────────────────

    private VBox sectionHeader(String title, String subtitle) {
        VBox header = new VBox(2);
        header.setPadding(new Insets(8, 12, 6, 12));
        header.setStyle("-fx-background-color:" + BG_HEADER
                + ";-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        Label t = new Label(title);
        t.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        t.setTextFill(Color.web(CYAN));
        Label s = new Label(subtitle);
        s.setFont(Font.font("Arial", 10));
        s.setTextFill(Color.web(DIM));
        header.getChildren().addAll(t, s);
        return header;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(82); tf.setPrefHeight(28);
        tf.setFont(Font.font("Arial", 12));
        tf.setStyle("-fx-background-color:" + BG_CARD
                + ";-fx-text-fill:" + TEXT
                + ";-fx-prompt-text-fill:" + DIM
                + ";-fx-border-color:" + BORDER
                + ";-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;");
        return tf;
    }

    private Button styledBtn(String text, String col) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        btn.setTextFill(Color.web(col));
        btn.setPrefHeight(28);
        String n = "-fx-background-color:" + BG_CARD + ";-fx-border-color:" + col
                + ";-fx-border-width:1;-fx-background-radius:4;-fx-border-radius:4;-fx-cursor:hand;";
        String h = "-fx-background-color:" + col + ";-fx-border-color:" + col
                + ";-fx-border-width:1;-fx-background-radius:4;-fx-border-radius:4;-fx-cursor:hand;";
        btn.setStyle(n);
        btn.setOnMouseEntered(e -> { btn.setStyle(h); btn.setTextFill(Color.web("#13131f")); });
        btn.setOnMouseExited (e -> { btn.setStyle(n); btn.setTextFill(Color.web(col));       });
        return btn;
    }
}