package org.netflixpp.service;

import org.netflixpp.config.MariaDBConfig;
import org.netflixpp.model.User;

import java.sql.*;
import java.util.*;

public class UserService {

    public List<User> listUsers() throws Exception {
        List<User> users = new ArrayList<>();
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT id, username, password, role, email FROM users");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("email")
                ));
            }
        }
        return users;
    }

    public User getUserById(int id) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT id, username, password, role, email FROM users WHERE id = ?")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("email")
                    );
                }
            }
        }
        return null;
    }

    public User getUserByUsername(String username) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT id, username, password, role, email FROM users WHERE username = ?")) {
            st.setString(1, username);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("email")
                    );
                }
            }
        }
        return null;
    }

    public void createUser(String username, String password, String role, String email) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("INSERT INTO users (username, password, role, email) VALUES (?, ?, ?, ?)")) {
            st.setString(1, username);
            st.setString(2, password);
            st.setString(3, role);
            st.setString(4, email);
            st.executeUpdate();
        }
    }

    public void updateUser(int id, String username, String password, String role, String email) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement(
                     "UPDATE users SET username=?, password=?, role=?, email=? WHERE id=?")) {
            st.setString(1, username);
            st.setString(2, password);
            st.setString(3, role);
            st.setString(4, email);
            st.setInt(5, id);
            st.executeUpdate();
        }
    }

    public void deleteUser(int id) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
            st.setInt(1, id);
            st.executeUpdate();
        }
    }
}
