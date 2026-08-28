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
 * Level8Servlet – Auditory Memory
 *   GET  /level8?part=1   → returns JSON with a random sequence of instrument names.
 *   GET  /level8?part=2   → returns JSON with a randomized list of animal sound cards.
 *   POST /level8          → validates user response and updates progress.
 */
@WebServlet("/level8")
public class Level8Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();



    private static final String[] WORDS = {
        "Apple", "River", "Mountain", "Clock", "Garden", "Tiger", "Pencil", "Window", "Mirror", "Castle", "Flower", "Planet", "Ocean", "Forest", "Bridge"
    };

    private static final String[] ANIMALS = {
        "Dog", "Cat", "Lion", "Elephant", "Cow", "Horse", "Monkey", "Bird", "Sheep", "Rooster"
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        String partParam = req.getParameter("part");
        int part = partParam == null ? 1 : Integer.parseInt(partParam);
        PrintWriter out = resp.getWriter();

        if (part == 1) {
            // Generate a sequence of 5 random words for part 1
            List<String> sequence = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                String word = WORDS[RAND.nextInt(WORDS.length)];
                sequence.add(word);
            }
            session.setAttribute("level8part1Sequence", sequence);
            out.print("{\"words\":" + listToJson(sequence) + "}");
        } else if (part == 2) {
            // Jumbled Words Game (10 words)
            List<String> pool = new ArrayList<>();
            for (String w : WORDS) {
                pool.add(w);
            }
            Collections.shuffle(pool, RAND);
            List<String> selectedWords = pool.subList(0, 10);
            
            session.setAttribute("level8part2Words", selectedWords);
            out.print("{\"words\":" + listToJson(selectedWords) + "}");
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
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();
        int part = extractInt(body, "part");
        boolean isCorrect = body.contains("\"correct\":true");
        boolean valid = false;

        if (part == 1 && session.getAttribute("level8part1Sequence") != null && isCorrect) {
            valid = true;
        } else if (part == 2 && session.getAttribute("level8part2Words") != null && isCorrect) {
            valid = true;
            // updateUserProgress handled via SaveProgressServlet
        }
        
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":" + valid + "}");
        out.flush();
    }

    private String listToJson(List<String> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private int extractInt(String json, String key) {
        String pattern = "\\\"" + key + "\\\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return -1;
        int start = idx + pattern.length();
        int end = json.indexOf(',', start);
        if (end == -1) end = json.indexOf('}', start);
        return Integer.parseInt(json.substring(start, end).trim());
    }

    private void updateUserProgress(int userId, int scoreAdd, int currentLevel) {
        try (Connection conn = DBConnection.getConnection()) {
            int currentScore = 0;
            int currentStreak = 0;
            int unlockedLevel = 1;
            int gamesPlayed = 0;
            int bestStreak = 0;
            String bestScoresStr = "[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]";
            String bestTimesStr = "[\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\"]";
            String starsStr = "[\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\"]";
            java.sql.Date dbLastDate = null;

            String selSql = "SELECT * FROM user_progress WHERE user_id = ?";
            try (PreparedStatement selStmt = conn.prepareStatement(selSql)) {
                selStmt.setInt(1, userId);
                try (ResultSet rs = selStmt.executeQuery()) {
                    if (rs.next()) {
                        currentScore = rs.getInt("score");
                        currentStreak = rs.getInt("streak");
                        unlockedLevel = rs.getInt("unlocked_level");
                        gamesPlayed = rs.getInt("games_played");
                        bestStreak = rs.getInt("best_streak");
                        if (rs.getString("best_scores") != null) bestScoresStr = rs.getString("best_scores");
                        if (rs.getString("best_times") != null) bestTimesStr = rs.getString("best_times");
                        if (rs.getString("stars") != null) starsStr = rs.getString("stars");
                        dbLastDate = rs.getDate("last_played_date");
                    }
                }
            }

            int streak = StreakUtil.calculateDayStreak(currentStreak, dbLastDate, true);
            int newBestStreak = Math.max(bestStreak, streak);
            int newScore = currentScore + scoreAdd;
            int newUnlockedLevel = Math.max(unlockedLevel, currentLevel + 1);
            int newGamesPlayed = gamesPlayed + 1;
            java.sql.Date now = java.sql.Date.valueOf(java.time.LocalDate.now());

            String sql = "INSERT INTO user_progress (user_id, score, streak, unlocked_level, games_played, best_streak, best_scores, best_times, stars, last_played_date) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE score = ?, streak = ?, unlocked_level = ?, games_played = ?, best_streak = ?, best_scores = ?, best_times = ?, stars = ?, last_played_date = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, newScore);
                stmt.setInt(3, streak);
                stmt.setInt(4, newUnlockedLevel);
                stmt.setInt(5, newGamesPlayed);
                stmt.setInt(6, newBestStreak);
                stmt.setString(7, bestScoresStr);
                stmt.setString(8, bestTimesStr);
                stmt.setString(9, starsStr);
                stmt.setDate(10, now);

                stmt.setInt(11, newScore);
                stmt.setInt(12, streak);
                stmt.setInt(13, newUnlockedLevel);
                stmt.setInt(14, newGamesPlayed);
                stmt.setInt(15, newBestStreak);
                stmt.setString(16, bestScoresStr);
                stmt.setString(17, bestTimesStr);
                stmt.setString(18, starsStr);
                stmt.setDate(19, now);

                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
