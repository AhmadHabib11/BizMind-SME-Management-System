package com.bizmind;

import com.bizmind.manager.ExpenseManager;
import com.bizmind.manager.InventoryManager;
import com.bizmind.manager.SalesManager;
import com.bizmind.model.Business;
import com.bizmind.session.SessionManager;
import com.bizmind.view.*;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class BizMindApp extends Application {

    private static Stage primaryStage;
    private static StackPane mainContainer;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        mainContainer = new StackPane();

        Scene scene = new Scene(mainContainer, 1100, 700);
        java.net.URL cssUrl = getClass().getResource("/com/bizmind/styles/dashboard.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        stage.setTitle("BizMind — SME Management & Decision System");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        showAuth();
    }

    // ── Navigation ─────────────────────────────────────────────────
    public static void navigateTo(Node node) {
        mainContainer.getChildren().setAll(node);
    }

    public static void showAuth() {
        navigateTo(new AuthView().getRoot());
    }

    public static void showOwnerHome() {
        navigateTo(new OwnerHomeView().getRoot());
    }

    public static void showWorkerHome() {
        navigateTo(new WorkerHomeView().getRoot());
    }

    public static void showDashboard() {
        navigateTo(new DashboardView().getRoot());
    }

    public static void showAddBusiness() {
        navigateTo(new AddBusinessView().getRoot());
    }

    public static void showJoinBusiness() {
        navigateTo(new JoinBusinessView().getRoot());
    }

    public static void showPendingRequests(Business business) {
        navigateTo(new PendingRequestsView(business).getRoot());
    }

    public static void logout() {
        InventoryManager.getInstance().clear();
        ExpenseManager.getInstance().clear();
        SalesManager.getInstance().clear();
        SessionManager.getInstance().clear();
        showAuth();
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
