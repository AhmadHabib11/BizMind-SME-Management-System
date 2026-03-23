package com.bizmind.view;

import com.bizmind.manager.ExpenseManager;
import com.bizmind.model.Expense;
import com.bizmind.report.ExpenseReportGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import com.bizmind.BizMindApp;

import java.io.File;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reports Page — US-19 extended.
 * Shows stat summary, 4 charts, and a downloadable PDF report.
 * Uses only JavaFX built-in charts (no extra dependencies for charts).
 * PDF generation uses Apache PDFBox (add to pom.xml — see below).
 */
public class ReportsView {

    private final StackPane contentArea;

    public ReportsView(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void show() {
        contentArea.getChildren().clear();

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
        VBox root = new VBox(24);
        root.setPadding(new Insets(32, 36, 32, 36));
        root.getStyleClass().add("content-area");

        root.getChildren().addAll(
                buildPageHeader(),
                buildStatsSummary(),
                buildChartsRow1(),
                buildChartsRow2(),
                buildDownloadPanel()
        );
        return root;
    }

    // ══════════════════════════════════════════════
    //  Page header
    // ══════════════════════════════════════════════
    private VBox buildPageHeader() {
        VBox header = new VBox(4);
        Label title = new Label("Expense Reports & Analytics");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Visual breakdown of all recorded business expenses.");
        sub.getStyleClass().add("page-subtitle");
        header.getChildren().addAll(title, sub);
        return header;
    }

    // ══════════════════════════════════════════════
    //  Stats summary row
    // ══════════════════════════════════════════════
    private HBox buildStatsSummary() {
        List<Expense> expenses = ExpenseManager.getInstance().getExpenses();

        double total   = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double average = expenses.isEmpty() ? 0 : total / expenses.size();
        double highest = expenses.stream().mapToDouble(Expense::getAmount).max().orElse(0);
        double lowest  = expenses.stream().mapToDouble(Expense::getAmount).min().orElse(0);
        int    count   = expenses.size();

        // Most expensive category
        Map<String, Double> byCat = groupByCategory(expenses);
        String topCat = byCat.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(
                statCard("📋", "Total Expenses",  String.format("PKR %.2f", total),   "stat-card-orange"),
                statCard("🔢", "No. of Records",  String.valueOf(count),               "stat-card-blue"),
                statCard("📊", "Average Expense", String.format("PKR %.2f", average),  "stat-card-purple"),
                statCard("⬆", "Highest Single",  String.format("PKR %.2f", highest),  "stat-card-green"),
                statCard("🏆", "Top Category",    topCat,                              "stat-card-orange")
        );

        for (var card : row.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        return row;
    }

    // ══════════════════════════════════════════════
    //  Charts row 1 — Pie chart + Bar chart (by category)
    // ══════════════════════════════════════════════
    private HBox buildChartsRow1() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);

        VBox pieCard  = buildPieChartCard();
        VBox barCard  = buildCategoryBarChartCard();

        HBox.setHgrow(pieCard, Priority.ALWAYS);
        HBox.setHgrow(barCard, Priority.ALWAYS);
        row.getChildren().addAll(pieCard, barCard);
        return row;
    }

    // ══════════════════════════════════════════════
    //  Charts row 2 — Line chart (over time) + Top 5 bar
    // ══════════════════════════════════════════════
    private HBox buildChartsRow2() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);

        VBox lineCard = buildLineChartCard();
        VBox top5Card = buildTop5BarChartCard();

        HBox.setHgrow(lineCard, Priority.ALWAYS);
        HBox.setHgrow(top5Card, Priority.ALWAYS);
        row.getChildren().addAll(lineCard, top5Card);
        return row;
    }

    // ── Pie chart — Expenses by Category ──
    private VBox buildPieChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 20, 20, 20));

        Label title = new Label("Expenses by Category");
        title.getStyleClass().add("card-title");

        List<Expense> expenses = ExpenseManager.getInstance().getExpenses();
        Map<String, Double> byCat = groupByCategory(expenses);

        if (byCat.isEmpty()) {
            card.getChildren().addAll(title, emptyChartLabel());
            return card;
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        byCat.forEach((cat, amt) ->
                pieData.add(new PieChart.Data(cat + "\nPKR " + String.format("%.0f", amt), amt))
        );

        PieChart pie = new PieChart(pieData);
        pie.setLegendVisible(true);
        pie.setLabelsVisible(true);
        pie.setStartAngle(90);
        pie.setPrefHeight(300);
        pie.setStyle("-fx-background-color: transparent;");

        card.getChildren().addAll(title, pie);
        return card;
    }

    // ── Bar chart — Expenses by Category ──
    private VBox buildCategoryBarChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 20, 20, 20));

        Label title = new Label("Category Comparison");
        title.getStyleClass().add("card-title");

        List<Expense> expenses = ExpenseManager.getInstance().getExpenses();
        Map<String, Double> byCat = groupByCategory(expenses);

        if (byCat.isEmpty()) {
            card.getChildren().addAll(title, emptyChartLabel());
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Category");
        yAxis.setLabel("Amount (PKR)");

        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        bar.setPrefHeight(300);
        bar.setStyle("-fx-background-color: transparent;");
        bar.setBarGap(4);
        bar.setCategoryGap(12);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        byCat.forEach((cat, amt) -> series.getData().add(new XYChart.Data<>(cat, amt)));
        bar.getData().add(series);

        card.getChildren().addAll(title, bar);
        return card;
    }

    // ── Line chart — Expenses over time (by date) ──
    private VBox buildLineChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 20, 20, 20));

        Label title = new Label("Expenses Over Time");
        title.getStyleClass().add("card-title");

        List<Expense> expenses = ExpenseManager.getInstance().getExpenses();

        if (expenses.isEmpty()) {
            card.getChildren().addAll(title, emptyChartLabel());
            return card;
        }

        // Group by Month-Year
        Map<String, Double> byMonth = new LinkedHashMap<>();
        expenses.stream()
                .sorted(Comparator.comparing(Expense::getDate))
                .forEach(e -> {
                    String key = e.getDate().getMonth()
                            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                            + " " + e.getDate().getYear();
                    byMonth.merge(key, e.getAmount(), Double::sum);
                });

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Month");
        yAxis.setLabel("Total (PKR)");

        LineChart<String, Number> line = new LineChart<>(xAxis, yAxis);
        line.setLegendVisible(false);
        line.setPrefHeight(300);
        line.setStyle("-fx-background-color: transparent;");
        line.setCreateSymbols(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Expenses");
        byMonth.forEach((month, amt) -> series.getData().add(new XYChart.Data<>(month, amt)));
        line.getData().add(series);

        card.getChildren().addAll(title, line);
        return card;
    }

    // ── Bar chart — Top 5 highest individual expenses ──
    private VBox buildTop5BarChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20, 20, 20, 20));

        Label title = new Label("Top 5 Highest Expenses");
        title.getStyleClass().add("card-title");

        List<Expense> expenses = ExpenseManager.getInstance().getExpenses();

        if (expenses.isEmpty()) {
            card.getChildren().addAll(title, emptyChartLabel());
            return card;
        }

        List<Expense> top5 = expenses.stream()
                .sorted(Comparator.comparingDouble(Expense::getAmount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Expense");
        yAxis.setLabel("Amount (PKR)");

        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        bar.setPrefHeight(300);
        bar.setStyle("-fx-background-color: transparent;");
        bar.setBarGap(4);
        bar.setCategoryGap(16);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        top5.forEach(e -> series.getData().add(
                new XYChart.Data<>(truncate(e.getTitle(), 14), e.getAmount())
        ));
        bar.getData().add(series);

        card.getChildren().addAll(title, bar);
        return card;
    }

    // ══════════════════════════════════════════════
    //  Download panel
    // ══════════════════════════════════════════════
    private VBox buildDownloadPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(24, 24, 24, 24));

        Label cardTitle = new Label("Generate & Download Report");
        cardTitle.getStyleClass().add("card-title");

        Label desc = new Label(
                "Export a full PDF report containing all expense records, " +
                "category totals, and a financial summary.");
        desc.getStyleClass().add("page-subtitle");
        desc.setWrapText(true);

        // Report options row
        HBox optionsRow = new HBox(16);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        // Date range filter for report
        Label fromLbl = new Label("From:");
        fromLbl.getStyleClass().add("field-label");
        DatePicker fromDate = new DatePicker();
        fromDate.setPromptText("Start date");

        Label toLbl = new Label("To:");
        toLbl.getStyleClass().add("field-label");
        DatePicker toDate = new DatePicker();
        toDate.setPromptText("End date");

        Label catLbl = new Label("Category:");
        catLbl.getStyleClass().add("field-label");
        ComboBox<String> catFilter = new ComboBox<>();
        ObservableList<String> opts = FXCollections.observableArrayList("All Categories");
        opts.addAll(ExpenseView.CATEGORIES);
        catFilter.setItems(opts);
        catFilter.setValue("All Categories");
        catFilter.getStyleClass().add("combo-input");

        optionsRow.getChildren().addAll(
                fromLbl, fromDate,
                toLbl, toDate,
                catLbl, catFilter
        );

        // Feedback label
        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.setVisible(false);
        feedback.setManaged(false);
        feedback.getStyleClass().add("feedback-label");

        // Download button
        Button downloadBtn = new Button("📄  Download PDF Report");
        downloadBtn.getStyleClass().add("primary-btn");
        downloadBtn.setOnAction(e -> handleDownload(fromDate, toDate, catFilter, feedback));

        HBox btnRow = new HBox(12, downloadBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(cardTitle, desc, optionsRow, feedback, btnRow);
        return panel;
    }

    // ══════════════════════════════════════════════
    //  PDF download handler
    // ══════════════════════════════════════════════
    private void handleDownload(DatePicker fromDate, DatePicker toDate,
                                ComboBox<String> catFilter, Label feedback) {

        // Filter expenses
        List<Expense> all = new ArrayList<>(ExpenseManager.getInstance().getExpenses());

        if (fromDate.getValue() != null) {
            all = all.stream()
                    .filter(e -> !e.getDate().isBefore(fromDate.getValue()))
                    .collect(Collectors.toList());
        }
        if (toDate.getValue() != null) {
            all = all.stream()
                    .filter(e -> !e.getDate().isAfter(toDate.getValue()))
                    .collect(Collectors.toList());
        }
        if (!"All Categories".equals(catFilter.getValue())) {
            String cat = catFilter.getValue();
            all = all.stream()
                    .filter(e -> e.getCategory().equals(cat))
                    .collect(Collectors.toList());
        }

        if (all.isEmpty()) {
            showFeedback(feedback, "⚠ No expense records match the selected filters.", true);
            return;
        }

        // File chooser
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Expense Report");
        chooser.setInitialFileName("BizMind_Expense_Report.pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        File file = chooser.showSaveDialog(BizMindApp.getPrimaryStage());

        if (file == null) return; // user cancelled

        try {
            ExpenseReportGenerator.generate(all, file);
            showFeedback(feedback, "✓ Report saved to: " + file.getAbsolutePath(), false);
        } catch (Exception ex) {
            showFeedback(feedback, "✗ Failed to generate report: " + ex.getMessage(), true);
            ex.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════
    private VBox statCard(String icon, String title, String value, String style) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "stat-card", style);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setAlignment(Pos.CENTER_LEFT);

        Text iconText = new Text(icon);
        iconText.setStyle("-fx-font-size: 24px;");

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("stat-value");
        valLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        valLbl.setWrapText(true);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("stat-title");

        card.getChildren().addAll(iconText, valLbl, titleLbl);
        return card;
    }

    private Map<String, Double> groupByCategory(List<Expense> expenses) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Expense e : expenses) {
            map.merge(e.getCategory(), e.getAmount(), Double::sum);
        }
        return map;
    }

    private Label emptyChartLabel() {
        Label lbl = new Label("No expense data available yet.\nAdd expenses to see this chart.");
        lbl.getStyleClass().add("page-subtitle");
        lbl.setStyle("-fx-padding: 40 0 40 0; -fx-alignment: center;");
        lbl.setWrapText(true);
        return lbl;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private void showFeedback(Label label, String message, boolean isError) {
        label.setText(message);
        label.getStyleClass().removeAll("feedback-success", "feedback-error");
        label.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
        label.setVisible(true);
        label.setManaged(true);
    }
}
