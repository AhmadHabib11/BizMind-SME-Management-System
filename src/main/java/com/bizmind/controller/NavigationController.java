package com.bizmind.controller;

import com.bizmind.manager.InventoryManager;
import com.bizmind.view.InventoryView;
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

    public NavigationController(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void showDashboardHome() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(buildDashboardHome());
    }

    public void showInventory() {
        if (inventoryView == null) {
            inventoryView = new InventoryView(contentArea);
        }
        inventoryView.show();
    }

    // ── Dashboard home content ──
    private VBox buildDashboardHome() {
        VBox home = new VBox(28);
        home.setPadding(new Insets(32, 40, 32, 40));
        home.getStyleClass().add("content-area");

        // Welcome header
        VBox headerBox = new VBox(4);
        Label welcome = new Label("Welcome to BizMind");
        welcome.getStyleClass().add("page-title");

        Label welcomeSub = new Label("Your SME management dashboard — all your business in one place.");
        welcomeSub.getStyleClass().add("page-subtitle");

        headerBox.getChildren().addAll(welcome, welcomeSub);

        // Stats cards row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        VBox productsCard = buildStatCard("📦", "Total Products",
                String.valueOf(InventoryManager.getInstance().getProductCount()),
                "stat-card-blue");

        VBox salesCard = buildStatCard("💰", "Total Sales", "—", "stat-card-green");
        VBox expenseCard = buildStatCard("📋", "Expenses", "—", "stat-card-orange");
        VBox revenueCard = buildStatCard("📈", "Revenue", "—", "stat-card-purple");

        // Update product count dynamically
        InventoryManager.getInstance().getProducts().addListener(
                (javafx.collections.ListChangeListener<com.bizmind.model.Product>) c -> {
                    Label countLabel = (Label) productsCard.lookup(".stat-value");
                    if (countLabel != null) {
                        countLabel.setText(String.valueOf(InventoryManager.getInstance().getProductCount()));
                    }
                }
        );

        statsRow.getChildren().addAll(productsCard, salesCard, expenseCard, revenueCard);

        // Quick actions card
        VBox actionsCard = new VBox(16);
        actionsCard.getStyleClass().add("card");
        actionsCard.setPadding(new Insets(24, 28, 24, 28));

        Label actionsTitle = new Label("Quick Actions");
        actionsTitle.getStyleClass().add("card-title");

        HBox actionsRow = new HBox(16);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox addProductAction = buildActionTile("📦", "Add Product", "Go to Inventory to add products");
        addProductAction.setOnMouseClicked(e -> showInventory());

        VBox salesAction = buildActionTile("💰", "Record Sale", "Coming in Sprint 2");
        salesAction.getStyleClass().add("action-tile-disabled");

        VBox reportAction = buildActionTile("📈", "View Reports", "Coming in Sprint 2");
        reportAction.getStyleClass().add("action-tile-disabled");

        actionsRow.getChildren().addAll(addProductAction, salesAction, reportAction);
        actionsCard.getChildren().addAll(actionsTitle, actionsRow);

        home.getChildren().addAll(headerBox, statsRow, actionsCard);
        return home;
    }

    private VBox buildStatCard(String icon, String title, String value, String styleClass) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "stat-card", styleClass);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMinWidth(180);
        card.setMaxWidth(220);
        HBox.setHgrow(card, Priority.ALWAYS);

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
        tile.setMinWidth(200);
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

