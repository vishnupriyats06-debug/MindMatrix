package com.mindmatrix;

import java.io.IOException;
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
 * DeleteAccountServlet – Handles permanently deleting user account, progress,
 * and streak/activity history.
 * POST /deleteAccount
 */
@WebServlet("/deleteAccount")
public class DeleteAccountServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized access. Please login.");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Delete user activity dates
            String sqlAct = "DELETE FROM user_activity_dates WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlAct)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }

            // 2. Delete user progress
            String sqlProg = "DELETE FROM user_progress WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlProg)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }

            // 3. Delete user account
            String sql = "DELETE FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }

            // Invalidate the session
            session.invalidate();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
