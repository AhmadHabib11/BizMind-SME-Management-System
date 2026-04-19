package com.bizmind.view;

import com.bizmind.BizMindApp;
import com.bizmind.db.DatabaseManager;
import com.bizmind.session.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.UUID;

public class JoinBusinessView {

    private final StackPane root;

    public JoinBusinessView() {
        root = new StackPane();
        root.getStyleClass().add("auth-root");
        root.getChildren().add(buildCard());
    }

    private VBox buildCard() {
        VBox card = new VBox(18);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(440);
        card.setPadding(new Insets(40));
        StackPane.setAlignment(card, Pos.CENTER);

        Label title = new Label("Join a Business");
        title.getStyleClass().add("auth-brand-name");
        title.setStyle("-fx-font-size: 22px;");

        Label sub = new Label("Enter the 4-digit code shared by the business owner and select your role.");
        sub.getStyleClass().add("page-subtitle");
        sub.setWrapText(true);

        Label codeLbl = new Label("4-Digit Join Code");
        codeLbl.getStyleClass().add("field-label");
        TextField codeField = new TextField();
        codeField.setPromptText("e.g. 4827");
        codeField.getStyleClass().add("text-input");

        Label roleLbl = new Label("Your Role");
        roleLbl.getStyleClass().add("field-label");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Store Manager", "Accountant", "Staff");
        roleCombo.setPromptText("Select your role");
        roleCombo.getStyleClass().add("combo-input");
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        Label feedback = new Label();
        feedback.getStyleClass().addAll("feedback-label");
        feedback.setWrapText(true);
        feedback.setVisible(false);
        feedback.setManaged(false);

        Button joinBtn = new Button("Send Join Request");
        joinBtn.getStyleClass().add("primary-btn");
        joinBtn.setMaxWidth(Double.MAX_VALUE);
        joinBtn.setOnAction(e -> handleJoin(codeField, roleCombo, feedback));

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("ghost-btn");
        backBtn.setOnAction(e -> BizMindApp.showWorkerHome());

        card.getChildren().addAll(title, sub, codeLbl, codeField, roleLbl, roleCombo, feedback, joinBtn, backBtn);
        return card;
    }

    private void handleJoin(TextField codeField, ComboBox<String> roleCombo, Label feedback) {
        String code = codeField.getText().trim();
        String roleDisplay = roleCombo.getValue();

        if (code.isEmpty()) {
            showFeedback(feedback, "Please enter the 4-digit join code.", true);
            return;
        }
        if (roleDisplay == null) {
            showFeedback(feedback, "Please select your role.", true);
            return;
        }

        String role = switch (roleDisplay) {
            case "Store Manager" -> "store_manager";
            case "Accountant"    -> "accountant";
            default              -> "staff";
        };

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            PreparedStatement bps = conn.prepareStatement(
                "SELECT id FROM businesses WHERE join_code = ?");
            bps.setString(1, code);
            ResultSet brs = bps.executeQuery();
            if (!brs.next()) {
                showFeedback(feedback, "No business found with that code. Check and try again.", true);
                return;
            }
            UUID businessId = (UUID) brs.getObject("id");
            UUID userId     = SessionManager.getInstance().getCurrentUser().getId();

            // Check for existing request
            PreparedStatement dup = conn.prepareStatement(
                "SELECT status FROM business_members WHERE business_id = ? AND user_id = ?");
            dup.setObject(1, businessId);
            dup.setObject(2, userId);
            ResultSet drs = dup.executeQuery();
            if (drs.next()) {
                String existingStatus = drs.getString("status");
                if ("pending".equals(existingStatus)) {
                    showFeedback(feedback, "You already have a pending request for this business.", true);
                } else if ("accepted".equals(existingStatus)) {
                    showFeedback(feedback, "You are already a member of this business.", true);
                } else {
                    showFeedback(feedback, "Your previous request was rejected by the owner.", true);
                }
                return;
            }

            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO business_members (business_id, user_id, role, status) VALUES (?, ?, ?, 'pending')");
            ins.setObject(1, businessId);
            ins.setObject(2, userId);
            ins.setString(3, role);
            ins.executeUpdate();

            showFeedback(feedback,
                "Request sent! The business owner will review and approve your request.", false);
            feedback.getStyleClass().removeAll("feedback-error");
            feedback.getStyleClass().add("feedback-success");

        } catch (Exception ex) {
            showFeedback(feedback, "Error: " + ex.getMessage(), true);
        }
    }

    private void showFeedback(Label label, String msg, boolean isError) {
        label.setText(msg);
        label.getStyleClass().removeAll("feedback-error", "feedback-success");
        label.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showFeedback(Label label, String msg) {
        showFeedback(label, msg, true);
    }

    public StackPane getRoot() { return root; }
}
