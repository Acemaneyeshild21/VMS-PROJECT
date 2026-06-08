package pkg.vms;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Contrôle de pagination générique pour TableView.
 *
 * <p>Usage :
 * <pre>
 *   table  = new TableView&lt;&gt;();
 *   pager  = new Pager&lt;&gt;(table);          // lie la table
 *   card.getChildren().addAll(table, pager.getBar());
 *
 *   // Dans loadData() :
 *   pager.setData(list);
 * </pre>
 *
 * <p>La barre affiche : <em>1–20 sur 87</em>  «  ‹  2 / 5  ›  »  Par page [20 ▼]
 */
public class Pager<T> {

    private final TableView<T>      table;
    private final ObservableList<T> visible = FXCollections.observableArrayList();
    private       List<T>           allData = new ArrayList<>();
    private       int               page    = 0;
    private       int               perPage = 20;

    // Barre de navigation (mise à jour à chaque changement de page)
    private final Label           lblInfo  = new Label("0 résultats");
    private final Label           lblPage  = new Label("1 / 1");
    private final Button          btnFirst = navBtn("«");
    private final Button          btnPrev  = navBtn("‹");
    private final Button          btnNext  = navBtn("›");
    private final Button          btnLast  = navBtn("»");
    private final ComboBox<Integer> cbxPer = new ComboBox<>();
    private final HBox            bar;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public Pager(TableView<T> table) {
        this.table = table;
        table.setItems(visible);
        bar = buildBar();
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /** Charge (ou recharge) la liste complète et revient à la page 1. */
    public void setData(List<T> data) {
        allData = data != null ? new ArrayList<>(data) : new ArrayList<>();
        page = 0;
        refresh();
    }

    /** Renvoie la barre de pagination à ajouter sous la TableView. */
    public HBox getBar() { return bar; }

    /** Nombre total d'éléments (toutes pages confondues). */
    public int size() { return allData.size(); }

    // ── Construction de la barre ──────────────────────────────────────────────

    private HBox buildBar() {
        HBox h = new HBox(6);
        h.setAlignment(Pos.CENTER_RIGHT);
        h.setPadding(new Insets(9, 16, 9, 16));
        h.setStyle(
            "-fx-background-color:#f8fafc;"
            + "-fx-border-color:#e2e8f0;-fx-border-width:1 0 0 0;");

        lblInfo.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        lblPage.setStyle(
            "-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1e293b;"
            + "-fx-min-width:54;-fx-alignment:center;");

        cbxPer.getItems().addAll(10, 20, 50, 100);
        cbxPer.setValue(perPage);
        cbxPer.setStyle("-fx-font-size:12;");
        cbxPer.setPrefWidth(72);
        cbxPer.setOnAction(e -> {
            perPage = cbxPer.getValue();
            page = 0;
            refresh();
        });

        btnFirst.setOnAction(e -> { page = 0; refresh(); });
        btnPrev.setOnAction(e  -> { if (page > 0) { page--; refresh(); } });
        btnNext.setOnAction(e  -> {
            if (page < totalPages() - 1) { page++; refresh(); }
        });
        btnLast.setOnAction(e  -> { page = Math.max(0, totalPages() - 1); refresh(); });

        Label lblPer = new Label("Par page :");
        lblPer.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Separator s1 = sep(), s2 = sep();

        h.getChildren().addAll(
            spacer, lblInfo,
            s1,
            btnFirst, btnPrev, lblPage, btnNext, btnLast,
            s2,
            lblPer, cbxPer
        );

        refresh(); // état initial (tout désactivé)
        return h;
    }

    // ── Rafraîchissement ──────────────────────────────────────────────────────

    private void refresh() {
        int total = allData.size();
        int from  = page * perPage;
        int to    = Math.min(from + perPage, total);

        visible.setAll(total == 0 ? List.of() : allData.subList(from, to));

        int tp = totalPages();
        lblInfo.setText(total == 0
            ? "Aucun résultat"
            : (from + 1) + "–" + to + " sur " + total + " entrée(s)");
        lblPage.setText((page + 1) + " / " + Math.max(1, tp));

        btnFirst.setDisable(page == 0);
        btnPrev.setDisable(page == 0);
        btnNext.setDisable(page >= tp - 1);
        btnLast.setDisable(page >= tp - 1);
    }

    private int totalPages() {
        return allData.isEmpty() ? 1 : (int) Math.ceil((double) allData.size() / perPage);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Button navBtn(String text) {
        Button b = new Button(text);
        String base = "-fx-font-size:13;-fx-padding:2 8;-fx-cursor:hand;"
                    + "-fx-border-radius:4;-fx-background-radius:4;";
        b.setStyle(base + "-fx-background-color:transparent;-fx-text-fill:#475569;");
        b.setOnMouseEntered(e -> {
            if (!b.isDisabled())
                b.setStyle(base + "-fx-background-color:#e2e8f0;-fx-text-fill:#1e293b;");
        });
        b.setOnMouseExited(e ->
            b.setStyle(base + "-fx-background-color:transparent;-fx-text-fill:#475569;"));
        return b;
    }

    private static Separator sep() {
        Separator s = new Separator(Orientation.VERTICAL);
        s.setStyle("-fx-background-color:#e2e8f0;");
        return s;
    }
}
