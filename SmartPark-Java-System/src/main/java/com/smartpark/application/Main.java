package com.smartpark.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * SmartPark Main JavaFX Application
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 750);
            scene.setFill(Color.web("#0f172a"));
            
            primaryStage.setTitle("SmartPark – Intelligent Parking Management System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(680);
            primaryStage.show();

            System.out.println("[SmartPark] JavaFX GUI initialized successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}