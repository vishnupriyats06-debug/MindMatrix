package com.mindmatrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Level5Servlet – Handles Level 5: Color Detection & Sequence Memory.
 *   GET  /level5?part=1  → returns JSON with round data for Odd Color Detection.
 *   GET  /level5?part=2  → returns JSON with a 10-color sequence for Sequence Memory.
 *   POST /level5         → validates the user's answer for either part.
 *                          On Part 2 success, saves progress and unlocks Level 6.
 */
@WebServlet("/level5")
public class Level5Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    // 5 rounds of odd-color detection, delta decreases each round (lightness shift %)
    private static final int[] ROUND_DELTAS = {15, 12, 9, 6, 4};

    // The 6 color pool for Part 2 sequences (names match frontend CSS colors)
    private static final String[] COLOR_NAMES = {"red", "blue", "green", "yellow", "purple", "orange"};

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession();
        String partParam = req.getParameter("part");
        int part = (partParam == null) ? 1 : Integer.parseInt(partParam);
        PrintWriter out = resp.getWriter();

        if (part == 1) {
            // Generate 5 rounds of odd-color data
            // For each round: hue (0-359), saturation (60-90%), base lightness (40-55%), oddIndex (0-24)
            int[][] rounds = new int[5][4];
            for (int r = 0; r < 5; r++) {
                rounds[r][0] = RAND.nextInt(360);                // hue
                rounds[r][1] = 60 + RAND.nextInt(31);            // saturation 60-90
                rounds[r][2] = 40 + RAND.nextInt(16);            // base lightness 40-55
                rounds[r][3] = RAND.nextInt(25);                 // oddIndex in 5x5 grid (0-24)
            }
            session.setAttribute("p1Rounds", rounds);
            out.print(roundsToJson(rounds));

        } else if (part == 2) {
            // Generate a random sequence of 10 color indices (0-5)
            int[] sequence = new int[10];
            for (int i = 0; i < 10; i++) {
                sequence[i] = RAND.nextInt(6);
            }
            session.setAttribute("p2Sequence", sequence);
            out.print(sequenceToJson(sequence));

        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid part parameter\"}");
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

        // Read body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        String body = sb.toString();

        int part = extractInt(body, "part");
        boolean correct = false;

        if (part == 1) {
            // Validate: user submits {"part":1,"round":R,"answer":N}
            int round  = extractInt(body, "round");   // 0-based round index
            int answer = extractInt(body, "answer");  // user's chosen cell index (0-24)

            int[][] rounds = (int[][]) session.getAttribute("p1Rounds");
            if (rounds != null && round >= 0 && round < rounds.length) {
                correct = (answer == rounds[round][3]);
            }

        } else if (part == 2) {
            // Validate: user submits {"part":2,"sequence":[0,1,2,...]}
            int[] sessionSeq = (int[]) session.getAttribute("p2Sequence");
            int[] userSeq    = extractIntArray(body, "sequence");

            if (sessionSeq != null && userSeq != null && sessionSeq.length == userSeq.length) {
                correct = true;
                for (int i = 0; i < sessionSeq.length; i++) {
                    if (sessionSeq[i] != userSeq[i]) { correct = false; break; }
                }
            }

            // On success, save progress and unlock level 6
            // updateUserProgress handled via SaveProgressServlet
        }

        resp.getWriter().print("{\"correct\":" + correct + "}");
        resp.getWriter().flush();
    }

    // ─── Progress Update ──────────────────────────────────────────────────────
    private void updateUserProgress(int userId, int scoreAdd, int currentLevel) {
        try (Connection conn = DBConnection.getConnection()) {
            int currentScore   = 0, currentStreak = 0, unlockedLevel = 1;
            int gamesPlayed    = 0, bestStreak = 0;
            String bestScoresStr = "[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]";
            String bestTimesStr  = "[\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\"," +
                                   "\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\"]";
            String starsStr      = "[\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\"," +
                                   "\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\"]";
            java.sql.Date dbLastDate = null;

            String selSql = "SELECT * FROM user_progress WHERE user_id = ?";
            try (PreparedStatement selStmt = conn.prepareStatement(selSql)) {
                selStmt.setInt(1, userId);
                try (ResultSet rs = selStmt.executeQuery()) {
                    if (rs.next()) {
                        currentScore  = rs.getInt("score");
                        currentStreak = rs.getInt("streak");
                        unlockedLevel = rs.getInt("unlocked_level");
                        gamesPlayed   = rs.getInt("games_played");
                        bestStreak    = rs.getInt("best_streak");
                        if (rs.getString("best_scores") != null) bestScoresStr = rs.getString("best_scores");
                        if (rs.getString("best_times")  != null) bestTimesStr  = rs.getString("best_times");
                        if (rs.getString("stars")       != null) starsStr      = rs.getString("stars");
                        dbLastDate = rs.getDate("last_played_date");
                    }
                }
            }

            int streak          = StreakUtil.calculateDayStreak(currentStreak, dbLastDate, true);
            int newBestStreak   = Math.max(bestStreak, streak);
            int newScore        = currentScore + scoreAdd;
            int newUnlocked     = Math.max(unlockedLevel, currentLevel + 1);
            int newGamesPlayed  = gamesPlayed + 1;
            java.sql.Date now   = java.sql.Date.valueOf(java.time.LocalDate.now());

            // Update best_scores[4] (index 4 = Level 5)
            bestScoresStr = updateJsonArrayIndex(bestScoresStr, 4, scoreAdd);
            // Update stars[4]
            starsStr      = updateJsonArrayIndexStr(starsStr, 4, "***");
            // Update best_times[4]
            bestTimesStr  = updateJsonArrayIndexStr(bestTimesStr, 4, "60s");

            String sql = "INSERT INTO user_progress " +
                "(user_id, score, streak, unlocked_level, games_played, best_streak, best_scores, best_times, stars, last_played_date) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE score=?, streak=?, unlocked_level=?, games_played=?, best_streak=?, best_scores=?, best_times=?, stars=?, last_played_date=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);       stmt.setInt(2, newScore);
                stmt.setInt(3, streak);       stmt.setInt(4, newUnlocked);
                stmt.setInt(5, newGamesPlayed); stmt.setInt(6, newBestStreak);
                stmt.setString(7, bestScoresStr); stmt.setString(8, bestTimesStr);
                stmt.setString(9, starsStr);  stmt.setDate(10, now);

                stmt.setInt(11, newScore);    stmt.setInt(12, streak);
                stmt.setInt(13, newUnlocked); stmt.setInt(14, newGamesPlayed);
                stmt.setInt(15, newBestStreak);
                stmt.setString(16, bestScoresStr); stmt.setString(17, bestTimesStr);
                stmt.setString(18, starsStr); stmt.setDate(19, now);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── JSON Builders ────────────────────────────────────────────────────────
    private String roundsToJson(int[][] rounds) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"rounds\":[");
        for (int r = 0; r < rounds.length; r++) {
            if (r > 0) sb.append(',');
            sb.append("{\"hue\":").append(rounds[r][0])
              .append(",\"sat\":").append(rounds[r][1])
              .append(",\"lit\":").append(rounds[r][2])
              .append(",\"odd\":").append(rounds[r][3])
              .append(",\"delta\":").append(ROUND_DELTAS[r])
              .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private String sequenceToJson(int[] seq) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"sequence\":[");
        for (int i = 0; i < seq.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(seq[i]);
        }
        sb.append("]}");
        return sb.toString();
    }

    // ─── JSON Array Helpers ───────────────────────────────────────────────────
    private String updateJsonArrayIndex(String json, int index, int value) {
        try {
            String[] parts = json.replaceAll("[\\[\\]]", "").split(",");
            if (index < parts.length) {
                int existing = 0;
                try { existing = Integer.parseInt(parts[index].trim()); } catch (Exception ignored) {}
                if (value > existing) parts[index] = String.valueOf(value);
            }
            return "[" + String.join(",", parts) + "]";
        } catch (Exception e) { return json; }
    }

    private String updateJsonArrayIndexStr(String json, int index, String value) {
        try {
            String inner = json.trim();
            if (inner.startsWith("[")) inner = inner.substring(1);
            if (inner.endsWith("]"))   inner = inner.substring(0, inner.length() - 1);
            String[] parts = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            if (index < parts.length) parts[index] = "\"" + value + "\"";
            return "[" + String.join(",", parts) + "]";
        } catch (Exception e) { return json; }
    }

    // ─── JSON Extraction Helpers ──────────────────────────────────────────────
    private int extractInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return -1;
        int start = idx + pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end).trim()); }
        catch (Exception e) { return -1; }
    }

    private int[] extractIntArray(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int start = idx + pattern.length();
        int end = json.indexOf(']', start);
        if (end == -1) return null;
        String inner = json.substring(start, end).trim();
        if (inner.isEmpty()) return new int[0];
        String[] tokens = inner.split(",");
        List<Integer> list = new ArrayList<>();
        for (String t : tokens) {
            try { list.add(Integer.parseInt(t.trim())); } catch (Exception ignored) {}
        }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
}
