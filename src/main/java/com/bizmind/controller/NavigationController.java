package com.bizmind.controller;

import com.bizmind.manager.InventoryManager;
import com.bizmind.manager.ExpenseManager;
import com.bizmind.manager.SalesManager;
import com.bizmind.manager.SalesManager;
import com.bizmind.view.InventoryView;
import com.bizmind.view.ExpenseView;
import com.bizmind.view.ReportsView;
import com.bizmind.view.RecordSaleView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * Handles sidebar navigation — swaps center content in the dashboard.
 */
public class NavigationController {

    private final StackPane contentArea;
    private InventoryView inventoryView;
    private ExpenseView   expenseView;
    private RecordSaleView salesView;
    private ReportsView   reportsView;

    public NavigationController(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void showDashboardHome() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(buildDashboardHome());
    }

    public void showInventory() {
        if (inventoryView == null) inventoryView = new InventoryView(contentArea);
        inventoryView.show();
    }

    public void showExpenses() {
        if (expenseView == null) expenseView = new ExpenseView(contentArea);
        expenseView.show();
    }

    public void showSales() {
        if (salesView == null) salesView = new RecordSaleView(contentArea);
        salesView.show();
    }

    public void showReports() {
        // Always rebuild so charts reflect latest data
        reportsView = new ReportsView(contentArea);
        reportsView.show();
    }

    // ══════════════════════════════════════════════
    //  Dashboard home
    // ══════════════════════════════════════════════
    private VBox buildDashboardHome() {
        VBox home = new VBox(28);
        home.setPadding(new Insets(32, 40, 32, 40));
        home.getStyleClass().add("content-area");

        // Header
        VBox headerBox = new VBox(4);
        Label welcome = new Label("Welcome to BizMind");
        welcome.getStyleClass().add("page-title");
        Label welcomeSub = new Label("Your SME management dashboard — all your business in one place.");
        welcomeSub.getStyleClass().add("page-subtitle");
        headerBox.getChildren().addAll(welcome, welcomeSub);

        // Stats row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        VBox productsCard = buildStatCard("📦", "Total Products",
                String.valueOf(InventoryManager.getInstance().getProductCount()), "stat-card-blue");

        VBox salesCard = buildStatCard("💰", "Total Sales Revenue",
                String.format("PKR %.2f", SalesManager.getInstance().getTotalSalesRevenue()), "stat-card-green");

        VBox expenseCard = buildStatCard("📋", "Total Expenses",
                String.format("PKR %.2f", ExpenseManager.getInstance().getTotalExpenses()),
                "stat-card-orange");

        VBox revenueCard = buildStatCard("📈", "Revenue", "—", "stat-card-purple");

        // Live listeners
        InventoryManager.getInstance().getProducts().addListener(
                (javafx.collections.ListChangeListener<com.bizmind.model.Product>) c -> {
                    Label lbl = (Label) productsCard.lookup(".stat-value");
                    if (lbl != null)
                        lbl.setText(String.valueOf(InventoryManager.getInstance().getProductCount()));
                });

        SalesManager.getInstance().getSales().addListener(
                (javafx.collections.ListChangeListener<com.bizmind.model.Sale>) c -> {
                    Label lbl = (Label) salesCard.lookup(".stat-value");
                    if (lbl != null)
                        lbl.setText(String.format("PKR %.2f",
                                SalesManager.getInstance().getTotalSalesRevenue()));
                });

        ExpenseManager.getInstance().getExpenses().addListener(
                (javafx.collections.ListChangeListener<com.bizmind.model.Expense>) c -> {
                    Label lbl = (Label) expenseCard.lookup(".stat-value");
                    if (lbl != null)
                        lbl.setText(String.format("PKR %.2f",
                                ExpenseManager.getInstance().getTotalExpenses()));
                });

        statsRow.getChildren().addAll(productsCard, salesCard, expenseCard, revenueCard);
        for (var card : statsRow.getChildren()) HBox.setHgrow(card, Priority.ALWAYS);

        // Quick actions
        VBox actionsCard = new VBox(16);
        actionsCard.getStyleClass().add("card");
        actionsCard.setPadding(new Insets(24, 28, 24, 28));

        Label actionsTitle = new Label("Quick Actions");
        actionsTitle.getStyleClass().add("card-title");

        HBox actionsRow = new HBox(16);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox addProductAction = buildActionTile("📦", "Add Product",   "Manage inventory");
        addProductAction.setOnMouseClicked(e -> showInventory());

        VBox addExpenseAction = buildActionTile("📋", "Add Expense",   "Record business costs");
        addExpenseAction.setOnMouseClicked(e -> showExpenses());

        VBox recordSaleAction = buildActionTile("💰", "Record Sale", "Track product sales");
        recordSaleAction.setOnMouseClicked(e -> showSales());

        VBox reportsAction = buildActionTile("📈", "View Reports",   "Charts & PDF export");
        reportsAction.setOnMouseClicked(e -> showReports());

        actionsRow.getChildren().addAll(addProductAction, addExpenseAction, recordSaleAction, reportsAction);
        actionsCard.getChildren().addAll(actionsTitle, actionsRow);

        home.getChildren().addAll(headerBox, statsRow, actionsCard);
        return home;
    }

    private VBox buildStatCard(String icon, String title, String value, String styleClass) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "stat-card", styleClass);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMinWidth(180);

        Text iconText = new Text(icon);
        iconText.setStyle("-fx-font-size: 28px;");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        card.getChildren().addAll(iconText, valueLabel, titleLabel);
        return card;
    }

    private VBox buildActionTile(String icon, String title, String desc) {
        VBox tile = new VBox(8);
        tile.getStyleClass().add("action-tile");
        tile.setPadding(new Insets(20, 24, 20, 24));
        tile.setMinWidth(180);
        tile.setAlignment(Pos.CENTER_LEFT);

        Text iconText = new Text(icon);
        iconText.setStyle("-fx-font-size: 24px;");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("action-tile-title");

        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("action-tile-desc");

        tile.getChildren().addAll(iconText, titleLabel, descLabel);
        return tile;
    }
}