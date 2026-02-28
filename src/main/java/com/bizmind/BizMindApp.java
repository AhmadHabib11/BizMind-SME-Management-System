package com.bizmind;

import com.bizmind.view.DashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX Application class for BizMind.
 * Sets up the primary stage and loads the dashboard.
 */
public class BizMindApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        DashboardView dashboard = new DashboardView();
        Scene scene = new Scene(dashboard.getRoot(), 1100, 700);

        // Load CSS stylesheet
        java.net.URL cssUrl = getClass().getResource("/com/bizmind/styles/dashboard.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("BizMind — SME Management & Decision System");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

