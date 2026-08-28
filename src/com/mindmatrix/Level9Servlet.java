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
 * Level9Servlet – Emoji Memory & Detection
 *   GET  /level9?part=1  → Returns 10 random emojis for Part 1.
 *   GET  /level9?part=2  → Returns subtle visual emoji pairing + odd emoji position for Part 2.
 *   POST /level9         → Validates user response.
 */
@WebServlet("/level9")
public class Level9Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    // Pool of emojis for Part 1 Sequence Memory
    private static final String[] EMOJI_POOL = {
        "😀", "😎", "🐶", "🐱", "🍎", "🚗", "⭐", "🌈", "⚽", "🎵",
        "🎁", "🔥", "⚡", "🍀", "💡", "🍕", "🚀", "🛸", "🎈", "💎"
    };

    // visually similar emoji pairings for Part 2
    private static final String[][] SIMILAR_PAIRS = {
        {"😀", "😃"},
        {"😐", "😑"},
        {"😮", "😯"},
        {"😭", "😢"},
        {"😁", "😆"},
        {"😍", "🥰"},
        {"😎", "🤓"},
        {"❤️", "🩷"},
        {"😜", "🤪"},
        {"😔", "😞"},
        {"🤔", "🤨"}
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        int part = req.getParameter("part") == null ? 1 : Integer.parseInt(req.getParameter("part"));
        PrintWriter out = resp.getWriter();

        if (part == 1) {
            // Select 10 random distinct emojis
            List<String> pool = new ArrayList<>();
            for (String emoji : EMOJI_POOL) {
                pool.add(emoji);
            }
            Collections.shuffle(pool, RAND);
            List<String> sequence = pool.subList(0, 10);
            
            session.setAttribute("level9p1Sequence", sequence);
            out.print("{\"sequence\":" + listToJson(sequence) + "}");
        } else if (part == 2) {
            // Select 5 distinct visual emoji pairs for 5 sub-rounds
            List<Integer> pairIndices = new ArrayList<>();
            for (int i = 0; i < SIMILAR_PAIRS.length; i++) pairIndices.add(i);
            Collections.shuffle(pairIndices, RAND);

            StringBuilder gridsJson = new StringBuilder();
            gridsJson.append("[");

            List<String> sessionGrids = new ArrayList<>();

            for (int r = 0; r < 5; r++) {
                int pairIdx = pairIndices.get(r % pairIndices.size());
                String[] pair = SIMILAR_PAIRS[pairIdx];
                String normal = pair[0];
                String odd = pair[1];
                if (RAND.nextBoolean()) {
                    normal = pair[1];
                    odd = pair[0];
                }
                int oddIndex = RAND.nextInt(100); // 10x10 grid has 100 cells

                if (r > 0) gridsJson.append(",");
                gridsJson.append("{")
                         .append("\"normal\":\"").append(escapeJson(normal)).append("\",")
                         .append("\"odd\":\"").append(escapeJson(odd)).append("\",")
                         .append("\"oddIndex\":").append(oddIndex)
                         .append("}");

                sessionGrids.add(normal + ":" + odd + ":" + oddIndex);
            }
            gridsJson.append("]");

            session.setAttribute("level9p2Grids", sessionGrids);
            out.print("{\"grids\":" + gridsJson.toString() + "}");
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid part\"}");
        }
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
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
        boolean valid = false;

        if (part == 1 && session.getAttribute("level9p1Sequence") != null && isCorrect) {
            valid = true;
        } else if (part == 2 && (session.getAttribute("level9p2Grids") != null || session.getAttribute("level9p2OddIndex") != null) && isCorrect) {
            valid = true;
            // updateUserProgress handled via SaveProgressServlet
        }
        resp.getWriter().print("{\"success\":" + valid + "}");
        resp.getWriter().flush();
    }

    private String listToJson(List<String> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
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
        return Integer.parseInt(json.substring(start, end).trim());
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
