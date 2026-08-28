package com.mindmatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * StreakServlet – Dedicated endpoint for checking and updating user streak info
 * and retrieving full historical activity dates.
 * GET  /streak  → Returns current streak and list of active dates.
 * POST /streak  → Updates streak on level completion and returns updated data.
 */
@WebServlet("/streak")
public class StreakServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().print("{\"error\":\"Unauthorized\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String dateParam = req.getParameter("lastPlayedDate");
        if (dateParam == null || dateParam.trim().isEmpty()) {
            dateParam = req.getParameter("clientDate");
        }
        java.time.LocalDate activityDate = null;
        if (dateParam != null && dateParam.trim().length() >= 10) {
            try {
                activityDate = java.time.LocalDate.parse(dateParam.trim().substring(0, 10));
            } catch (Exception e) {
                activityDate = java.time.LocalDate.now();
            }
        } else {
            activityDate = java.time.LocalDate.now();
        }

        try {
            StreakDAO.StreakInfo info = StreakService.getUserStreak(userId, activityDate);
            String lastDateStr = (info.lastActivityDate != null) ? info.lastActivityDate.toString() : "";
            
            PrintWriter out = resp.getWriter();
            out.print("{"
                + "\"success\":true,"
                + "\"currentStreak\":" + info.currentStreak + ","
                + "\"longestStreak\":" + info.longestStreak + ","
                + "\"lastActivityDate\":\"" + lastDateStr + "\","
                + "\"playedToday\":" + info.playedToday + ","
                + "\"activityDates\":" + toJsonArray(info.activityDates)
                + "}");
            out.flush();
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().print("{\"error\":\"Unauthorized\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String dateParam = req.getParameter("lastPlayedDate");
        if (dateParam == null || dateParam.trim().isEmpty()) {
            dateParam = req.getParameter("clientDate");
        }
        java.time.LocalDate activityDate = null;
        if (dateParam != null && dateParam.trim().length() >= 10) {
            try {
                activityDate = java.time.LocalDate.parse(dateParam.trim().substring(0, 10));
            } catch (Exception e) {
                activityDate = java.time.LocalDate.now();
            }
        } else {
            activityDate = java.time.LocalDate.now();
        }

        try {
            StreakDAO.StreakInfo updated = StreakService.handleLevelCompleted(userId, activityDate);
            String lastDateStr = (updated.lastActivityDate != null) ? updated.lastActivityDate.toString() : "";

            PrintWriter out = resp.getWriter();
            out.print("{"
                + "\"success\":true,"
                + "\"currentStreak\":" + updated.currentStreak + ","
                + "\"longestStreak\":" + updated.longestStreak + ","
                + "\"lastActivityDate\":\"" + lastDateStr + "\","
                + "\"playedToday\":true,"
                + "\"activityDates\":" + toJsonArray(updated.activityDates)
                + "}");
            out.flush();
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
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
}
