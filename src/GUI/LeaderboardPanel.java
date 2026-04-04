package GUI;

import Model.GameRecord;
import World.GameStateController;

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

import java.util.List;

/**
 * C8 – Leaderboard panel (right sidebar).
 *
 * Sections:
 *   1. TOP 10      – MaxHeap.topK(10)
 *   2. RANK QUERY  – input rank → MaxHeap.topK(rank)
 *   3. RANGE QUERY – input [lo, hi] → AVLTree.rangeQuery(lo, hi)
 */
public class LeaderboardPanel extends VBox {

    private static final String BG      = "#16213e";
    private static final String BG_CARD = "#0f1e3a";
    private static final String BORDER  = "#0f3460";
    private static final String RED     = "#e94560";
    private static final String GOLD    = "#ffd700";
    private static final String SILVER  = "#c0c0c0";
    private static final String BRONZE  = "#cd7f32";
    private static final String TEXT    = "#a8dadc";
    private static final String DIM     = "#555555";
    private static final String GREEN   = "#57cc99";

    private final GameStateController gameState;
    private final VBox                topListBox;
    private final VBox                rangeResultBox;
    private final TextField           loField;
    private final TextField           hiField;
    private final TextField           rankField;
    private final Label               rankResultLabel;
    private final Label               rangeCountLabel;

    public LeaderboardPanel(GameStateController gameState) {
        this.gameState = gameState;

        setPrefWidth(210);
        setSpacing(0);
        setStyle("-fx-background-color: " + BG + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 0 0 0 2;");

        // Section 1 – Top 10
        topListBox = new VBox(3);
        topListBox.setPadding(new Insets(6, 10, 6, 10));

        ScrollPane topScroll = new ScrollPane(topListBox);
        topScroll.setFitToWidth(true);
        topScroll.setPrefViewportHeight(220);
        topScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        topScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        topScroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");

        // Section 2 – Rank query
        rankField = styledField("Rank (1, 2…)");
        Button rankBtn = styledBtn("Find", TEXT);
        rankBtn.setOnAction(e -> runRankQuery());
        HBox rankInputRow = new HBox(6, rankField, rankBtn);
        rankInputRow.setPadding(new Insets(0, 10, 4, 10));

        rankResultLabel = new Label("");
        rankResultLabel.setFont(Font.font("Monospaced", 12));
        rankResultLabel.setTextFill(Color.web(GOLD));
        rankResultLabel.setPadding(new Insets(0, 10, 6, 10));
        rankResultLabel.setWrapText(true);

        // Section 3 – Range query
        loField = styledField("Min score");
        hiField = styledField("Max score");
        Button queryBtn = styledBtn("Search", GREEN);
        queryBtn.setOnAction(e -> runRangeQuery());

        HBox inputRow = new HBox(6, loField, hiField);
        inputRow.setPadding(new Insets(0, 10, 4, 10));
        HBox btnRow = new HBox();
        btnRow.setPadding(new Insets(0, 10, 4, 10));
        btnRow.getChildren().add(queryBtn);

        rangeCountLabel = new Label("");
        rangeCountLabel.setFont(Font.font("Monospaced", 11));
        rangeCountLabel.setTextFill(Color.web(DIM));
        rangeCountLabel.setPadding(new Insets(0, 10, 0, 10));

        rangeResultBox = new VBox(3);
        rangeResultBox.setPadding(new Insets(4, 10, 6, 10));
        ScrollPane rangeScroll = new ScrollPane(rangeResultBox);
        rangeScroll.setFitToWidth(true);
        rangeScroll.setPrefViewportHeight(160);
        rangeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rangeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rangeScroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");

        getChildren().addAll(
                sectionHeader("🏆  TOP SCORES", "MaxHeap · topK(10)"),
                topScroll,
                sectionHeader("🔢  RANK QUERY", "MaxHeap · topK(rank)"),
                rankInputRow,
                rankResultLabel,
                sectionHeader("🔍  RANGE QUERY", "AVLTree · rangeQuery(lo,hi)"),
                inputRow, btnRow, rangeCountLabel, rangeScroll
        );

        refresh();
    }

    public void refresh() { refreshTopList(); }

    // ──── Top-10 ────

    private void refreshTopList() {
        topListBox.getChildren().clear();
        List<Integer> top = gameState.getTopK(10);
        if (top.isEmpty()) {
            Label empty = new Label("No scores yet.\nComplete a path to appear here!");
            empty.setFont(Font.font("Monospaced", 11));
            empty.setTextFill(Color.web(DIM));
            empty.setWrapText(true);
            topListBox.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < top.size(); i++) topListBox.getChildren().add(buildTopRow(i + 1, top.get(i)));
    }

    private HBox buildTopRow(int rank, int score) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 6, 3, 6));
        row.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 4;");

        String medalIcon, rankColour;
        if      (rank == 1) { medalIcon = "🥇"; rankColour = GOLD;   }
        else if (rank == 2) { medalIcon = "🥈"; rankColour = SILVER; }
        else if (rank == 3) { medalIcon = "🥉"; rankColour = BRONZE; }
        else                { medalIcon = "  "; rankColour = DIM;    }

        Label medalLbl = new Label(medalIcon); medalLbl.setFont(Font.font("Monospaced", 13));
        Label rankLbl  = new Label(String.format("%2d.", rank));
        rankLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        rankLbl.setTextFill(Color.web(rankColour)); rankLbl.setPrefWidth(28);
        Label scoreLbl = new Label(String.format("%,d pts", score));
        scoreLbl.setFont(Font.font("Monospaced", 12));
        scoreLbl.setTextFill(Color.web(rank <= 3 ? rankColour : TEXT));

        row.getChildren().addAll(medalLbl, rankLbl, scoreLbl);
        return row;
    }

    // ──── Rank query ────

    private void runRankQuery() {
        rankResultLabel.setText("");
        int rank;
        try { rank = Integer.parseInt(rankField.getText().trim()); }
        catch (NumberFormatException ex) {
            rankResultLabel.setText("⚠ Enter a valid rank number.");
            rankResultLabel.setTextFill(Color.web(RED)); return;
        }
        if (rank <= 0) {
            rankResultLabel.setText("⚠ Rank must be ≥ 1.");
            rankResultLabel.setTextFill(Color.web(RED)); return;
        }
        List<Integer> top = gameState.getTopK(rank);
        if (top.size() < rank) {
            rankResultLabel.setText("Only " + top.size() + " score" + (top.size() == 1 ? "" : "s") + " recorded so far.");
            rankResultLabel.setTextFill(Color.web(DIM)); return;
        }
        int score = top.get(rank - 1);
        rankResultLabel.setText("Rank #" + rank + "  →  " + String.format("%,d", score) + " pts");
        rankResultLabel.setTextFill(Color.web(GOLD));
    }

    // ──── Range query ────

    private void runRangeQuery() {
        rangeResultBox.getChildren().clear(); rangeCountLabel.setText("");
        int lo, hi;
        try {
            lo = Integer.parseInt(loField.getText().trim());
            hi = Integer.parseInt(hiField.getText().trim());
        } catch (NumberFormatException ex) {
            rangeCountLabel.setText("⚠ Enter valid integers.");
            rangeCountLabel.setTextFill(Color.web(RED)); return;
        }
        if (lo > hi) {
            rangeCountLabel.setText("⚠ Min must be ≤ Max.");
            rangeCountLabel.setTextFill(Color.web(RED)); return;
        }
        List<GameRecord> results = gameState.getScoreRange(lo, hi);
        if (results.isEmpty()) {
            rangeCountLabel.setText("No records in [" + lo + ", " + hi + "]");
            rangeCountLabel.setTextFill(Color.web(DIM)); return;
        }
        rangeCountLabel.setText("Found " + results.size()
                + " record" + (results.size() > 1 ? "s" : "") + " in [" + lo + ", " + hi + "]");
        rangeCountLabel.setTextFill(Color.web(GREEN));
        for (GameRecord rec : results) rangeResultBox.getChildren().add(buildRangeRow(rec));
    }

    private HBox buildRangeRow(GameRecord rec) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 6, 3, 6));
        row.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 4;");

        Label playerLbl = new Label(rec.getPlayerName());
        playerLbl.setFont(Font.font("Monospaced", 11));
        playerLbl.setTextFill(Color.web(TEXT)); playerLbl.setPrefWidth(52);
        Label scoreLbl = new Label(String.format("%,d", rec.getScore()));
        scoreLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        scoreLbl.setTextFill(Color.web(GREEN));
        Label lvLbl = new Label("Lv" + rec.getLevelId());
        lvLbl.setFont(Font.font("Monospaced", 10)); lvLbl.setTextFill(Color.web(DIM));

        row.getChildren().addAll(playerLbl, scoreLbl, lvLbl);
        return row;
    }

    // ──── UI helpers ────

    private VBox sectionHeader(String title, String subtitle) {
        VBox header = new VBox(2);
        header.setPadding(new Insets(10, 10, 6, 10));
        header.setStyle("-fx-background-color: " + BORDER + ";");
        Label t = new Label(title);
        t.setFont(Font.font("Monospaced", FontWeight.BOLD, 12)); t.setTextFill(Color.web(RED));
        Label s = new Label(subtitle);
        s.setFont(Font.font("Monospaced", 9)); s.setTextFill(Color.web(DIM));
        header.getChildren().addAll(t, s);
        return header;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt); tf.setPrefWidth(88); tf.setPrefHeight(26);
        tf.setFont(Font.font("Monospaced", 11));
        tf.setStyle("-fx-background-color: #0f3460; -fx-text-fill: #a8dadc;"
                + "-fx-prompt-text-fill: #444466; -fx-border-color: #334466;"
                + "-fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;");
        return tf;
    }

    private Button styledBtn(String text, String col) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        btn.setTextFill(Color.web(col)); btn.setPrefHeight(26);
        String n = "-fx-background-color:#0f3460;-fx-border-color:" + col + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-cursor:hand;";
        String h = "-fx-background-color:" + col + ";-fx-border-color:" + col + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-cursor:hand;";
        btn.setStyle(n);
        btn.setOnMouseEntered(e -> { btn.setStyle(h); btn.setTextFill(Color.web("#0d0d1a")); });
        btn.setOnMouseExited(e  -> { btn.setStyle(n); btn.setTextFill(Color.web(col));        });
        return btn;
    }
}
