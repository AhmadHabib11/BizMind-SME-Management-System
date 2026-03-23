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
    private final StackPane  contentArea;
    private final NavigationController navController;

    private HBox dashBtn;
    private HBox inventoryBtn;
    private HBox expenseBtn;
    private HBox reportsBtn;
    private VBox sidebar;

    public DashboardView() {
        root = new BorderPane();
        root.getStyleClass().add("root-pane");

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        navController = new NavigationController(contentArea);

        sidebar = buildSidebar();
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        navController.showDashboardHome();
    }

    private VBox buildSidebar() {
        VBox sb = new VBox();
        sb.getStyleClass().add("sidebar");
        sb.setPrefWidth(240);
        sb.setSpacing(4);
        sb.setPadding(new Insets(0));

        // ── Brand ──
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

        // ── MAIN MENU ──
        Label navLabel = new Label("MAIN MENU");
        navLabel.getStyleClass().add("sidebar-section-label");
        VBox.setMargin(navLabel, new Insets(16, 24, 8, 24));

        dashBtn = createNavButton("📊", "Dashboard", true);
        dashBtn.getStyleClass().add("nav-btn-active");
        dashBtn.setOnMouseClicked(e -> {
            setActive(dashBtn, sb);
            navController.showDashboardHome();
        });

        inventoryBtn = createNavButton("📦", "Inventory", true);
        inventoryBtn.setOnMouseClicked(e -> {
            setActive(inventoryBtn, sb);
            navController.showInventory();
        });

        expenseBtn = createNavButton("📋", "Expenses", true);
        expenseBtn.setOnMouseClicked(e -> {
            setActive(expenseBtn, sb);
            navController.showExpenses();
        });

        reportsBtn = createNavButton("📈", "Reports", true);
        reportsBtn.setOnMouseClicked(e -> {
            setActive(reportsBtn, sb);
            navController.showReports();
        });

        // ── COMING SOON ──
        Label comingSoonLabel = new Label("COMING SOON");
        comingSoonLabel.getStyleClass().add("sidebar-section-label");
        VBox.setMargin(comingSoonLabel, new Insets(24, 24, 8, 24));

        HBox salesBtn     = createNavButton("💰", "Sales",     false);
        HBox analyticsBtn = createNavButton("🧠", "Analytics", false);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label versionLabel = new Label("v2.0.0  •  Sprint 2");
        versionLabel.getStyleClass().add("sidebar-version");
        VBox.setMargin(versionLabel, new Insets(0, 24, 20, 24));

        sb.getChildren().addAll(
                brandBox, sep,
                navLabel, dashBtn, inventoryBtn, expenseBtn, reportsBtn,
                comingSoonLabel, salesBtn, analyticsBtn,
                spacer, versionLabel
        );
        return sb;
    }

    private void setActive(HBox chosen, VBox sb) {
        for (Node node : sb.getChildren()) {
            node.getStyleClass().remove("nav-btn-active");
        }
        chosen.getStyleClass().add("nav-btn-active");
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

    public BorderPane getRoot() {
        return root;
    }
}