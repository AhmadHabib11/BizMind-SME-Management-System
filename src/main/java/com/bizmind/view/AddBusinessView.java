package com.bizmind.view;

import com.bizmind.BizMindApp;
import com.bizmind.db.DatabaseManager;
import com.bizmind.session.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.Random;
import java.util.UUID;

public class AddBusinessView {

    private final StackPane root;

    public AddBusinessView() {
        root = new StackPane();
        root.getStyleClass().add("auth-root");
        root.getChildren().add(buildCard());
    }

    private VBox buildCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(440);
        card.setPadding(new Insets(40));
        StackPane.setAlignment(card, Pos.CENTER);

        Label title = new Label("Create a New Business");
        title.getStyleClass().add("auth-brand-name");
        title.setStyle("-fx-font-size: 22px;");

        Label sub = new Label("A unique 4-digit join code will be generated automatically.");
        sub.getStyleClass().add("page-subtitle");
        sub.setWrapText(true);

        Label nameLbl = new Label("Business Name");
        nameLbl.getStyleClass().add("field-label");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Rayyan's Electronics");
        nameField.getStyleClass().add("text-input");

        Label feedback = new Label();
        feedback.getStyleClass().addAll("feedback-label", "feedback-error");
        feedback.setWrapText(true);
        feedback.setVisible(false);
        feedback.setManaged(false);

        Button saveBtn = new Button("Create Business");
        saveBtn.getStyleClass().add("primary-btn");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> handleSave(nameField, feedback));

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("ghost-btn");
        backBtn.setOnAction(e -> BizMindApp.showOwnerHome());

        card.getChildren().addAll(title, sub, nameLbl, nameField, feedback, saveBtn, backBtn);
        return card;
    }

    private void handleSave(TextField nameField, Label feedback) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showFeedback(feedback, "Business name is required.");
            return;
        }
        try {
            String code = generateUniqueCode();
            UUID ownerId = SessionManager.getInstance().getCurrentUser().getId();

            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO businesses (owner_id, name, join_code) VALUES (?, ?, ?)");
            ps.setObject(1, ownerId);
            ps.setString(2, name);
            ps.setString(3, code);
            ps.executeUpdate();

            BizMindApp.showOwnerHome();

        } catch (Exception ex) {
            showFeedback(feedback, "Error: " + ex.getMessage());
        }
    }

    private String generateUniqueCode() throws Exception {
        Connection conn = DatabaseManager.getInstance().getConnection();
        Random rng = new Random();
        String code;
        do {
            code = String.format("%04d", rng.nextInt(10000));
            PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM businesses WHERE join_code = ?");
            ps.setString(1, code);
            if (!ps.executeQuery().next()) break;
        } while (true);
        return code;
    }

    private void showFeedback(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    public StackPane getRoot() { return root; }
}
