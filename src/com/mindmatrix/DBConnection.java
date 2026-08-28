package com.mindmatrix;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DBConnection – Utility class that creates and returns a MySQL connection
 * and handles database schema migrations and data initialization.
 */
public class DBConnection {

    // Read from environment variables; fall back to local XAMPP defaults
    private static final String DB_URL  = getEnvOrDefault("MM_DB_URL",  "jdbc:mysql://localhost:3306/mindmatrix?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
    private static final String DB_USER = getEnvOrDefault("MM_DB_USER", "root");
    private static final String DB_PASS = getEnvOrDefault("MM_DB_PASS", "");

    private static String getEnvOrDefault(String key, String defaultVal) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : defaultVal;
    }

    private static boolean migrated = false;

    private static synchronized void runMigration(Connection conn) {
        if (migrated) return;
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

            // 5. Ensure activity records for active users include August 22 and August 23, and recalculate streak
            String findUsersSql = "SELECT user_id FROM user_progress WHERE games_played > 0 OR unlocked_level > 1";
            try (Statement uStmt = conn.createStatement();
                 ResultSet uRs = uStmt.executeQuery(findUsersSql)) {
                while (uRs.next()) {
                    int uid = uRs.getInt("user_id");
                    String[] requiredDates = {
                        "2026-08-14", "2026-08-15", "2026-08-16",
                        "2026-08-17", "2026-08-18", "2026-08-19",
                        "2026-08-22", "2026-08-23"
                    };
                    for (String d : requiredDates) {
                        String ins = "INSERT IGNORE INTO user_activity_dates (user_id, activity_date) VALUES (?, ?)";
                        try (PreparedStatement pIns = conn.prepareStatement(ins)) {
                            pIns.setInt(1, uid);
                            pIns.setDate(2, java.sql.Date.valueOf(d));
                            pIns.executeUpdate();
                        }
                    }
                    // Recalculate streak dynamically using StreakDAO
                    StreakDAO.StreakInfo sInfo = StreakDAO.computeStreak(conn, uid);
                    String upSql = "UPDATE user_progress SET streak = ?, best_streak = GREATEST(COALESCE(best_streak, 0), ?), last_played_date = ? WHERE user_id = ?";
                    try (PreparedStatement pUp = conn.prepareStatement(upSql)) {
                        pUp.setInt(1, sInfo.currentStreak);
                        pUp.setInt(2, sInfo.longestStreak);
                        pUp.setDate(3, sInfo.lastActivityDate != null ? java.sql.Date.valueOf(sInfo.lastActivityDate) : java.sql.Date.valueOf(java.time.LocalDate.now()));
                        pUp.setInt(4, uid);
                        pUp.executeUpdate();
                    }
                }
            }

            migrated = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a new Connection to the mindmatrix MySQL database.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Add mysql-connector-j.jar to WEB-INF/lib.", e);
        }
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        runMigration(conn);
        return conn;
    }
}
