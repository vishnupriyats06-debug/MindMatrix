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
 * Level4Servlet – Handles the Position Memory Matrix level.
 *   GET  /level4?part=1   → returns JSON with a 4×4 numeric matrix.
 *   GET  /level4?part=2   → returns JSON with a 5×5 alphanumeric matrix (three blanks).
 *   POST /level4          → validates the user's answers.
 */
@WebServlet("/level4")
public class Level4Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Random RAND = new Random();

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
            int[][] matrix = generateNumericMatrix(4);
            session.setAttribute("part1Matrix", matrix);
            out.print(matrixToJson(matrix));
        } else if (part == 2) {
            String[][] matrix = generateAlphaNumericMatrix(5);
            // pick three distinct cells to hide later
            List<int[]> missing = pickMissingCells(5, 3);
            session.setAttribute("part2Matrix", matrix);
            session.setAttribute("part2Missing", missing);
            out.print(alphaMatrixToJson(matrix, missing));
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
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();
        int part = extractInt(body, "part");
        String answersJson = extractArray(body, "answers");
        boolean correct = false;
        if (part == 1) {
            int[][] matrix = (int[][]) session.getAttribute("part1Matrix");
            int[][] userAns = parseIntMatrix(answersJson);
            correct = compareNumeric(matrix, userAns);
        } else if (part == 2) {
            String[][] matrix = (String[][]) session.getAttribute("part2Matrix");
            @SuppressWarnings("unchecked")
            List<int[]> missing = (List<int[]>) session.getAttribute("part2Missing");
            String[][] userAns = parseStringMatrix(answersJson);
            correct = compareAlphaPartial(matrix, userAns, missing);
            
            // updateUserProgress handled via SaveProgressServlet
        }
        PrintWriter out = resp.getWriter();
        out.print("{\"correct\":" + correct + "}");
        out.flush();
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

    // ---------- Helpers ----------
    private int[][] generateNumericMatrix(int size) {
        int[][] m = new int[size][size];
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= size * size; i++) numbers.add(i);
        Collections.shuffle(numbers, RAND);
        int idx = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                m[r][c] = numbers.get(idx++);
            }
        }
        return m;
    }

    private String[][] generateAlphaNumericMatrix(int size) {
        String[][] m = new String[size][size];
        List<String> pool = new ArrayList<>();
        int numCount = (size * size) / 2;
        for (int i = 1; i <= numCount; i++) pool.add(String.valueOf(i));
        char letter = 'A';
        while (pool.size() < size * size) {
            pool.add(String.valueOf(letter++));
            if (letter > 'Z') break;
        }
        Collections.shuffle(pool, RAND);
        int idx = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                m[r][c] = pool.get(idx++);
            }
        }
        return m;
    }

    private List<int[]> pickMissingCells(int size, int count) {
        List<int[]> cells = new ArrayList<>();
        while (cells.size() < count) {
            int r = RAND.nextInt(size);
            int c = RAND.nextInt(size);
            boolean exists = false;
            for (int[] p : cells) {
                if (p[0] == r && p[1] == c) { exists = true; break; }
            }
            if (!exists) cells.add(new int[]{r, c});
        }
        return cells;
    }

    private String matrixToJson(int[][] m) {
        StringBuilder sb = new StringBuilder();
        sb.append('{').append("\"type\":\"numeric\",");
        sb.append("\"matrix\":[");
        for (int i = 0; i < m.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('[');
            for (int j = 0; j < m[i].length; j++) {
                if (j > 0) sb.append(',');
                sb.append(m[i][j]);
            }
            sb.append(']');
        }
        sb.append(']');
        sb.append('}');
        return sb.toString();
    }

    private String alphaMatrixToJson(String[][] m, List<int[]> missing) {
        StringBuilder sb = new StringBuilder();
        sb.append('{').append("\"type\":\"alphanumeric\",");
        sb.append("\"matrix\":[");
        for (int i = 0; i < m.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('[');
            for (int j = 0; j < m[i].length; j++) {
                if (j > 0) sb.append(',');
                sb.append('\"').append(m[i][j]).append('\"');
            }
            sb.append(']');
        }
        sb.append(']');
        sb.append(",\"missing\":[");
        for (int i = 0; i < missing.size(); i++) {
            int[] cell = missing.get(i);
            if (i > 0) sb.append(',');
            sb.append('[').append(cell[0]).append(',').append(cell[1]).append(']');
        }
        sb.append(']');
        sb.append('}');
        return sb.toString();
    }

    // Simple JSON extraction utilities (controlled payloads only)
    private int extractInt(String json, String key) {
        String pattern = "\\\"" + key + "\\\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return -1;
        int start = idx + pattern.length();
        int end = json.indexOf(',', start);
        if (end == -1) end = json.indexOf('}', start);
        return Integer.parseInt(json.substring(start, end).trim());
    }

    private String extractArray(String json, String key) {
        String pattern = "\\\"" + key + "\\\":[";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        int start = idx + pattern.length() - 1; // include '['
        int bracket = 1;
        int i = start + 1;
        while (i < json.length() && bracket > 0) {
            char c = json.charAt(i);
            if (c == '[') bracket++;
            else if (c == ']') bracket--;
            i++;
        }
        return json.substring(start, i);
    }

    private int[][] parseIntMatrix(String json) {
        // Expected format: [[row,col,value],...]
        List<Integer> nums = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+").matcher(json);
        while (m.find()) {
            nums.add(Integer.parseInt(m.group()));
        }
        int entries = nums.size() / 3;
        int[][] result = new int[4][4]; // 4x4 matrix
        for (int i = 0; i < entries; i++) {
            int row = nums.get(i * 3);
            int col = nums.get(i * 3 + 1);
            int val = nums.get(i * 3 + 2);
            result[row][col] = val;
        }
        return result;
    }

    private String[][] parseStringMatrix(String json) {
        // Expected format: [[row,col,"value"],...]
        List<String> values = new ArrayList<>();
        java.util.regex.Matcher vm = java.util.regex.Pattern.compile("\\\"(.*?)\\\"").matcher(json);
        while (vm.find()) {
            values.add(vm.group(1));
        }
        java.util.regex.Matcher numMatcher = java.util.regex.Pattern.compile("-?\\d+").matcher(json);
        List<Integer> nums = new ArrayList<>();
        while (numMatcher.find()) {
            nums.add(Integer.parseInt(numMatcher.group()));
        }
        int entries = values.size();
        String[][] result = new String[5][5]; // 5x5 matrix
        for (int i = 0; i < entries; i++) {
            int row = nums.get(i * 2);
            int col = nums.get(i * 2 + 1);
            result[row][col] = values.get(i);
        }
        return result;
    }

    private boolean compareNumeric(int[][] expected, int[][] actual) {
        if (expected == null || actual == null) return false;
        if (expected.length != actual.length) return false;
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[i].length; j++) {
                if (expected[i][j] != actual[i][j]) return false;
            }
        }
        return true;
    }

    private boolean compareAlphaPartial(String[][] expected, String[][] actual, List<int[]> missing) {
        if (expected == null || actual == null || missing == null) return false;
        for (int[] cell : missing) {
            int r = cell[0];
            int c = cell[1];
            if (!expected[r][c].equalsIgnoreCase(actual[r][c])) return false;
        }
        return true;
    }
}
