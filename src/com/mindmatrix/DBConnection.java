package com.mindmatrix;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * DBConnection – Lightweight pooled connection manager for MySQL database.
 * Executes database initialization once on application boot and reuses open connections
 * to minimize TCP handshake latency.
 */
public class DBConnection {

    // Read from environment variables; fall back to local XAMPP defaults
    private static final String DB_URL  = getEnvOrDefault("MM_DB_URL",  "jdbc:mysql://localhost:3306/mindmatrix?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
    private static final String DB_USER = getEnvOrDefault("MM_DB_USER", "root");
    private static final String DB_PASS = getEnvOrDefault("MM_DB_PASS", "");

    private static final int MAX_POOL_SIZE = 15;
    private static final BlockingQueue<Connection> pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);

    private static volatile boolean migrated = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    private static String getEnvOrDefault(String key, String defaultVal) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : defaultVal;
    }

    /**
     * One-time database schema migration executed on server boot by AppInitializer.
     */
    public static synchronized void initDatabase() {
        if (migrated) return;
        try (Connection conn = createPhysicalConnection()) {
            runMigration(conn);
            migrated = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection createPhysicalConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private static synchronized void runMigration(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 1. Ensure last_played_date exists in user_progress
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "user_progress", "last_played_date")) {
                if (!rs.next()) {
                    stmt.executeUpdate("ALTER TABLE user_progress ADD COLUMN last_played_date DATE DEFAULT NULL");
                }
            }

            // 2. Ensure avatar_id exists in users table (default 1)
            try (ResultSet rs = meta.getColumns(null, null, "users", "avatar_id")) {
                if (!rs.next()) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN avatar_id INT NOT NULL DEFAULT 0");
                }
            }

            // 3. Ensure user_activity_dates table exists
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS user_activity_dates (" +
                "  user_id INT NOT NULL," +
                "  activity_date DATE NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  PRIMARY KEY (user_id, activity_date)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );

            // 4. Clean up any invalid future activity records beyond today
            stmt.executeUpdate("DELETE FROM user_activity_dates WHERE activity_date > CURRENT_DATE()");
            stmt.executeUpdate("UPDATE user_progress SET last_played_date = CURRENT_DATE() WHERE last_played_date > CURRENT_DATE()");

            // 5. Recalculate streak for active users based on genuine activity dates
            String findUsersSql = "SELECT user_id FROM user_progress WHERE games_played > 0 OR unlocked_level > 1";
            try (Statement uStmt = conn.createStatement();
                 ResultSet uRs = uStmt.executeQuery(findUsersSql)) {
                while (uRs.next()) {
                    int uid = uRs.getInt("user_id");
                    // Recalculate streak dynamically using StreakDAO based on actual activity dates
                    StreakDAO.StreakInfo sInfo = StreakDAO.computeStreak(conn, uid);
                    String upSql = "UPDATE user_progress SET streak = ?, best_streak = GREATEST(COALESCE(best_streak, 0), ?) WHERE user_id = ?";
                    try (PreparedStatement pUp = conn.prepareStatement(upSql)) {
                        pUp.setInt(1, sInfo.currentStreak);
                        pUp.setInt(2, sInfo.longestStreak);
                        pUp.setInt(3, uid);
                        pUp.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a pooled Connection to the mindmatrix MySQL database.
     */
    public static Connection getConnection() throws SQLException {
        if (!migrated) {
            initDatabase();
        }

        Connection physicalConn = pool.poll();
        if (physicalConn != null) {
            try {
                if (!physicalConn.isClosed() && physicalConn.isValid(1)) {
                    return wrapConnection(physicalConn);
                } else {
                    try { physicalConn.close(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                try { physicalConn.close(); } catch (Exception ignored) {}
            }
        }

        return wrapConnection(createPhysicalConnection());
    }

    /**
     * Wraps physical connection so that conn.close() returns it to the pool queue.
     */
    private static Connection wrapConnection(final Connection physicalConn) {
        return (Connection) Proxy.newProxyInstance(
            DBConnection.class.getClassLoader(),
            new Class<?>[]{ Connection.class },
            new InvocationHandler() {
                private boolean closed = false;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String name = method.getName();
                    if ("close".equals(name)) {
                        if (!closed) {
                            closed = true;
                            if (!physicalConn.isClosed() && pool.offer(physicalConn)) {
                                return null;
                            }
                            physicalConn.close();
                        }
                        return null;
                    }
                    if ("isClosed".equals(name)) {
                        return closed || physicalConn.isClosed();
                    }
                    if (closed) {
                        throw new SQLException("Connection is closed.");
                    }
                    try {
                        return method.invoke(physicalConn, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            }
        );
    }
}
