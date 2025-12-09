package org.netflixpp.service;

import org.netflixpp.config.DbConfig;
import org.netflixpp.util.JWTUtil;
import java.sql.*;
import java.util.*;

public class AuthService {

    public String login(String username, String password) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, role FROM users WHERE username = ? AND password = ?")) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Em produção usar hash!
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                return JWTUtil.generateToken(username, role);
            }
        }
        return null;
    }

    public boolean register(String username, String password, String email) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)")) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email != null ? email : "");
            stmt.setString(4, "user"); // Default role

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                return false;
            }
            throw e;
        }
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT id FROM users WHERE username = ? AND password = ?");
             PreparedStatement updateStmt = conn.prepareStatement(
                     "UPDATE users SET password = ? WHERE username = ? AND password = ?")) {

            // Verificar senha antiga
            checkStmt.setString(1, username);
            checkStmt.setString(2, oldPassword);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                return false; // Senha antiga incorreta
            }

            // Atualizar senha
            updateStmt.setString(1, newPassword);
            updateStmt.setString(2, username);
            updateStmt.setString(3, oldPassword);

            return updateStmt.executeUpdate() > 0;
        }
    }

    public boolean resetPassword(String email) throws SQLException {
        // Em produção: enviar email com link de reset
        // Por enquanto apenas log
        System.out.println("Password reset requested for email: " + email);
        return true;
    }

    public Map<String, Object> getUserByToken(String token) throws SQLException {
        String username = JWTUtil.getUsername(token);
        if (username == null) return null;

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, role, email, created_at FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("username", rs.getString("username"));
                user.put("role", rs.getString("role"));
                user.put("email", rs.getString("email"));
                user.put("createdAt", rs.getTimestamp("created_at"));
                return user;
            }
        }
        return null;
    }
}