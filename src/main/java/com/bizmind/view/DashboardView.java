package com.bizmind.view;

import com.bizmind.BizMindApp;
import com.bizmind.controller.NavigationController;
import com.bizmind.session.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

public class DashboardView {

    private final BorderPane root;
    private final StackPane  contentArea;
    private final NavigationController navController;
    private VBox sidebar;

    public DashboardView() {
        root        = new BorderPane();
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        navController = new NavigationController(contentArea);

        sidebar = buildSidebar();
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        navController.showDashboardHome();
    }

    private VBox buildSidebar() {
        String role = SessionManager.getInstance().getCurrentRole();
        boolean isOwner = "owner".equals(role);

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

        String businessName = SessionManager.getInstance().getCurrentBusiness() != null
            ? SessionManager.getInstance().getCurrentBusiness().getName()
            : "BizMind";

        Label brandName = new Label(businessName);
        brandName.getStyleClass().add("brand-name");
        brandName.setWrapText(true);
        Label brandSub = new Label(formatRole(role));
        brandSub.getStyleClass().add("brand-subtitle");
        brandBox.getChildren().addAll(brandName, brandSub);

        Separator sep = new Separator();
        sep.getStyleClass().add("sidebar-separator");

        Label navLabel = new Label("MAIN MENU");
        navLabel.getStyleClass().add("sidebar-section-label");
        VBox.setMargin(navLabel, new Insets(16, 24, 8, 24));

        // ── Dashboard (all roles) ──
        HBox dashBtn = createNavButton("📊", "Dashboard", true);
        dashBtn.getStyleClass().add("nav-btn-active");
        dashBtn.setOnMouseClicked(e -> { setActive(dashBtn, sb); navController.showDashboardHome(); });

        sb.getChildren().addAll(brandBox, sep, navLabel, dashBtn);

        // ── Inventory (owner, store_manager) ──
        if (isOwner || "store_manager".equals(role)) {
            HBox inventoryBtn = createNavButton("📦", "Inventory", true);
            inventoryBtn.setOnMouseClicked(e -> { setActive(inventoryBtn, sb); navController.showInventory(); });
            sb.getChildren().add(inventoryBtn);
        }

        // ── Expenses (owner, store_manager, accountant) ──
        if (isOwner || "store_manager".equals(role) || "accountant".equals(role)) {
            HBox expenseBtn = createNavButton("📋", "Expenses", true);
            expenseBtn.setOnMouseClicked(e -> { setActive(expenseBtn, sb); navController.showExpenses(); });
            sb.getChildren().add(expenseBtn);
        }

        // ── Sales (owner, store_manager, staff) ──
        if (isOwner || "store_manager".equals(role) || "staff".equals(role)) {
            HBox salesBtn = createNavButton("💰", "Sales", true);
            salesBtn.setOnMouseClicked(e -> { setActive(salesBtn, sb); navController.showSales(); });
            sb.getChildren().add(salesBtn);
        }

        // ── Reports (owner, store_manager, accountant) ──
        if (isOwner || "store_manager".equals(role) || "accountant".equals(role)) {
            HBox reportsBtn = createNavButton("📈", "Reports", true);
            reportsBtn.setOnMouseClicked(e -> { setActive(reportsBtn, sb); navController.showReports(); });
            sb.getChildren().add(reportsBtn);
        }

        // ── Analytics (owner only — live, others see Coming Soon) ──
        Label comingSoon = new Label("ANALYTICS");
        comingSoon.getStyleClass().add("sidebar-section-label");
        VBox.setMargin(comingSoon, new Insets(24, 24, 8, 24));
        sb.getChildren().add(comingSoon);

        if (isOwner) {
            HBox analyticsBtn = createNavButton("🧠", "Analytics", true);
            analyticsBtn.setOnMouseClicked(e -> { setActive(analyticsBtn, sb); navController.showAnalytics(); });
            sb.getChildren().add(analyticsBtn);
        } else {
            HBox analyticsBtn = createNavButton("🧠", "Analytics", false);
            sb.getChildren().add(analyticsBtn);
        }

        // ── Spacer + footer ──
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sb.getChildren().add(spacer);

        // Back to businesses
        Button backBtn = new Button("← My Businesses");
        backBtn.getStyleClass().add("sidebar-footer-btn");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setOnAction(e -> {
            if ("owner".equals(role)) BizMindApp.showOwnerHome();
            else                      BizMindApp.showWorkerHome();
        });
        VBox.setMargin(backBtn, new Insets(0, 16, 4, 16));

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().addAll("sidebar-footer-btn", "sidebar-logout-btn");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> BizMindApp.logout());
        VBox.setMargin(logoutBtn, new Insets(0, 16, 20, 16));

        sb.getChildren().addAll(backBtn, logoutBtn);
        return sb;
    }

    private String formatRole(String role) {
        if (role == null) return "SME Management System";
        return switch (role) {
            case "owner"         -> "Owner";
            case "store_manager" -> "Store Manager";
            case "accountant"    -> "Accountant";
            case "staff"         -> "Staff";
            default              -> role;
        };
    }

    private void setActive(HBox chosen, VBox sb) {
        for (Node node : sb.getChildren()) node.getStyleClass().remove("nav-btn-active");
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

    public BorderPane getRoot() { return root; }
}
