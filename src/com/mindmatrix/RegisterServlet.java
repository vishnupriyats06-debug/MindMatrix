package com.mindmatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * RegisterServlet – Handles new user registration.
 *
 * GET  /register  → forwards to register.html
 * POST /register  → validates, inserts user, redirects to dashboard
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Show the registration page */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/register.html");
    }

    /** Process registration form submission */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String username = trim(request.getParameter("username"));
        String email    = trim(request.getParameter("email"));
        String password = trim(request.getParameter("password"));

        // --- Server-side validation ---
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            sendError(response, "register.html", "All fields are required.");
            return;
        }
        if (username.length() < 3 || username.length() > 50) {
            sendError(response, "register.html", "Username must be 3–50 characters.");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            sendError(response, "register.html", "Please enter a valid email address.");
            return;
        }
        if (password.length() < 6) {
            sendError(response, "register.html", "Password must be at least 6 characters.");
            return;
        }

        // --- Database operations ---
        try (Connection conn = DBConnection.getConnection()) {

            // Check for existing username or email
            String checkSql = "SELECT id FROM users WHERE username = ? OR email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                checkStmt.setString(2, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    sendError(response, "register.html", "Username or Email already exists. Please use a different one.");
                    return;
                }
            }

            // Insert new user with default avatar_id = 0 (neutral profile state)
            int userId = 0;
            String insertSql = "INSERT INTO users (username, email, password, avatar_id) VALUES (?, ?, ?, 0)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, email);
                insertStmt.setString(3, password);   // Plain text – hash with BCrypt for production
                insertStmt.executeUpdate();
                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            // Insert default progress row for this user
            String progressSql = "INSERT INTO user_progress (user_id, score, unlocked_level, streak, games_played, best_streak, hints, best_scores, best_times, stars) " +
                                 "VALUES (?, 0, 1, 0, 0, 0, 3, '[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]', '[\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\",\"-\"]', '[\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\"]')";
            try (PreparedStatement progressStmt = conn.prepareStatement(progressSql)) {
                progressStmt.setInt(1, userId);
                progressStmt.executeUpdate();
            }

            // Create session and redirect to dashboard
            HttpSession session = request.getSession();
            session.setAttribute("userId", userId);
            session.setAttribute("username", username);
            session.setAttribute("email", email);
            session.setAttribute("avatarId", 0);
            response.sendRedirect(request.getContextPath() + "/dashboard.html?v=2");

        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, "register.html", "Database error: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String trim(String value) {
        return (value == null) ? "" : value.trim();
    }

    /**
     * Redirect back to the given page with an error message encoded in the URL.
     */
    private void sendError(HttpServletResponse response, String page, String message)
            throws IOException {
        String encoded = java.net.URLEncoder.encode(message, "UTF-8");
        response.sendRedirect(page + "?error=" + encoded);
    }
}
