package com.mindmatrix;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * StreakService – Business logic for persistent streak & activity history management.
 */
public class StreakService {

    /**
     * Updates the user's streak and activity dates upon level completion.
     */
    public static StreakDAO.StreakInfo handleLevelCompleted(int userId, LocalDate activityDate) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return StreakDAO.recordLevelCompletion(conn, userId, activityDate);
        }
    }

    public static StreakDAO.StreakInfo handleLevelCompleted(int userId) throws SQLException {
        return handleLevelCompleted(userId, null);
    }

    /**
     * Gets the user's computed streak and activity dates history without modifying it.
     */
    public static StreakDAO.StreakInfo getUserStreak(int userId, LocalDate activityDate) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return StreakDAO.computeStreak(conn, userId, activityDate);
        }
    }

    public static StreakDAO.StreakInfo getUserStreak(int userId) throws SQLException {
        return getUserStreak(userId, null);
    }
}
