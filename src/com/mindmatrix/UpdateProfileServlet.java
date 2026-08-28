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
 * UpdateProfileServlet – Handles user profile changes (username, email, password).
 * POST /updateProfile
 */
@WebServlet("/updateProfile")
public class UpdateProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized access. Please login.");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String username = trim(request.getParameter("username"));
        String email = trim(request.getParameter("email"));
        String password = trim(request.getParameter("password"));

        // Server-side validation
        if (username.isEmpty() || email.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and Email cannot be empty.");
            return;
        }
        if (username.length() < 3 || username.length() > 50) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username must be between 3 and 50 characters.");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid email address.");
            return;
        }
        if (!password.isEmpty() && password.length() < 6) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 6 characters.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Check if username or email is already taken by a different user
            String checkSql = "SELECT id FROM users WHERE (username = ? OR email = ?) AND id != ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                checkStmt.setString(2, email);
                checkStmt.setInt(3, userId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username or Email already taken.");
                    return;
                }
            }

            // Perform update
            String updateSql;
            if (!password.isEmpty()) {
                updateSql = "UPDATE users SET username = ?, email = ?, password = ? WHERE id = ?";
            } else {
                updateSql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, username);
                updateStmt.setString(2, email);
                if (!password.isEmpty()) {
                    updateStmt.setString(3, password);
                    updateStmt.setInt(4, userId);
                } else {
                    updateStmt.setInt(3, userId);
                }
                updateStmt.executeUpdate();
            }

            // Update session info
            session.setAttribute("username", username);
            session.setAttribute("email", email);

            // Respond success in JSON format
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format("{\"username\":\"%s\",\"email\":\"%s\"}", username, email));

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private String trim(String value) {
        return (value == null) ? "" : value.trim();
    }
}
