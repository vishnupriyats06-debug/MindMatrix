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
 * Level11Servlet – Object Memory
 *   GET  /level11?part=1  → Random objects placed in a grid (Part 1: Object Position Memory).
 *   GET  /level11?part=2  → Random object collection with one removed (Part 2: Missing Object Detection).
 *   POST /level11         → Called on completion; saves progress on Part 2 completion.
 */
@WebServlet("/level11")
public class Level11Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

    // Object pool: label|emoji
    private static final String[][] OBJECTS = {
        {"Ball",        "\u26BD"},   // ⚽
        {"Book",        "\uD83D\uDCDA"},  // 📖 (U+1F4DA)
        {"Clock",       "\uD83D\uDD50"},  // 🕐
        {"Pen",         "\u270F\uFE0F"}, // ✏️
        {"Headphones",  "\uD83C\uDFA7"}, // 🎧
        {"Coffee",      "\u2615"},        // ☕
        {"Key",         "\uD83D\uDD11"}, // 🔑
        {"Teddy Bear",  "\uD83E\uDDF8"}, // 🧸
        {"Phone",       "\uD83D\uDCF1"}, // 📱
        {"Guitar",      "\uD83C\uDFB8"}, // 🎸
        {"Camera",      "\uD83D\uDCF7"}, // 📷
        {"Lamp",        "\uD83D\uDCA1"}, // 💡
        {"Umbrella",    "\u2602\uFE0F"}, // ☂️
        {"Crown",       "\uD83D\uDC51"}, // 👑
        {"Diamond",     "\uD83D\uDC8E"}, // 💎
        {"Apple",       "\uD83C\uDF4E"}, // 🍎
        {"Rocket",      "\uD83D\uDE80"}, // 🚀
        {"Star",        "\u2B50"},       // ⭐
        {"Moon",        "\uD83C\uDF19"}, // 🌙
        {"Heart",       "\u2764\uFE0F"}, // ❤️
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
            // Part 1: place 6–8 objects in a 4×4 grid (16 cells)
            int objectCount = 6 + RAND.nextInt(3); // 6, 7, or 8
            List<Integer> objPool = new ArrayList<>();
            for (int i = 0; i < OBJECTS.length; i++) objPool.add(i);
            Collections.shuffle(objPool, RAND);
            List<Integer> selectedObjs = new ArrayList<>(objPool.subList(0, objectCount));

            // Pick random unique cells in a 4x4 grid
            List<Integer> cells = new ArrayList<>();
            for (int i = 0; i < 16; i++) cells.add(i);
            Collections.shuffle(cells, RAND);
            List<Integer> selectedCells = new ArrayList<>(cells.subList(0, objectCount));

            // Build JSON
            StringBuilder sb = new StringBuilder("{\"part\":1,\"gridCols\":4,\"gridRows\":4,\"placements\":[");
            for (int i = 0; i < objectCount; i++) {
                int oi = selectedObjs.get(i);
                int cell = selectedCells.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"objIdx\":").append(oi)
                  .append(",\"label\":\"").append(escapeJson(OBJECTS[oi][0])).append("\"")
                  .append(",\"emoji\":\"").append(escapeJson(OBJECTS[oi][1])).append("\"")
                  .append(",\"cell\":").append(cell).append("}");
            }
            sb.append("]}");

            // Store in session for server-side validation (optional — client validates)
            session.setAttribute("level11p1Placements", sb.toString());
            out.print(sb.toString());

        } else if (part == 2) {
            // Part 2: show 6–8 objects, randomly remove one
            int objectCount = 6 + RAND.nextInt(3); // 6, 7, or 8
            List<Integer> objPool = new ArrayList<>();
            for (int i = 0; i < OBJECTS.length; i++) objPool.add(i);
            Collections.shuffle(objPool, RAND);
            List<Integer> selectedObjs = new ArrayList<>(objPool.subList(0, objectCount));

            int missingIdx = RAND.nextInt(objectCount); // which position is missing

            StringBuilder sb = new StringBuilder("{\"part\":2,\"objects\":[");
            for (int i = 0; i < objectCount; i++) {
                int oi = selectedObjs.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"objIdx\":").append(oi)
                  .append(",\"label\":\"").append(escapeJson(OBJECTS[oi][0])).append("\"")
                  .append(",\"emoji\":\"").append(escapeJson(OBJECTS[oi][1])).append("\"}");
            }
            sb.append("],\"missingIdx\":").append(missingIdx).append("}");

            session.setAttribute("level11p2MissingLabel", OBJECTS[selectedObjs.get(missingIdx)][0]);
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
