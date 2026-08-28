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
 * Level10Servlet – Mask Memory
 *   GET  /level10?part=1  → 6 random person-mask pairings for Part 1 (Person → Mask).
 *   GET  /level10?part=2  → 6 random person-mask pairings for Part 2 (Mask → Person).
 *   POST /level10         → Validates user submission, saves progress on Part 2 completion.
 */
@WebServlet("/level10")
public class Level10Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    // Person pool: name|emoji pairs
    private static final String[][] PEOPLE = {
        {"Alex",   "\uD83D\uDC68"},       // 👨
        {"Maya",   "\uD83D\uDC69"},       // 👩
        {"George", "\uD83D\uDC74"},       // 👴
        {"Lily",   "\uD83D\uDC67"},       // 👧
        {"Sam",    "\uD83E\uDDD1"},       // 🧑
        {"Rose",   "\uD83D\uDC69\u200D\uD83E\uDDB0"}, // 👩‍🦰
        {"Jordan", "\uD83D\uDC71"},       // 👱
        {"Finn",   "\uD83D\uDC76"},       // 👶
        {"Grace",  "\uD83D\uDC75"},       // 👵
        {"Noah",   "\uD83D\uDC66"}        // 👦
    };

    // Mask pool: label|emoji pairs
    private static final String[][] MASKS = {
        {"Star",     "\u2B50"},     // ⭐
        {"Red",      "\uD83D\uDD34"},     // 🔴
        {"Diamond",  "\uD83D\uDC8E"},     // 💎
        {"Moon",     "\uD83C\uDF19"},     // 🌙
        {"Blue",     "\uD83D\uDD35"},     // 🔵
        {"Green",    "\uD83D\uDFE2"},     // 🟢
        {"Orange",   "\uD83D\uDD36"},     // 🔶
        {"Purple",   "\uD83D\uDC9C"},     // 💜
        {"Yellow",   "\uD83D\uDFE1"},     // 🟡
        {"Black",    "\u2B1B"}            // ⬛
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        int part = req.getParameter("part") == null ? 1 : Integer.parseInt(req.getParameter("part"));
        PrintWriter out = resp.getWriter();

        // Select 6 random people
        List<Integer> peopleIdx = new ArrayList<>();
        for (int i = 0; i < PEOPLE.length; i++) peopleIdx.add(i);
        Collections.shuffle(peopleIdx, RAND);
        List<Integer> selectedPeople = peopleIdx.subList(0, 6);

        // Select 6 random masks
        List<Integer> maskIdx = new ArrayList<>();
        for (int i = 0; i < MASKS.length; i++) maskIdx.add(i);
        Collections.shuffle(maskIdx, RAND);
        List<Integer> selectedMasks = maskIdx.subList(0, 6);

        // Build JSON arrays
        StringBuilder peopleJson = new StringBuilder("[");
        StringBuilder masksJson  = new StringBuilder("[");
        StringBuilder mappingJson = new StringBuilder("[");

        for (int i = 0; i < 6; i++) {
            int pi = selectedPeople.get(i);
            int mi = selectedMasks.get(i);
            if (i > 0) { peopleJson.append(","); masksJson.append(","); mappingJson.append(","); }
            peopleJson.append("{\"name\":\"").append(PEOPLE[pi][0]).append("\",\"emoji\":\"").append(escapeJson(PEOPLE[pi][1])).append("\"}");
            masksJson.append("{\"label\":\"").append(MASKS[mi][0]).append("\",\"emoji\":\"").append(escapeJson(MASKS[mi][1])).append("\"}");
            mappingJson.append("{\"personIdx\":").append(i).append(",\"maskIdx\":").append(i).append("}");
        }
        peopleJson.append("]");
        masksJson.append("]");
        mappingJson.append("]");

        // Store correct mapping in session
        if (part == 1) {
            session.setAttribute("level10p1People", selectedPeople);
            session.setAttribute("level10p1Masks", selectedMasks);
            out.print("{\"people\":" + peopleJson + ",\"masks\":" + masksJson + ",\"mapping\":" + mappingJson + "}");
        } else if (part == 2) {
            session.setAttribute("level10p2People", selectedPeople);
            session.setAttribute("level10p2Masks", selectedMasks);
            out.print("{\"people\":" + peopleJson + ",\"masks\":" + masksJson + ",\"mapping\":" + mappingJson + "}");
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

        if (part == 1 && session.getAttribute("level10p1People") != null && isCorrect) {
            valid = true;
        } else if (part == 2 && session.getAttribute("level10p2People") != null && isCorrect) {
            valid = true;
            // updateUserProgress handled via SaveProgressServlet
        }
        resp.getWriter().print("{\"success\":" + valid + "}");
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
