package com.bizmind.view;

import com.bizmind.controller.ExpenseController;
import com.bizmind.manager.ExpenseManager;
import com.bizmind.model.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Expense Module UI — US-16, US-17, US-18, US-19.
 * All style classes match dashboard.css exactly.
 */
public class ExpenseView {

    public static final List<String> CATEGORIES = Arrays.asList(
            "Rent", "Utilities", "Salaries", "Inventory Purchase",
            "Marketing", "Maintenance", "Transportation", "Other"
    );

    // ── Form fields (package-visible for controller) ──
    public TextField        titleField;
    public ComboBox<String> categoryCombo;
    public TextField        amountField;
    public DatePicker       datePicker;
    public TextArea         descriptionArea;
    public Label            feedbackLabel;

    // ── Filter controls ──
    public ComboBox<String>   filterCategoryCombo;
    public TextField          searchField;

    // ── Table ──
    public TableView<Expense> expenseTable;

    // ── Summary ──
    private Label totalLabel;
    private HBox  categoryCardsRow;

    private final StackPane contentArea;
    private ExpenseController controller;

    public ExpenseView(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void show() {
        controller = new ExpenseController(this);
        contentArea.getChildren().clear();

        // Wrap in ScrollPane so nothing gets clipped on smaller screens
        ScrollPane scroll = new ScrollPane(buildRoot());
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.getStyleClass().add("add-product-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        contentArea.getChildren().add(scroll);
    }

    // ══════════════════════════════════════════════
    //  Root
    // ══════════════════════════════════════════════
    private VBox buildRoot() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(32, 36, 32, 36));
        root.getStyleClass().add("content-area");

        // Page header
        VBox header = new VBox(4);
        Label pageTitle = new Label("Expense Management");
        pageTitle.getStyleClass().add("page-title");
        Label pageSub = new Label("Record, categorize and review all business expenses.");
        pageSub.getStyleClass().add("page-subtitle");
        header.getChildren().addAll(pageTitle, pageSub);

        // Two-column row
        HBox mainRow = new HBox(20);
        mainRow.setAlignment(Pos.TOP_LEFT);

        VBox formPanel    = buildFormPanel();
        VBox historyPanel = buildHistoryPanel();

        formPanel.setPrefWidth(320);
        formPanel.setMinWidth(300);
        formPanel.setMaxWidth(340);
        HBox.setHgrow(historyPanel, Priority.ALWAYS);

        mainRow.getChildren().addAll(formPanel, historyPanel);

        // Summary report
        VBox summaryPanel = buildSummaryPanel();

        root.getChildren().addAll(header, mainRow, summaryPanel);
        return root;
    }

    // ══════════════════════════════════════════════
    //  Add Expense form — US-16, US-17
    // ══════════════════════════════════════════════
    private VBox buildFormPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(24, 24, 24, 24));

        Label cardTitle = new Label("Add New Expense");
        cardTitle.getStyleClass().add("card-title");

        titleField = new TextField();
        titleField.setPromptText("e.g. Monthly Rent, Staff Wages");
        titleField.getStyleClass().add("text-input");
        titleField.setMaxWidth(Double.MAX_VALUE);

        categoryCombo = new ComboBox<>(FXCollections.observableArrayList(CATEGORIES));
        categoryCombo.setPromptText("Select category");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.getStyleClass().add("combo-input");

        amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.getStyleClass().add("text-input");
        amountField.setMaxWidth(Double.MAX_VALUE);

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Optional notes...");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        descriptionArea.getStyleClass().add("text-area-input");
        descriptionArea.setMaxWidth(Double.MAX_VALUE);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        feedbackLabel.getStyleClass().add("feedback-label");

        Button saveBtn = new Button("💾  Save Expense");
        saveBtn.getStyleClass().add("primary-btn");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> controller.handleSave());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("secondary-btn");
        clearBtn.setOnAction(e -> clearForm());

        HBox btnRow = new HBox(10, saveBtn, clearBtn);
        HBox.setHgrow(saveBtn, Priority.ALWAYS);

        panel.getChildren().addAll(
                cardTitle,
                fieldBlock("Expense Title *", titleField),
                fieldBlock("Category *",       categoryCombo),
                fieldBlock("Amount (PKR) *",   amountField),
                fieldBlock("Date *",            datePicker),
                fieldBlock("Description",       descriptionArea),
                feedbackLabel,
                btnRow
        );
        return panel;
    }

    // ══════════════════════════════════════════════
    //  Expense History table — US-18
    // ══════════════════════════════════════════════
    private VBox buildHistoryPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(24, 24, 24, 24));

        Label cardTitle = new Label("Expense History");
        cardTitle.getStyleClass().add("card-title");

        // Filter row
        searchField = new TextField();
        searchField.setPromptText("Search by title...");
        searchField.getStyleClass().add("text-input");

        filterCategoryCombo = new ComboBox<>();
        ObservableList<String> opts = FXCollections.observableArrayList("All Categories");
        opts.addAll(CATEGORIES);
        filterCategoryCombo.setItems(opts);
        filterCategoryCombo.setValue("All Categories");
        filterCategoryCombo.getStyleClass().add("combo-input");

        Button filterBtn = new Button("🔍  Filter");
        filterBtn.getStyleClass().add("secondary-btn");
        filterBtn.setOnAction(e -> applyFilter());

        Button clearFilterBtn = new Button("Clear");
        clearFilterBtn.getStyleClass().add("ghost-btn");
        clearFilterBtn.setOnAction(e -> {
            searchField.clear();
            filterCategoryCombo.setValue("All Categories");
            expenseTable.setItems(ExpenseManager.getInstance().getExpenses());
        });

        HBox filterRow = new HBox(8, searchField, filterCategoryCombo, filterBtn, clearFilterBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        // Table sub-header with count badge
        Label histLbl = new Label("All Records");
        histLbl.getStyleClass().add("field-label");

        Label countBadge = new Label();
        countBadge.getStyleClass().add("product-count-badge");
        updateCountBadge(countBadge);
        ExpenseManager.getInstance().getExpenses().addListener(
                (javafx.collections.ListChangeListener<Expense>) c -> updateCountBadge(countBadge)
        );

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox tableHeader = new HBox(8, histLbl, sp, countBadge);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        expenseTable = buildTable();
        expenseTable.setMinHeight(300);
        VBox.setVgrow(expenseTable, Priority.ALWAYS);

        panel.getChildren().addAll(cardTitle, filterRow, tableHeader, expenseTable);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private TableView<Expense> buildTable() {
        TableView<Expense> table = new TableView<>();
        table.getStyleClass().add("product-table");
        table.setPlaceholder(new Label("No expenses recorded yet."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Title
        TableColumn<Expense, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setMinWidth(130);

        // Category — rendered as badge
        TableColumn<Expense, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setMinWidth(130);
        catCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(val);
                badge.getStyleClass().add("category-badge");
                setGraphic(badge);
                setText(null);
            }
        });

        // Amount
        TableColumn<Expense, Double> amtCol = new TableColumn<>("Amount (PKR)");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setMinWidth(120);
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : String.format("PKR %.2f", val));
            }
        });

        // Date
        TableColumn<Expense, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setMinWidth(110);
        dateCol.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            @Override protected void updateItem(LocalDate val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : fmt.format(val));
            }
        });

        // Notes
        TableColumn<Expense, String> descCol = new TableColumn<>("Notes");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setMinWidth(120);

        table.getColumns().addAll(titleCol, catCol, amtCol, dateCol, descCol);
        table.setItems(ExpenseManager.getInstance().getExpenses());
        return table;
    }

    // ══════════════════════════════════════════════
    //  Expense Summary Report — US-19
    // ══════════════════════════════════════════════
    private VBox buildSummaryPanel() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(24, 24, 24, 24));

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = new Label("Expense Summary Report");
        cardTitle.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.getStyleClass().add("secondary-btn");
        refreshBtn.setOnAction(e -> refreshSummary());

        headerRow.getChildren().addAll(cardTitle, spacer, refreshBtn);

        totalLabel = new Label("Total Expenses: PKR 0.00");
        totalLabel.getStyleClass().add("stat-value");
        totalLabel.setStyle("-fx-font-size: 18px;");

        // Horizontally scrollable category breakdown
        categoryCardsRow = new HBox(14);
        categoryCardsRow.setAlignment(Pos.CENTER_LEFT);
        categoryCardsRow.setPadding(new Insets(4, 0, 4, 0));

        ScrollPane cardsScroll = new ScrollPane(categoryCardsRow);
        cardsScroll.setFitToHeight(true);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.getStyleClass().add("add-product-scroll");
        cardsScroll.setPrefHeight(130);
        cardsScroll.setMinHeight(130);

        panel.getChildren().addAll(headerRow, totalLabel, cardsScroll);

        // Live-refresh when expenses change
        ExpenseManager.getInstance().getExpenses().addListener(
                (javafx.collections.ListChangeListener<Expense>) c -> refreshSummary()
        );

        refreshSummary();
        return panel;
    }

    public void refreshSummary() {
        if (totalLabel == null || categoryCardsRow == null) return;

        double total = ExpenseManager.getInstance().getTotalExpenses();
        totalLabel.setText(String.format("Total Expenses: PKR %.2f", total));

        categoryCardsRow.getChildren().clear();
        boolean anyData = false;

        for (String cat : CATEGORIES) {
            double catTotal = ExpenseManager.getInstance().getTotalByCategory(cat);
            if (catTotal <= 0) continue;
            anyData = true;

            VBox card = new VBox(8);
            card.getStyleClass().addAll("card", "stat-card", "stat-card-orange");
            card.setPadding(new Insets(16, 20, 16, 20));
            card.setMinWidth(155);
            card.setMaxWidth(195);
            card.setAlignment(Pos.CENTER_LEFT);

            Text icon = new Text(categoryIcon(cat));
            icon.setStyle("-fx-font-size: 22px;");

            Label valLbl = new Label(String.format("PKR %.2f", catTotal));
            valLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #d97706;");

            Label nameLbl = new Label(cat);
            nameLbl.getStyleClass().add("stat-title");

            card.getChildren().addAll(icon, valLbl, nameLbl);
            categoryCardsRow.getChildren().add(card);
        }

        if (!anyData) {
            Label empty = new Label("No expense data yet — save your first expense above.");
            empty.getStyleClass().add("page-subtitle");
            categoryCardsRow.getChildren().add(empty);
        }
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════
    private void applyFilter() {
        String search   = searchField.getText().trim().toLowerCase();
        String category = filterCategoryCombo.getValue();

        FilteredList<Expense> filtered = new FilteredList<>(
                ExpenseManager.getInstance().getExpenses(), e -> {
            boolean matchTitle = search.isEmpty() || e.getTitle().toLowerCase().contains(search);
            boolean matchCat   = "All Categories".equals(category) || e.getCategory().equals(category);
            return matchTitle && matchCat;
        });
        expenseTable.setItems(filtered);
    }

    private void updateCountBadge(Label badge) {
        int count = ExpenseManager.getInstance().getExpenseCount();
        badge.setText(count + (count == 1 ? " expense" : " expenses"));
    }

    public void clearForm() {
        titleField.clear();
        categoryCombo.setValue(null);
        amountField.clear();
        datePicker.setValue(LocalDate.now());
        descriptionArea.clear();
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        titleField.getStyleClass().remove("input-error");
        amountField.getStyleClass().remove("input-error");
        categoryCombo.getStyleClass().remove("input-error");
        datePicker.getStyleClass().remove("input-error");
    }

    private VBox fieldBlock(String labelText, javafx.scene.Node input) {
        VBox box = new VBox(5);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("field-label");
        box.getChildren().addAll(lbl, input);
        return box;
    }

    private String categoryIcon(String cat) {
        return switch (cat) {
            case "Rent"               -> "🏠";
            case "Utilities"          -> "💡";
            case "Salaries"           -> "👥";
            case "Inventory Purchase" -> "📦";
            case "Marketing"          -> "📣";
            case "Maintenance"        -> "🔧";
            case "Transportation"     -> "🚗";
            default                   -> "💼";
        };
    }
}