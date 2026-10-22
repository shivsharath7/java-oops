package com.smartpark.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton database connection manager for SmartPark.
 * Reads configuration from config/db.properties or classpath resource.
 */
public class DatabaseConnection {

    private static String URL = "jdbc:mysql://localhost:3306/smartpark_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000";
    private static String USER = "root";
    private static String PASSWORD = "SmartPark@123";

    private static Connection connection = null;

    static {
        loadConfiguration();
    }

    private DatabaseConnection() {
        // Private constructor for singleton
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        try {
            File externalFile = new File("config/db.properties");
            if (externalFile.exists()) {
                try (InputStream in = new FileInputStream(externalFile)) {
                    props.load(in);
                }
            } else {
                try (InputStream in = DatabaseConnection.class.getResourceAsStream("/config/db.properties")) {
                    if (in != null) {
                        props.load(in);
                    }
                }
            }

            if (props.containsKey("db.url")) {
                URL = props.getProperty("db.url");
            }
            if (props.containsKey("db.user")) {
                USER = props.getProperty("db.user");
            }
            if (props.containsKey("db.password")) {
                PASSWORD = props.getProperty("db.password");
            }
        } catch (Exception e) {
            System.err.println("[DB Config] Warning: Could not load external db.properties, using defaults: " + e.getMessage());
        }
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DB Error] MySQL JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB Error] Could not connect to MySQL: " + e.getMessage());
        }
        return connection;
    }

    /**
     * Creates a new separate Connection for transactions or concurrent worker threads.
     */
    public static Connection createNewConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver class not found", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("[DB] Database connection closed.");
            } catch (SQLException e) {
                System.err.println("[DB Error] Error closing connection: " + e.getMessage());
            }
        }
    }
}