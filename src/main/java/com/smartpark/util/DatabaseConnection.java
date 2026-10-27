package com.smartpark.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance, thread-safe connection pool and database manager for SmartPark.
 * Reads configuration from config/db.properties, environment variables, or classpath resource.
 * Wraps connections in dynamic proxies to return them to the pool upon close(),
 * supporting concurrent UI threads and background tasks without connection collisions.
 */
public class DatabaseConnection {

    private static String URL = "jdbc:mysql://localhost:3306/smartpark_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000";
    private static String USER = "root";
    private static String PASSWORD = "SmartPark@123";
    private static int MAX_POOL_SIZE = 10;

    private static final BlockingQueue<Connection> pool = new LinkedBlockingQueue<>();
    private static final AtomicInteger createdConnections = new AtomicInteger(0);
    private static volatile boolean initialized = false;

    static {
        loadConfiguration();
    }

    private DatabaseConnection() {
        // Private constructor for utility class
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
                URL = props.getProperty("db.url").trim();
            }
            if (props.containsKey("db.user")) {
                USER = props.getProperty("db.user").trim();
            }
            if (props.containsKey("db.password")) {
                PASSWORD = props.getProperty("db.password").trim();
            }
            if (props.containsKey("db.pool.maxActive")) {
                try {
                    MAX_POOL_SIZE = Integer.parseInt(props.getProperty("db.pool.maxActive").trim());
                } catch (NumberFormatException ignored) {}
            }

            // Environment variable overrides (precedence for containerized/cloud deployments)
            String envUrl = System.getenv("SMARTPARK_DB_URL");
            if (envUrl != null && !envUrl.isBlank()) URL = envUrl.trim();

            String envUser = System.getenv("SMARTPARK_DB_USER");
            if (envUser != null && !envUser.isBlank()) USER = envUser.trim();

            String envPass = System.getenv("SMARTPARK_DB_PASSWORD");
            if (envPass != null && !envPass.isBlank()) PASSWORD = envPass.trim();

            // Register driver once
            Class.forName("com.mysql.cj.jdbc.Driver");
            initialized = true;
        } catch (Exception e) {
            System.err.println("[DB Config] Warning: Configuration initialization issue: " + e.getMessage());
        }
    }

    /**
     * Obtains a thread-safe pooled connection.
     * When closed by try-with-resources, the connection is automatically returned to the pool.
     *
     * @return a proxy-wrapped java.sql.Connection
     * @throws SQLException if a connection cannot be established or pool is exhausted
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
        }

        Connection realConn = null;

        // 1. Try to reuse an available idle connection from the pool
        while (!pool.isEmpty()) {
            Connection candidate = pool.poll();
            if (candidate != null) {
                try {
                    if (!candidate.isClosed() && candidate.isValid(1)) {
                        realConn = candidate;
                        break;
                    } else {
                        try { candidate.close(); } catch (Exception ignored) {}
                        createdConnections.decrementAndGet();
                    }
                } catch (SQLException e) {
                    try { candidate.close(); } catch (Exception ignored) {}
                    createdConnections.decrementAndGet();
                }
            }
        }

        // 2. If no idle connection and pool capacity remains, create a new physical connection
        if (realConn == null) {
            if (createdConnections.get() < MAX_POOL_SIZE) {
                realConn = createNewPhysicalConnection();
                createdConnections.incrementAndGet();
            } else {
                // Wait for an available connection with timeout
                try {
                    realConn = pool.poll(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for database connection", e);
                }

                if (realConn == null || realConn.isClosed() || !realConn.isValid(1)) {
                    // Pool timeout or invalid connection: create on-demand
                    realConn = createNewPhysicalConnection();
                }
            }
        }

        // 3. Return dynamic proxy that intercepts close() to recycle connection into pool
        return createPooledProxy(realConn);
    }

    private static Connection createNewPhysicalConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found on classpath", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static Connection createPooledProxy(final Connection physicalConnection) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean closed = false;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                if ("close".equals(methodName)) {
                    if (!closed) {
                        closed = true;
                        if (!physicalConnection.isClosed() && physicalConnection.isValid(1)) {
                            // Ensure transaction is clean before returning to pool
                            if (!physicalConnection.getAutoCommit()) {
                                physicalConnection.setAutoCommit(true);
                            }
                            pool.offer(physicalConnection);
                        } else {
                            try { physicalConnection.close(); } catch (Exception ignored) {}
                            createdConnections.decrementAndGet();
                        }
                    }
                    return null;
                }

                if ("isClosed".equals(methodName)) {
                    return closed || physicalConnection.isClosed();
                }

                if (closed) {
                    throw new SQLException("Connection has already been closed.");
                }

                return method.invoke(physicalConnection, args);
            }
        };

        return (Connection) Proxy.newProxyInstance(
                DatabaseConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    /**
     * Creates an unpooled, direct Connection for transactions requiring manual lifecycle management.
     */
    public static Connection createNewConnection() throws SQLException {
        return createNewPhysicalConnection();
    }

    /**
     * Tests connectivity to the MySQL database without consuming pool slots.
     */
    public static boolean testConnection() {
        try (Connection conn = createNewPhysicalConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Shuts down the connection pool, closing all physical database connections.
     */
    public static void closeConnection() {
        Connection conn;
        while ((conn = pool.poll()) != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ignored) {}
        }
        createdConnections.set(0);
        System.out.println("[DB] Database connection pool shut down and all connections closed.");
    }
}