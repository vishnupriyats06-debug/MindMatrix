package com.mindmatrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Level12Servlet – Symbol Memory
 *   GET  /level12?part=1  → Random symbol sequence for Part 1 (Symbol Sequence Memory).
 *   GET  /level12?part=2  → Shuffled symbol pairs for Part 2 (Symbol Matching card game).
 *   POST /level12         → Called on completion; saves progress on Part 2 completion.
 */
@WebServlet("/level12")
public class Level12Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    // Symbol pool
    private static final String[] SYMBOLS = {
        "@", "#", "$", "%", "&", "*", "+", "=", "?", "!", "^", "~",
        ">", "<", "/", "\\", "|", "-", "_", ";"
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        String partStr = req.getParameter("part");
        int part = (partStr == null) ? 1 : Integer.parseInt(partStr);
        PrintWriter out = resp.getWriter();

        if (part == 1) {
            // Part 1: 5–7 unique symbols in a random sequence
            int count = 5 + RAND.nextInt(3); // 5, 6, or 7
            List<Integer> pool = new ArrayList<>();
            for (int i = 0; i < SYMBOLS.length; i++) pool.add(i);
            Collections.shuffle(pool, RAND);

            StringBuilder sb = new StringBuilder("{\"part\":1,\"sequence\":[");
            for (int i = 0; i < count; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(SYMBOLS[pool.get(i)])).append("\"");
            }
            sb.append("]}");

            session.setAttribute("level12p1Sequence", sb.toString());
            out.print(sb.toString());

        } else if (part == 2) {
            // Part 2: 6 pairs (12 cards) shuffled
            int pairs = 6;
            List<Integer> pool = new ArrayList<>();
            for (int i = 0; i < SYMBOLS.length; i++) pool.add(i);
            Collections.shuffle(pool, RAND);

            // Build cards list: each pair appears twice
            List<String> cards = new ArrayList<>();
            for (int i = 0; i < pairs; i++) {
                String sym = SYMBOLS[pool.get(i)];
                cards.add(sym);
                cards.add(sym);
            }
            Collections.shuffle(cards, RAND);

            StringBuilder sb = new StringBuilder("{\"part\":2,\"cards\":[");
            for (int i = 0; i < cards.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(cards.get(i))).append("\"");
            }
            sb.append("],\"pairs\":").append(pairs).append("}");

            session.setAttribute("level12p2Cards", sb.toString());
            out.print(sb.toString());

        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid part\"}");
        }
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().print("{\"error\":\"Session expired\"}");
            return;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        String body = sb.toString();
        int part = extractInt(body, "part");
        boolean isCorrect = body.contains("\"correct\":true");

        // updateUserProgress handled via SaveProgressServlet
        resp.getWriter().print("{\"success\":true}");
        resp.getWriter().flush();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private int extractInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return -1;
        int start = idx + pattern.length();
        int end = json.indexOf(',', start);
        if (end == -1) end = json.indexOf('}', start);
        if (end == -1) return -1;
        try { return Integer.parseInt(json.substring(start, end).trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private void updateUserProgress(int userId, int scoreAdd, int currentLevel) {
        try (Connection conn = DBConnection.getConnection()) {
            int currentScore = 0, currentStreak = 0, unlockedLevel = 1, gamesPlayed = 0, bestStreak = 0;
            String bestScoresStr = "[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]";
            String bestTimesStr  = "[\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\"]";
            String starsStr      = "[\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\"]";
            java.sql.Date dbLastDate = null;
            try (PreparedStatement sel = conn.prepareStatement("SELECT * FROM user_progress WHERE user_id = ?")) {
                sel.setInt(1, userId);
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        currentScore  = rs.getInt("score");
                        currentStreak = rs.getInt("streak");
                        unlockedLevel = rs.getInt("unlocked_level");
                        gamesPlayed   = rs.getInt("games_played");
                        bestStreak    = rs.getInt("best_streak");
                        if (rs.getString("best_scores") != null) bestScoresStr = rs.getString("best_scores");
                        if (rs.getString("best_times")  != null) bestTimesStr  = rs.getString("best_times");
                        if (rs.getString("stars")       != null) starsStr      = rs.getString("stars");
                        dbLastDate    = rs.getDate("last_played_date");
                    }
                }
            }
            int streak           = StreakUtil.calculateDayStreak(currentStreak, dbLastDate, true);
            int newBestStreak    = Math.max(bestStreak, streak);
            int newScore         = currentScore + scoreAdd;
            int newUnlockedLevel = Math.max(unlockedLevel, currentLevel + 1);
            int newGamesPlayed   = gamesPlayed + 1;
            java.sql.Date now    = java.sql.Date.valueOf(java.time.LocalDate.now());
            String sql = "INSERT INTO user_progress (user_id,score,streak,unlocked_level,games_played,best_streak,best_scores,best_times,stars,last_played_date) " +
                         "VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE score=?,streak=?,unlocked_level=?,games_played=?,best_streak=?,best_scores=?,best_times=?,stars=?,last_played_date=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1,userId); stmt.setInt(2,newScore); stmt.setInt(3,streak);
                stmt.setInt(4,newUnlockedLevel); stmt.setInt(5,newGamesPlayed); stmt.setInt(6,newBestStreak);
                stmt.setString(7,bestScoresStr); stmt.setString(8,bestTimesStr);
                stmt.setString(9,starsStr); stmt.setDate(10,now);
                stmt.setInt(11,newScore); stmt.setInt(12,streak); stmt.setInt(13,newUnlockedLevel);
                stmt.setInt(14,newGamesPlayed); stmt.setInt(15,newBestStreak);
                stmt.setString(16,bestScoresStr); stmt.setString(17,bestTimesStr);
                stmt.setString(18,starsStr); stmt.setDate(19,now);
                stmt.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
