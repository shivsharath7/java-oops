package com.smartpark.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database connection manager for SmartPark.
 */
public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/smartpark_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    private static Connection connection = null;

    private DatabaseConnection() {
        // Private constructor for singleton
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DB] Connected to MySQL successfully.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DB Error] MySQL JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB Error] Could not connect to MySQL: " + e.getMessage());
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[DB] Database connection closed.");
            } catch (SQLException e) {
                System.err.println("[DB Error] Error closing connection: " + e.getMessage());
            }
        }
    }
}