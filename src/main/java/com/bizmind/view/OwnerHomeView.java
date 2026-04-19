package com.bizmind.view;

import com.bizmind.BizMindApp;
import com.bizmind.db.DatabaseManager;
import com.bizmind.manager.ExpenseManager;
import com.bizmind.manager.InventoryManager;
import com.bizmind.manager.SalesManager;
import com.bizmind.model.Business;
import com.bizmind.session.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OwnerHomeView {

    private final VBox root;

    public OwnerHomeView() {
        root = new VBox(0);
        root.getStyleClass().add("home-root");
        root.getChildren().addAll(buildTopBar(), buildContent());
    }

    // ── Top bar with logout ────────────────────────────────────────
    private HBox buildTopBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("home-topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 28, 16, 28));

        Label brand = new Label("BizMind");
        brand.getStyleClass().add("home-topbar-brand");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String name = SessionManager.getInstance().getCurrentUser().getFullName();
        Label userLabel = new Label("Hi, " + name);
        userLabel.getStyleClass().add("home-topbar-user");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("ghost-btn");
        logoutBtn.setOnAction(e -> BizMindApp.logout());

        bar.getChildren().addAll(brand, spacer, userLabel, logoutBtn);
        return bar;
    }

    // ── Main content ───────────────────────────────────────────────
    private ScrollPane buildContent() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 48, 36, 48));
        content.getStyleClass().add("home-content");

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        Label title = new Label("My Businesses");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Click a business to manage it, or add a new one below.");
        sub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("＋  New Business");
        addBtn.getStyleClass().add("primary-btn");
        addBtn.setMinHeight(42);
        addBtn.setOnAction(e -> BizMindApp.showAddBusiness());

        header.getChildren().addAll(titleBox, spacer, addBtn);

        // Business grid
        FlowPane grid = buildBusinessGrid();

        content.getChildren().addAll(header, grid);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    private FlowPane buildBusinessGrid() {
        FlowPane grid = new FlowPane(20, 20);
        grid.getStyleClass().add("business-grid");

        List<Business> businesses = loadOwnerBusinesses();
        if (businesses.isEmpty()) {
            Label empty = new Label("You don't own any businesses yet. Click \"＋ New Business\" to create one.");
            empty.getStyleClass().add("page-subtitle");
            grid.getChildren().add(empty);
        } else {
            for (Business b : businesses) {
                grid.getChildren().add(buildBusinessCard(b));
            }
        }
        return grid;
    }

    private VBox buildBusinessCard(Business business) {
        VBox card = new VBox(12);
        card.getStyleClass().add("business-card");
        card.setPadding(new Insets(24, 28, 24, 28));
        card.setPrefWidth(280);

        Label nameLabel = new Label(business.getName());
        nameLabel.getStyleClass().add("business-card-name");
        nameLabel.setWrapText(true);

        HBox codeBox = new HBox(8);
        codeBox.setAlignment(Pos.CENTER_LEFT);
        Label codeLbl = new Label("Join Code:");
        codeLbl.getStyleClass().add("field-label");
        Label codeVal = new Label(business.getJoinCode());
        codeVal.getStyleClass().add("join-code-badge");
        codeBox.getChildren().addAll(codeLbl, codeVal);

        // Pending requests count
        int pending = countPendingRequests(business.getId());
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Button enterBtn = new Button("Enter Business");
        enterBtn.getStyleClass().add("primary-btn");
        enterBtn.setOnAction(e -> enterBusiness(business));

        if (pending > 0) {
            Button reqBtn = new Button("Requests (" + pending + ")");
            reqBtn.getStyleClass().add("secondary-btn");
            reqBtn.setOnAction(e -> BizMindApp.showPendingRequests(business));
            btnRow.getChildren().addAll(enterBtn, reqBtn);
        } else {
            btnRow.getChildren().add(enterBtn);
        }

        card.getChildren().addAll(nameLabel, codeBox, btnRow);
        return card;
    }

    private void enterBusiness(Business business) {
        SessionManager.getInstance().setCurrentBusiness(business);
        SessionManager.getInstance().setCurrentRole("owner");
        try {
            InventoryManager.getInstance().refresh();
            ExpenseManager.getInstance().refresh();
            SalesManager.getInstance().refresh();
        } catch (Exception ex) {
            showError("Failed to load business data: " + ex.getMessage());
            return;
        }
        BizMindApp.showDashboard();
    }

    private List<Business> loadOwnerBusinesses() {
        List<Business> list = new ArrayList<>();
        UUID ownerId = SessionManager.getInstance().getCurrentUser().getId();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, owner_id, name, join_code FROM businesses WHERE owner_id = ? ORDER BY created_at DESC");
            ps.setObject(1, ownerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Business(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("owner_id"),
                    rs.getString("name"),
                    rs.getString("join_code")
                ));
            }
        } catch (Exception e) {
            showError("Failed to load businesses: " + e.getMessage());
        }
        return list;
    }

    private int countPendingRequests(UUID businessId) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM business_members WHERE business_id = ? AND status = 'pending'");
            ps.setObject(1, businessId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public VBox getRoot() { return root; }
}
