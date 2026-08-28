package com.mindmatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * GetProgressServlet – Fetches user session, game progress, streak, and complete
 * historical activity dates from MySQL and returns it as JSON.
 * GET /getProgress
 */
@WebServlet("/getProgress")
public class GetProgressServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        // Prevent browser caching to ensure fresh stats are loaded per user
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized access. Please login.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        String email = (String) session.getAttribute("email");

        // Defaults
        int score = 0;
        int unlockedLevel = 1;
        int gamesPlayed = 0;
        int hints = 3;
        int avatarId = 0;
        String bestScores = "[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]";
        String bestTimes = "[\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\"]";
        String stars = "[\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\"]";
        String lastPlayedDate = "";
        StreakDAO.StreakInfo streakInfo = null;

        String clientDateParam = request.getParameter("lastPlayedDate");
        if (clientDateParam == null || clientDateParam.trim().isEmpty()) {
            clientDateParam = request.getParameter("clientDate");
        }
        java.time.LocalDate activityDate = null;
        if (clientDateParam != null && clientDateParam.trim().length() >= 10) {
            try {
                activityDate = java.time.LocalDate.parse(clientDateParam.trim().substring(0, 10));
            } catch (Exception e) {
                activityDate = java.time.LocalDate.now();
            }
        } else {
            activityDate = java.time.LocalDate.now();
        }

        try (Connection conn = DBConnection.getConnection()) {
            streakInfo = StreakDAO.computeStreak(conn, userId, activityDate);

            // Fetch avatar_id from users table
            String userSql = "SELECT avatar_id FROM users WHERE id = ?";
            try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                userStmt.setInt(1, userId);
                try (ResultSet uRs = userStmt.executeQuery()) {
                    if (uRs.next()) {
                        int dbAvatar = uRs.getInt("avatar_id");
                        if (!uRs.wasNull() && dbAvatar >= 1 && dbAvatar <= 20) {
                            avatarId = dbAvatar;
                        } else {
                            avatarId = 0;
                        }
                    }
                }
            }
            session.setAttribute("avatarId", avatarId);

            String sql = "SELECT * FROM user_progress WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        score = rs.getInt("score");
                        unlockedLevel = rs.getInt("unlocked_level");
                        gamesPlayed = rs.getInt("games_played");
                        int dbHints = rs.getInt("hints");
                        if (!rs.wasNull()) {
                            hints = dbHints;
                        }
                        if (rs.getString("best_scores") != null) {
                            bestScores = rs.getString("best_scores");
                        }
                        if (rs.getString("best_times") != null) {
                            bestTimes = rs.getString("best_times");
                        }
                        if (rs.getString("stars") != null) {
                            stars = rs.getString("stars");
                        }
                        Date dbLastDate = rs.getDate("last_played_date");
                        if (dbLastDate != null) {
                            lastPlayedDate = dbLastDate.toString();
                        }
                    } else {
                        // Initialize progress if it doesn't exist (failsafe)
                        String initSql = "INSERT INTO user_progress (user_id, hints) VALUES (?, 3)";
                        try (PreparedStatement initStmt = conn.prepareStatement(initSql)) {
                            initStmt.setInt(1, userId);
                            initStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        int streak = (streakInfo != null) ? streakInfo.currentStreak : 0;
        int bestStreak = (streakInfo != null) ? streakInfo.longestStreak : 0;
        List<String> activityDates = (streakInfo != null) ? streakInfo.activityDates : java.util.Collections.emptyList();

        String json = String.format(
            "{" +
            "\"username\":\"%s\"," +
            "\"email\":\"%s\"," +
            "\"score\":%d," +
            "\"streak\":%d," +
            "\"unlockedLevel\":%d," +
            "\"gamesPlayed\":%d," +
            "\"bestStreak\":%d," +
            "\"hints\":%d," +
            "\"avatarId\":%d," +
            "\"bestScores\":%s," +
            "\"bestTimes\":%s," +
            "\"stars\":%s," +
            "\"lastPlayedDate\":\"%s\"," +
            "\"activityDates\":%s" +
            "}",
            escapeJson(username),
            escapeJson(email),
            score,
            streak,
            unlockedLevel,
            gamesPlayed,
            bestStreak,
            hints,
            avatarId,
            bestScores,
            bestTimes,
            stars,
            escapeJson(lastPlayedDate),
            toJsonArray(activityDates)
        );

        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
