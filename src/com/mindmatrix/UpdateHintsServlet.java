package com.mindmatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * UpdateHintsServlet – Updates the remaining hints count in the database for the logged-in user.
 * POST /updateHints
 */
@WebServlet("/updateHints")
public class UpdateHintsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized access. Please login.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String hintsParam = request.getParameter("hints");
        if (hintsParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Missing hints parameter\"}");
            return;
        }

        try {
            int hints = Math.max(0, Integer.parseInt(hintsParam));

            try (Connection conn = DBConnection.getConnection()) {
                String sql = "UPDATE user_progress SET hints = ? WHERE user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, hints);
                    stmt.setInt(2, userId);
                    int rows = stmt.executeUpdate();
                    if (rows == 0) {
                        // User progress record not yet created, insert it
                        String insSql = "INSERT INTO user_progress (user_id, hints) VALUES (?, ?)";
                        try (PreparedStatement insStmt = conn.prepareStatement(insSql)) {
                            insStmt.setInt(1, userId);
                            insStmt.setInt(2, hints);
                            insStmt.executeUpdate();
                        }
                    }
                }
            }

            PrintWriter out = response.getWriter();
            out.print("{\"status\":\"ok\",\"hints\":" + hints + "}");
            out.flush();

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid hints number format\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
}
