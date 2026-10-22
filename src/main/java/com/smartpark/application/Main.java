package com.smartpark.application;

import com.smartpark.service.AuthenticationService;
import com.smartpark.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * SmartPark — Intelligent Parking and Congestion Management System
 * Main JavaFX Application Entry Point.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Verify and test DB connection on startup
            boolean dbConnected = DatabaseConnection.testConnection();
            if (dbConnected) {
                System.out.println("[SmartPark Startup] MySQL database connection established successfully.");
            } else {
                System.err.println("[SmartPark Startup Warning] Could not connect to MySQL. Verify service and credentials.");
            }

            // Initialize default admin session for seamless demo evaluation
            AuthenticationService.getInstance().login("admin", "admin123");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_shell.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 800);
            scene.setFill(Color.web("#0f172a"));

            primaryStage.setTitle("SmartPark — Intelligent Parking and Congestion Management System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(720);
            primaryStage.show();

            System.out.println("[SmartPark] JavaFX GUI initialized successfully.");
        } catch (Exception e) {
            System.err.println("[SmartPark Fatal Error] Exception during application startup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Clean up database connections and background workers on application exit
        DatabaseConnection.closeConnection();
        System.out.println("[SmartPark] Application shutdown complete.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}