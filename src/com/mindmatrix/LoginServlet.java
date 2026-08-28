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
import javax.servlet.http.HttpSession;

/**
 * LoginServlet – Handles user authentication.
 *
 * GET  /login  → forwards to login.html
 * POST /login  → verifies credentials, creates session, redirects to dashboard
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Show the login page */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/login.html");
    }

    /** Process login form submission */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String username = trim(request.getParameter("username"));
        String password = trim(request.getParameter("password"));

        // --- Server-side validation ---
        if (username.isEmpty() || password.isEmpty()) {
            sendError(response, "login.html", "Username and Password are required.");
            return;
        }

        // --- Database lookup ---
        try (Connection conn = DBConnection.getConnection()) {

            String sql = "SELECT id, username, email, avatar_id FROM users WHERE (username = ? OR email = ?) AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, username); // check if the input is their email
                stmt.setString(3, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    // Credentials match – create session
                    HttpSession session = request.getSession();
                    int avatarId = rs.getInt("avatar_id");
                    if (rs.wasNull() || avatarId < 1 || avatarId > 20) {
                        avatarId = 0;
                    }
                    session.setAttribute("userId",   rs.getInt("id"));
                    session.setAttribute("username", rs.getString("username"));
                    session.setAttribute("email",    rs.getString("email"));
                    session.setAttribute("avatarId", avatarId);
                    response.sendRedirect(request.getContextPath() + "/dashboard.html?v=2");
                } else {
                    sendError(response, "login.html", "Invalid username or password. Please try again.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, "login.html", "Database error: " + e.getMessage());
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
