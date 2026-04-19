package com.bizmind.view;

import com.bizmind.BizMindApp;
import com.bizmind.db.DatabaseManager;
import com.bizmind.model.User;
import com.bizmind.session.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.UUID;

public class AuthView {

    private final StackPane root;

    public AuthView() {
        root = new StackPane();
        root.getStyleClass().add("auth-root");
        root.getChildren().add(buildCard());
    }

    private VBox buildCard() {
        VBox card = new VBox(24);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(460);
        card.setPadding(new Insets(44, 40, 44, 40));
        StackPane.setAlignment(card, Pos.CENTER);

        VBox brand = new VBox(4);
        brand.setAlignment(Pos.CENTER);
        Label name = new Label("BizMind");
        name.getStyleClass().add("auth-brand-name");
        Label sub = new Label("SME Management System");
        sub.getStyleClass().add("auth-brand-sub");
        brand.getChildren().addAll(name, sub);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("auth-tabs");
        tabs.getTabs().addAll(
            new Tab("Login",    buildLoginPane()),
            new Tab("Register", buildRegisterPane())
        );

        card.getChildren().addAll(brand, tabs);
        return card;
    }

    // ── Login ──────────────────────────────────────────────────────
    private VBox buildLoginPane() {
        VBox pane = new VBox(14);
        pane.setPadding(new Insets(20, 4, 4, 4));

        Label emailLbl = new Label("Email");
        emailLbl.getStyleClass().add("field-label");
        TextField emailField = new TextField();
        emailField.setPromptText("your@email.com");
        emailField.getStyleClass().add("text-input");

        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passField.getStyleClass().add("text-input");

        Label feedback = new Label();
        feedback.getStyleClass().addAll("feedback-label", "feedback-error");
        feedback.setWrapText(true);
        feedback.setVisible(false);
        feedback.setManaged(false);

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("primary-btn");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> handleLogin(emailField, passField, feedback));
        passField.setOnAction(e -> handleLogin(emailField, passField, feedback));

        pane.getChildren().addAll(emailLbl, emailField, passLbl, passField, feedback, loginBtn);
        return pane;
    }

    // ── Register ───────────────────────────────────────────────────
    private VBox buildRegisterPane() {
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(20, 4, 4, 4));

        Label nameLbl = new Label("Full Name");
        nameLbl.getStyleClass().add("field-label");
        TextField nameField = new TextField();
        nameField.setPromptText("Your full name");
        nameField.getStyleClass().add("text-input");

        Label emailLbl = new Label("Email");
        emailLbl.getStyleClass().add("field-label");
        TextField emailField = new TextField();
        emailField.setPromptText("your@email.com");
        emailField.getStyleClass().add("text-input");

        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Minimum 6 characters");
        passField.getStyleClass().add("text-input");

        Label typeLbl = new Label("Account Type");
        typeLbl.getStyleClass().add("field-label");
        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton ownerRb  = new RadioButton("Owner  (I own a business)");
        ownerRb.setToggleGroup(typeGroup);
        ownerRb.setSelected(true);
        RadioButton workerRb = new RadioButton("Worker  (I work at a business)");
        workerRb.setToggleGroup(typeGroup);
        HBox typeRow = new HBox(20, ownerRb, workerRb);
        typeRow.setAlignment(Pos.CENTER_LEFT);

        Label feedback = new Label();
        feedback.getStyleClass().addAll("feedback-label", "feedback-error");
        feedback.setWrapText(true);
        feedback.setVisible(false);
        feedback.setManaged(false);

        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().add("primary-btn");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e ->
            handleRegister(nameField, emailField, passField, ownerRb.isSelected(), feedback));

        pane.getChildren().addAll(
            nameLbl, nameField, emailLbl, emailField,
            passLbl, passField, typeLbl, typeRow,
            feedback, registerBtn
        );
        return pane;
    }

    // ── Handlers ───────────────────────────────────────────────────
    private void handleLogin(TextField emailField, PasswordField passField, Label feedback) {
        String email    = emailField.getText().trim();
        String password = passField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showFeedback(feedback, "Email and password are required.");
            return;
        }
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, full_name, email, password_hash, account_type FROM users WHERE email = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                showFeedback(feedback, "No account found with that email.");
                return;
            }
            String hash = rs.getString("password_hash");
            if (!BCrypt.checkpw(password, hash)) {
                showFeedback(feedback, "Incorrect password.");
                return;
            }
            User user = new User(
                (UUID) rs.getObject("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                hash,
                rs.getString("account_type")
            );
            SessionManager.getInstance().setCurrentUser(user);
            if ("owner".equals(user.getAccountType())) BizMindApp.showOwnerHome();
            else                                        BizMindApp.showWorkerHome();

        } catch (Exception ex) {
            showFeedback(feedback, "Connection error: " + ex.getMessage());
        }
    }

    private void handleRegister(TextField nameField, TextField emailField,
                                PasswordField passField, boolean isOwner, Label feedback) {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passField.getText();
        String type     = isOwner ? "owner" : "worker";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showFeedback(feedback, "All fields are required.");
            return;
        }
        if (password.length() < 6) {
            showFeedback(feedback, "Password must be at least 6 characters.");
            return;
        }
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            PreparedStatement check = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
            check.setString(1, email);
            if (check.executeQuery().next()) {
                showFeedback(feedback, "An account with this email already exists.");
                return;
            }

            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (full_name, email, password_hash, account_type) " +
                "VALUES (?, ?, ?, ?) RETURNING id");
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, type);
            ResultSet rs = ps.executeQuery();
            UUID newId = rs.next() ? (UUID) rs.getObject(1) : null;

            User user = new User(newId, name, email, hash, type);
            SessionManager.getInstance().setCurrentUser(user);
            if ("owner".equals(type)) BizMindApp.showOwnerHome();
            else                      BizMindApp.showWorkerHome();

        } catch (Exception ex) {
            showFeedback(feedback, "Error: " + ex.getMessage());
        }
    }

    private void showFeedback(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    public StackPane getRoot() { return root; }
}
