package com.mindmatrix;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * SaveProgressServlet – Handles updating or inserting user progress and persistent activity dates in MySQL.
 * POST /saveProgress
 */
@WebServlet("/saveProgress")
public class SaveProgressServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized access. Please login.");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        try {
            int clientScore = Integer.parseInt(request.getParameter("score"));
            int clientUnlockedLevel = Integer.parseInt(request.getParameter("unlockedLevel"));
            int gamesPlayed = Integer.parseInt(request.getParameter("gamesPlayed"));
            int clientBestStreak = request.getParameter("bestStreak") != null ? Integer.parseInt(request.getParameter("bestStreak")) : 0;
            String bestScores = request.getParameter("bestScores");
            String bestTimes = request.getParameter("bestTimes");
            String stars = request.getParameter("stars");
            String hintsParam = request.getParameter("hints");
            String clientDateParam = request.getParameter("lastPlayedDate");
            if (clientDateParam == null || clientDateParam.trim().isEmpty()) {
                clientDateParam = request.getParameter("clientDate");
            }

            LocalDate activityDate = null;
            if (clientDateParam != null && clientDateParam.trim().length() >= 10) {
                try {
                    activityDate = LocalDate.parse(clientDateParam.trim().substring(0, 10));
                } catch (Exception e) {
                    activityDate = LocalDate.now();
                }
            } else {
                activityDate = LocalDate.now();
            }

            try (Connection conn = DBConnection.getConnection()) {
                // 1. Record user's local activity date persistently in user_activity_dates
                StreakDAO.StreakInfo streakInfo = StreakDAO.recordLevelCompletion(conn, userId, activityDate);

                int dbScore = 0;
                int dbUnlockedLevel = 1;
                int dbHints = 3;

                boolean exists = false;
                String selSql = "SELECT score, unlocked_level, hints FROM user_progress WHERE user_id = ?";
                try (PreparedStatement selStmt = conn.prepareStatement(selSql)) {
                    selStmt.setInt(1, userId);
                    try (ResultSet rs = selStmt.executeQuery()) {
                        if (rs.next()) {
                            exists = true;
                            dbScore = rs.getInt("score");
                            dbUnlockedLevel = rs.getInt("unlocked_level");
                            int h = rs.getInt("hints");
                            if (!rs.wasNull()) dbHints = h;
                        }
                    }
                }

                int unlockedLevel = Math.max(dbUnlockedLevel, clientUnlockedLevel);
                int score = Math.max(dbScore, clientScore);
                int bestStreak = Math.max(streakInfo.longestStreak, clientBestStreak);
                int hints = (hintsParam != null) ? Math.max(0, Integer.parseInt(hintsParam)) : dbHints;
                Date lastPlayedDate = Date.valueOf(activityDate);

                if (exists) {
                    String sql = "UPDATE user_progress SET score = ?, streak = ?, unlocked_level = ?, games_played = ?, best_streak = ?, best_scores = ?, best_times = ?, stars = ?, last_played_date = ?, hints = ? WHERE user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, score);
                        stmt.setInt(2, streakInfo.currentStreak);
                        stmt.setInt(3, unlockedLevel);
                        stmt.setInt(4, gamesPlayed);
                        stmt.setInt(5, bestStreak);
                        stmt.setString(6, bestScores);
                        stmt.setString(7, bestTimes);
                        stmt.setString(8, stars);
                        stmt.setDate(9, lastPlayedDate);
                        stmt.setInt(10, hints);
                        stmt.setInt(11, userId);
                        stmt.executeUpdate();
                    }
                } else {
                    String sql = "INSERT INTO user_progress (user_id, score, streak, unlocked_level, games_played, best_streak, best_scores, best_times, stars, last_played_date, hints) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, userId);
                        stmt.setInt(2, score);
                        stmt.setInt(3, streakInfo.currentStreak);
                        stmt.setInt(4, unlockedLevel);
                        stmt.setInt(5, gamesPlayed);
                        stmt.setInt(6, bestStreak);
                        stmt.setString(7, bestScores);
                        stmt.setString(8, bestTimes);
                        stmt.setString(9, stars);
                        stmt.setDate(10, lastPlayedDate);
                        stmt.setInt(11, hints);
                        stmt.executeUpdate();
                    }
                }
            }
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric format.");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
