package com.mindmatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * SaveProgressServlet – Handles updating or inserting user progress and persistent activity dates in MySQL.
 * Performs server-side atomic merging of DB and Client progress so levels completed on one device
 * are NEVER overwritten or lost when logging in from another device.
 * POST /saveProgress
 */
@WebServlet("/saveProgress")
public class SaveProgressServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized access. Please login.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        try {
            int clientScore = request.getParameter("score") != null ? Integer.parseInt(request.getParameter("score")) : 0;
            int clientUnlockedLevel = request.getParameter("unlockedLevel") != null ? Integer.parseInt(request.getParameter("unlockedLevel")) : 1;
            int clientGamesPlayed = request.getParameter("gamesPlayed") != null ? Integer.parseInt(request.getParameter("gamesPlayed")) : 0;
            int clientBestStreak = request.getParameter("bestStreak") != null ? Integer.parseInt(request.getParameter("bestStreak")) : 0;
            
            String clientBestScoresParam = request.getParameter("bestScores");
            String clientBestTimesParam = request.getParameter("bestTimes");
            String clientStarsParam = request.getParameter("stars");
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

            int finalScore = clientScore;
            int finalUnlockedLevel = clientUnlockedLevel;
            int finalGamesPlayed = clientGamesPlayed;
            int finalBestStreak = clientBestStreak;
            int finalStreak = 0;
            int finalHints = 3;
            int[] mergedScores = new int[20];
            String[] mergedTimes = new String[20];
            Arrays.fill(mergedTimes, "-");
            String[] mergedStars = new String[20];
            Arrays.fill(mergedStars, "0");

            int[] clientScores = parseJsonIntArray(clientBestScoresParam, 20);
            String[] clientTimes = parseJsonStringArray(clientBestTimesParam, 20, "-");
            String[] clientStars = parseJsonStringArray(clientStarsParam, 20, "0");

            try (Connection conn = DBConnection.getConnection()) {
                // 1. Record user's local activity date persistently in user_activity_dates
                StreakDAO.StreakInfo streakInfo = StreakDAO.recordLevelCompletion(conn, userId, activityDate);
                finalStreak = streakInfo.currentStreak;

                int dbScore = 0;
                int dbUnlockedLevel = 1;
                int dbGamesPlayed = 0;
                int dbBestStreak = 0;
                int dbHints = 3;
                int[] dbScores = new int[20];
                String[] dbTimes = new String[20];
                Arrays.fill(dbTimes, "-");
                String[] dbStars = new String[20];
                Arrays.fill(dbStars, "0");

                boolean exists = false;
                String selSql = "SELECT score, unlocked_level, games_played, best_streak, best_scores, best_times, stars, hints FROM user_progress WHERE user_id = ?";
                try (PreparedStatement selStmt = conn.prepareStatement(selSql)) {
                    selStmt.setInt(1, userId);
                    try (ResultSet rs = selStmt.executeQuery()) {
                        if (rs.next()) {
                            exists = true;
                            dbScore = rs.getInt("score");
                            dbUnlockedLevel = rs.getInt("unlocked_level");
                            dbGamesPlayed = rs.getInt("games_played");
                            dbBestStreak = rs.getInt("best_streak");
                            int h = rs.getInt("hints");
                            if (!rs.wasNull()) dbHints = h;
                            
                            dbScores = parseJsonIntArray(rs.getString("best_scores"), 20);
                            dbTimes = parseJsonStringArray(rs.getString("best_times"), 20, "-");
                            dbStars = parseJsonStringArray(rs.getString("stars"), 20, "0");
                        }
                    }
                }

                // 2. Perform Atomic Multi-Device Merge
                finalScore = Math.max(dbScore, clientScore);
                finalGamesPlayed = Math.max(dbGamesPlayed, clientGamesPlayed);
                finalBestStreak = Math.max(dbBestStreak, Math.max(streakInfo.longestStreak, clientBestStreak));
                finalUnlockedLevel = Math.max(dbUnlockedLevel, clientUnlockedLevel);
                finalHints = (hintsParam != null) ? Math.max(0, Integer.parseInt(hintsParam)) : dbHints;

                for (int i = 0; i < 20; i++) {
                    mergedScores[i] = Math.max(dbScores[i], clientScores[i]);
                    mergedStars[i] = maxStarValue(dbStars[i], clientStars[i]);
                    mergedTimes[i] = pickBestTime(dbTimes[i], clientTimes[i]);
                    
                    // Auto-heal unlockedLevel based on completed levels
                    if (mergedScores[i] > 0 || isStarCompleted(mergedStars[i])) {
                        if (finalUnlockedLevel < i + 2) {
                            finalUnlockedLevel = Math.min(20, i + 2);
                        }
                    }
                }

                Date lastPlayedDate = Date.valueOf(activityDate);
                String bestScoresJson = toJsonIntArray(mergedScores);
                String bestTimesJson = toJsonStringArray(mergedTimes);
                String starsJson = toJsonStringArray(mergedStars);

                if (exists) {
                    String sql = "UPDATE user_progress SET score = ?, streak = ?, unlocked_level = ?, games_played = ?, best_streak = ?, best_scores = ?, best_times = ?, stars = ?, last_played_date = ?, hints = ? WHERE user_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, finalScore);
                        stmt.setInt(2, finalStreak);
                        stmt.setInt(3, finalUnlockedLevel);
                        stmt.setInt(4, finalGamesPlayed);
                        stmt.setInt(5, finalBestStreak);
                        stmt.setString(6, bestScoresJson);
                        stmt.setString(7, bestTimesJson);
                        stmt.setString(8, starsJson);
                        stmt.setDate(9, lastPlayedDate);
                        stmt.setInt(10, finalHints);
                        stmt.setInt(11, userId);
                        stmt.executeUpdate();
                    }
                } else {
                    String sql = "INSERT INTO user_progress (user_id, score, streak, unlocked_level, games_played, best_streak, best_scores, best_times, stars, last_played_date, hints) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, userId);
                        stmt.setInt(2, finalScore);
                        stmt.setInt(3, finalStreak);
                        stmt.setInt(4, finalUnlockedLevel);
                        stmt.setInt(5, finalGamesPlayed);
                        stmt.setInt(6, finalBestStreak);
                        stmt.setString(7, bestScoresJson);
                        stmt.setString(8, bestTimesJson);
                        stmt.setString(9, starsJson);
                        stmt.setDate(10, lastPlayedDate);
                        stmt.setInt(11, finalHints);
                        stmt.executeUpdate();
                    }
                }
            }
            
            String jsonResponse = String.format(
                "{\"status\":\"success\",\"score\":%d,\"unlockedLevel\":%d,\"gamesPlayed\":%d,\"streak\":%d,\"bestStreak\":%d,\"bestScores\":%s,\"bestTimes\":%s,\"stars\":%s}",
                finalScore, finalUnlockedLevel, finalGamesPlayed, finalStreak, finalBestStreak,
                toJsonIntArray(mergedScores), toJsonStringArray(mergedTimes), toJsonStringArray(mergedStars)
            );
            
            PrintWriter out = response.getWriter();
            out.print(jsonResponse);
            out.flush();

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid numeric format.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    // --- Helper Methods ---

    private int[] parseJsonIntArray(String json, int length) {
        int[] arr = new int[length];
        if (json == null || json.trim().isEmpty()) return arr;
        try {
            String cleaned = json.trim().replaceAll("^\\[|\\]$", "");
            if (cleaned.isEmpty()) return arr;
            String[] parts = cleaned.split(",");
            for (int i = 0; i < Math.min(parts.length, length); i++) {
                try {
                    arr[i] = Integer.parseInt(parts[i].trim().replaceAll("^\"|\"$", ""));
                } catch (Exception e) {
                    arr[i] = 0;
                }
            }
        } catch (Exception e) {}
        return arr;
    }

    private String[] parseJsonStringArray(String json, int length, String defaultVal) {
        String[] arr = new String[length];
        Arrays.fill(arr, defaultVal);
        if (json == null || json.trim().isEmpty()) return arr;
        try {
            String cleaned = json.trim().replaceAll("^\\[|\\]$", "");
            if (cleaned.isEmpty()) return arr;
            String[] parts = cleaned.split(",");
            for (int i = 0; i < Math.min(parts.length, length); i++) {
                String val = parts[i].trim().replaceAll("^\"|\"$", "");
                arr[i] = val.isEmpty() ? defaultVal : val;
            }
        } catch (Exception e) {}
        return arr;
    }

    private String maxStarValue(String s1, String s2) {
        int v1 = getStarRank(s1);
        int v2 = getStarRank(s2);
        return (v1 >= v2) ? (s1 == null || s1.equals("0") ? "0" : s1) : s2;
    }

    private int getStarRank(String s) {
        if (s == null) return 0;
        if (s.equals("3") || s.equals("***") || s.contains("★")) return 3;
        if (s.equals("2") || s.length() == 2) return 2;
        if (s.equals("1") || s.length() == 1 && !s.equals("0")) return 1;
        return 0;
    }

    private boolean isStarCompleted(String s) {
        return getStarRank(s) > 0;
    }

    private String pickBestTime(String t1, String t2) {
        if (t1 == null || t1.equals("-") || t1.equals("--") || t1.isEmpty()) return (t2 != null && !t2.isEmpty()) ? t2 : "-";
        if (t2 == null || t2.equals("-") || t2.equals("--") || t2.isEmpty()) return t1;
        return t1; // keep existing non-empty time
    }

    private String toJsonIntArray(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonStringArray(String[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(arr[i]).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}

