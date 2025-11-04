package org.netflixpp.service;

import org.netflixpp.config.MariaDBConfig;
import org.netflixpp.model.User;
import org.netflixpp.util.JWTUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {

    public String login(String username, String password) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT id, username, password, role, email FROM users WHERE username = ?")) {
            st.setString(1, username);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                String pw = rs.getString("password");
                // Aqui estamos a comparar plain-text; em produção compara hashes.
                if (!password.equals(pw)) return null;
                String role = rs.getString("role");
                return JWTUtil.generateToken(username, role);
            }
        }
    }

    public boolean register(String username, String password, String role, String email) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("INSERT INTO users (username, password, role, email) VALUES (?, ?, ?, ?)")) {
            st.setString(1, username);
            st.setString(2, password);
            st.setString(3, role == null ? "user" : role);
            st.setString(4, email);
            st.executeUpdate();
            return true;
        } catch (Exception e) {
            // possivelmente username duplicado -> false
            return false;
        }
    }

    public User getUserByUsername(String username) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT id, username, password, role, email FROM users WHERE username = ?")) {
            st.setString(1, username);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"), rs.getString("role"), rs.getString("email"));
                }
            }
        }
        return null;
    }
}
