package com.bizmind.view;

import com.bizmind.BizMindApp;
import com.bizmind.db.DatabaseManager;
import com.bizmind.manager.ExpenseManager;
import com.bizmind.manager.InventoryManager;
import com.bizmind.manager.SalesManager;
import com.bizmind.model.Business;
import com.bizmind.model.BusinessMember;
import com.bizmind.session.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkerHomeView {

    private final VBox root;

    public WorkerHomeView() {
        root = new VBox(0);
        root.getStyleClass().add("home-root");
        root.getChildren().addAll(buildTopBar(), buildContent());
    }

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

    private ScrollPane buildContent() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 48, 36, 48));
        content.getStyleClass().add("home-content");

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        Label title = new Label("My Workplaces");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Select a business to enter, or join a new one.");
        sub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button joinBtn = new Button("＋  Join a Business");
        joinBtn.getStyleClass().add("primary-btn");
        joinBtn.setMinHeight(42);
        joinBtn.setOnAction(e -> BizMindApp.showJoinBusiness());

        header.getChildren().addAll(titleBox, spacer, joinBtn);

        FlowPane grid = buildMembershipGrid();
        content.getChildren().addAll(header, grid);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    private FlowPane buildMembershipGrid() {
        FlowPane grid = new FlowPane(20, 20);
        grid.getStyleClass().add("business-grid");

        List<BusinessMember> memberships = loadMemberships();
        if (memberships.isEmpty()) {
            Label empty = new Label("You haven't joined any businesses yet. Click \"＋ Join a Business\" to get started.");
            empty.getStyleClass().add("page-subtitle");
            grid.getChildren().add(empty);
        } else {
            for (BusinessMember m : memberships) {
                grid.getChildren().add(buildMemberCard(m));
            }
        }
        return grid;
    }

    private VBox buildMemberCard(BusinessMember member) {
        VBox card = new VBox(12);
        card.getStyleClass().add("business-card");
        card.setPadding(new Insets(24, 28, 24, 28));
        card.setPrefWidth(280);

        Label nameLabel = new Label(member.getBusinessName());
        nameLabel.getStyleClass().add("business-card-name");
        nameLabel.setWrapText(true);

        Label roleLabel = new Label(member.getRoleDisplay());
        roleLabel.getStyleClass().add("role-badge");

        if ("pending".equals(member.getStatus())) {
            Label pendingLbl = new Label("Awaiting owner approval...");
            pendingLbl.getStyleClass().add("page-subtitle");
            card.getChildren().addAll(nameLabel, roleLabel, pendingLbl);
        } else {
            Button enterBtn = new Button("Enter Business");
            enterBtn.getStyleClass().add("primary-btn");
            enterBtn.setOnAction(e -> enterBusiness(member));
            card.getChildren().addAll(nameLabel, roleLabel, enterBtn);
        }
        return card;
    }

    private void enterBusiness(BusinessMember member) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, owner_id, name, join_code FROM businesses WHERE id = ?");
            ps.setObject(1, member.getBusinessId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            Business business = new Business(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("owner_id"),
                rs.getString("name"),
                rs.getString("join_code")
            );
            SessionManager.getInstance().setCurrentBusiness(business);
            SessionManager.getInstance().setCurrentRole(member.getRole());

            InventoryManager.getInstance().refresh();
            ExpenseManager.getInstance().refresh();
            SalesManager.getInstance().refresh();
            BizMindApp.showDashboard();

        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Failed to enter business: " + ex.getMessage(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    private List<BusinessMember> loadMemberships() {
        List<BusinessMember> list = new ArrayList<>();
        UUID userId = SessionManager.getInstance().getCurrentUser().getId();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT bm.id, bm.business_id, bm.user_id, bm.role, bm.status, b.name AS business_name " +
                "FROM business_members bm JOIN businesses b ON b.id = bm.business_id " +
                "WHERE bm.user_id = ? AND bm.status IN ('accepted','pending') " +
                "ORDER BY bm.requested_at DESC");
            ps.setObject(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BusinessMember m = new BusinessMember(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("business_id"),
                    (UUID) rs.getObject("user_id"),
                    rs.getString("role"),
                    rs.getString("status")
                );
                m.setBusinessName(rs.getString("business_name"));
                list.add(m);
            }
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Failed to load memberships: " + e.getMessage(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
        return list;
    }

    public VBox getRoot() { return root; }
}
