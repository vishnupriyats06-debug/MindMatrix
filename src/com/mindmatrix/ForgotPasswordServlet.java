package com.mindmatrix;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ForgotPasswordServlet – Handles password reset request for users who forgot their password.
 *
 * GET  /forgot-password  → forwards to forgot-password.html
 * POST /forgot-password  → validates user identity, updates password, redirects to login.html with success
 */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Show the forgot password page */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/forgot-password.html");
    }

    /** Process password reset form submission */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String identifier      = trim(request.getParameter("identifier"));
        String newPassword     = trim(request.getParameter("newPassword"));
        String confirmPassword = trim(request.getParameter("confirmPassword"));

        // --- Server-side validation ---
        if (identifier.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            sendError(response, "forgot-password.html", "All fields are required.");
            return;
        }

        if (newPassword.length() < 6) {
            sendError(response, "forgot-password.html", "New password must be at least 6 characters.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            sendError(response, "forgot-password.html", "Passwords do not match.");
            return;
        }

        // --- Database operation ---
        try (Connection conn = DBConnection.getConnection()) {

            // Check if user exists with the provided username or email
            int userId = -1;
            String checkSql = "SELECT id FROM users WHERE username = ? OR email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, identifier);
                checkStmt.setString(2, identifier);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("id");
                } else {
                    sendError(response, "forgot-password.html", "No account found with that Username or Email address.");
                    return;
                }
            }

            // Update user password
            String updateSql = "UPDATE users SET password = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newPassword);
                updateStmt.setInt(2, userId);
                updateStmt.executeUpdate();
            }

            // Password updated successfully -> redirect to login with success message
            String successMsg = java.net.URLEncoder.encode("Password reset successfully! Please sign in with your new password.", "UTF-8");
            response.sendRedirect(request.getContextPath() + "/login.html?success=" + successMsg);

        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, "forgot-password.html", "Database error: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String trim(String value) {
        return (value == null) ? "" : value.trim();
    }

    private void sendError(HttpServletResponse response, String page, String message)
            throws IOException {
        String encoded = java.net.URLEncoder.encode(message, "UTF-8");
        response.sendRedirect(page + "?error=" + encoded);
    }
}
