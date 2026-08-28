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
 * UpdateAvatarServlet – Updates the selected profile avatar ID (1–20).
 * POST /updateAvatar
 */
@WebServlet("/updateAvatar")
public class UpdateAvatarServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Integer userId = null;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            userId = (Integer) session.getAttribute("userId");
        }

        String usernameParam = request.getParameter("username");
        if (usernameParam != null) {
            usernameParam = usernameParam.trim();
        }

        String avatarIdStr = request.getParameter("avatarId");
        if (avatarIdStr == null || avatarIdStr.trim().isEmpty()) {
            avatarIdStr = request.getParameter("avatar_id");
        }

        if (avatarIdStr == null || avatarIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Please select an avatar first.\"}");
            return;
        }

        int avatarId;
        try {
            avatarId = Integer.parseInt(avatarIdStr.trim());
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Invalid avatar ID format.\"}");
            return;
        }

        // Strict validation: avatarId must be between 1 and 20
        if (avatarId < 1 || avatarId > 20) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Invalid avatar ID. Please choose an avatar between 1 and 20.\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // If session doesn't have userId, attempt to look up via usernameParam
            if (userId == null) {
                if (usernameParam != null && !usernameParam.isEmpty()) {
                    String findUserSql = "SELECT id, username, email FROM users WHERE username = ? OR email = ?";
                    try (PreparedStatement findStmt = conn.prepareStatement(findUserSql)) {
                        findStmt.setString(1, usernameParam);
                        findStmt.setString(2, usernameParam);
                        try (ResultSet rs = findStmt.executeQuery()) {
                            if (rs.next()) {
                                userId = rs.getInt("id");
                                session = request.getSession(true);
                                session.setAttribute("userId", userId);
                                session.setAttribute("username", rs.getString("username"));
                                session.setAttribute("email", rs.getString("email"));
                            }
                        }
                    }
                }
            }

            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"error\":\"Please login to save your avatar.\"}");
                return;
            }

            // Update user's avatar in MySQL
            String sql = "UPDATE users SET avatar_id = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, avatarId);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }

            // Update session attribute
            if (session != null) {
                session.setAttribute("avatarId", avatarId);
            }

            PrintWriter out = response.getWriter();
            out.write(String.format("{\"success\":true,\"avatarId\":%d,\"message\":\"Avatar saved successfully!\"}", avatarId));
            out.flush();

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }
}
