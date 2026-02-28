package com.bizmind.view;

import com.bizmind.controller.NavigationController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * Main dashboard layout with persistent sidebar and swappable center content.
 */
public class DashboardView {

    private final BorderPane root;
    private final StackPane contentArea;
    private final NavigationController navController;

    public DashboardView() {
        root = new BorderPane();
        root.getStyleClass().add("root-pane");

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        navController = new NavigationController(contentArea);

        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        // Show dashboard home by default
        navController.showDashboardHome();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        sidebar.setSpacing(4);
        sidebar.setPadding(new Insets(0));

        // ── Brand header ──
        VBox brandBox = new VBox(2);
        brandBox.getStyleClass().add("sidebar-brand");
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(28, 24, 24, 24));

        Label brandName = new Label("BizMind");
        brandName.getStyleClass().add("brand-name");

        Label brandSub = new Label("SME Management System");
        brandSub.getStyleClass().add("brand-subtitle");

        brandBox.getChildren().addAll(brandName, brandSub);

        Separator sep = new Separator();
        sep.getStyleClass().add("sidebar-separator");

        // ── Navigation section ──
        Label navLabel = new Label("MAIN MENU");
        navLabel.getStyleClass().add("sidebar-section-label");
        VBox.setMargin(navLabel, new Insets(16, 24, 8, 24));

        // Dashboard button
        HBox dashBtn = createNavButton("📊", "Dashboard", true);
        dashBtn.setOnMouseClicked(e -> {
            clearActiveStates(sidebar);
            dashBtn.getStyleClass().add("nav-btn-active");
            navController.showDashboardHome();
        });
        dashBtn.getStyleClass().add("nav-btn-active");

        // Inventory button
        HBox inventoryBtn = createNavButton("📦", "Inventory", true);
        inventoryBtn.setOnMouseClicked(e -> {
            clearActiveStates(sidebar);
            inventoryBtn.getStyleClass().add("nav-btn-active");
            navController.showInventory();
        });

        // ── Coming Soon section ──
        Label comingSoonLabel = new Label("COMING SOON");
        comingSoonLabel.getStyleClass().add("sidebar-section-label");
        VBox.setMargin(comingSoonLabel, new Insets(24, 24, 8, 24));

        HBox salesBtn = createNavButton("💰", "Sales", false);
        HBox expenseBtn = createNavButton("📋", "Expenses", false);
        HBox reportsBtn = createNavButton("📈", "Reports", false);
        HBox analyticsBtn = createNavButton("🧠", "Analytics", false);

        // Spacer to push version to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── Version footer ──
        Label versionLabel = new Label("v1.0.0  •  Sprint 1");
        versionLabel.getStyleClass().add("sidebar-version");
        VBox.setMargin(versionLabel, new Insets(0, 24, 20, 24));

        sidebar.getChildren().addAll(
                brandBox, sep,
                navLabel, dashBtn, inventoryBtn,
                comingSoonLabel, salesBtn, expenseBtn, reportsBtn, analyticsBtn,
                spacer, versionLabel
        );

        return sidebar;
    }

    private HBox createNavButton(String icon, String text, boolean enabled) {
        HBox btn = new HBox(12);
        btn.getStyleClass().add("nav-btn");
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 24, 10, 24));

        Text iconText = new Text(icon);
        iconText.getStyleClass().add("nav-icon");

        Label label = new Label(text);
        label.getStyleClass().add("nav-label");

        if (!enabled) {
            btn.getStyleClass().add("nav-btn-disabled");
            Label badge = new Label("Soon");
            badge.getStyleClass().add("coming-soon-badge");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            btn.getChildren().addAll(iconText, label, sp, badge);
        } else {
            btn.getChildren().addAll(iconText, label);
        }

        return btn;
    }

    private void clearActiveStates(VBox sidebar) {
        for (Node node : sidebar.getChildren()) {
            node.getStyleClass().remove("nav-btn-active");
        }
    }

    public BorderPane getRoot() {
        return root;
    }
}

