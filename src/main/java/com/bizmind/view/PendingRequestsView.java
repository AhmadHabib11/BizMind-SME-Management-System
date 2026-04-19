package com.bizmind.view;

import com.bizmind.BizMindApp;
import com.bizmind.db.DatabaseManager;
import com.bizmind.model.Business;
import com.bizmind.model.BusinessMember;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PendingRequestsView {

    private final Business business;
    private final StackPane root;

    public PendingRequestsView(Business business) {
        this.business = business;
        root = new StackPane();
        root.getStyleClass().add("auth-root");
        root.getChildren().add(buildContent());
    }

    private VBox buildContent() {
        VBox card = new VBox(20);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(600);
        card.setPadding(new Insets(36, 36, 36, 36));
        StackPane.setAlignment(card, Pos.CENTER);

        Label title = new Label("Join Requests — " + business.getName());
        title.getStyleClass().add("page-title");
        title.setWrapText(true);

        Label sub = new Label("Review and approve or reject worker requests below.");
        sub.getStyleClass().add("page-subtitle");

        VBox requestList = buildRequestList();

        Button backBtn = new Button("← Back to My Businesses");
        backBtn.getStyleClass().add("ghost-btn");
        backBtn.setOnAction(e -> BizMindApp.showOwnerHome());

        card.getChildren().addAll(title, sub, requestList, backBtn);
        return card;
    }

    private VBox buildRequestList() {
        VBox list = new VBox(12);
        List<BusinessMember> requests = loadPendingRequests();

        if (requests.isEmpty()) {
            Label empty = new Label("No pending requests.");
            empty.getStyleClass().add("page-subtitle");
            list.getChildren().add(empty);
            return list;
        }

        for (BusinessMember m : requests) {
            list.getChildren().add(buildRequestRow(m, list));
        }
        return list;
    }

    private HBox buildRequestRow(BusinessMember member, VBox list) {
        HBox row = new HBox(16);
        row.getStyleClass().add("request-row");
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        Label nameLabel = new Label(member.getUserName() != null ? member.getUserName() : "Unknown");
        nameLabel.getStyleClass().add("business-card-name");
        Label roleLabel = new Label(member.getRoleDisplay());
        roleLabel.getStyleClass().add("role-badge");
        info.getChildren().addAll(nameLabel, roleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button acceptBtn = new Button("Accept");
        acceptBtn.getStyleClass().add("primary-btn");
        acceptBtn.setOnAction(e -> resolveRequest(member.getId(), "accepted", row, list));

        Button rejectBtn = new Button("Reject");
        rejectBtn.getStyleClass().add("danger-btn");
        rejectBtn.setOnAction(e -> resolveRequest(member.getId(), "rejected", row, list));

        row.getChildren().addAll(info, spacer, acceptBtn, rejectBtn);
        return row;
    }

    private void resolveRequest(UUID memberId, String newStatus, HBox row, VBox list) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE business_members SET status = ?, resolved_at = NOW() WHERE id = ?");
            ps.setString(1, newStatus);
            ps.setObject(2, memberId);
            ps.executeUpdate();
            list.getChildren().remove(row);
            if (list.getChildren().isEmpty()) {
                Label empty = new Label("No more pending requests.");
                empty.getStyleClass().add("page-subtitle");
                list.getChildren().add(empty);
            }
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    private List<BusinessMember> loadPendingRequests() {
        List<BusinessMember> list = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT bm.id, bm.business_id, bm.user_id, bm.role, bm.status, u.full_name " +
                "FROM business_members bm JOIN users u ON u.id = bm.user_id " +
                "WHERE bm.business_id = ? AND bm.status = 'pending' ORDER BY bm.requested_at ASC");
            ps.setObject(1, business.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BusinessMember m = new BusinessMember(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("business_id"),
                    (UUID) rs.getObject("user_id"),
                    rs.getString("role"),
                    rs.getString("status")
                );
                m.setUserName(rs.getString("full_name"));
                list.add(m);
            }
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Failed to load requests: " + e.getMessage(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
        return list;
    }

    public StackPane getRoot() { return root; }
}
