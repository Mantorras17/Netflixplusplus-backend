package org.netflixpp.service;

import org.netflixpp.config.DbConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    /**
     * Cria ou atualiza um utilizador interno com base nos dados do Firebase.
     * @param firebaseUid UID do Firebase (obrigatório)
     * @param email Email do Firebase (pode ser null)
     * @param name Nome do Firebase (pode ser null)
     * @return Map com dados do utilizador interno (id, username, role, email, createdAt, firebaseUid)
     */
    public Map<String, Object> getOrCreateUserFromFirebase(String firebaseUid, String email, String name) throws SQLException {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return null;
        }

        try (Connection conn = DbConfig.getMariaDB()) {
            // 1) Verificar se já existe user com este firebase_uid
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id, username, role, email, created_at, firebase_uid " +
                            "FROM users WHERE firebase_uid = ?")) {

                checkStmt.setString(1, firebaseUid);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    return mapUser(rs);
                }
            }

            // 2) Se não existir, criar um novo utilizador
            String username = (email != null && !email.isBlank())
                    ? email
                    : ("user_" + firebaseUid.substring(0, Math.min(8, firebaseUid.length())));

            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (username, email, role, firebase_uid) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                insertStmt.setString(1, username);
                insertStmt.setString(2, email != null ? email : "");
                insertStmt.setString(3, "user"); // role padrão
                insertStmt.setString(4, firebaseUid);

                int affected = insertStmt.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long newId = generatedKeys.getLong(1);
                        // Buscar o registo completo
                        try (PreparedStatement selectStmt = conn.prepareStatement(
                                "SELECT id, username, role, email, created_at, firebase_uid FROM users WHERE id = ?")) {
                            selectStmt.setLong(1, newId);
                            ResultSet rs = selectStmt.executeQuery();
                            if (rs.next()) {
                                return mapUser(rs);
                            }
                        }
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }
        }
        return null;
    }

    /**
     * Obtém utilizador interno a partir do firebaseUid.
     */
    public Map<String, Object> getUserByFirebaseUid(String firebaseUid) throws SQLException {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return null;
        }

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, role, email, created_at, firebase_uid FROM users WHERE firebase_uid = ?")) {

            stmt.setString(1, firebaseUid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }
        }
        return null;
    }

    /**
     * Mapeia ResultSet -> Map<String, Object> com os campos principais do utilizador.
     */
    private Map<String, Object> mapUser(ResultSet rs) throws SQLException {
        Map<String, Object> user = new HashMap<>();
        user.put("id", rs.getInt("id"));
        user.put("username", rs.getString("username"));
        user.put("role", rs.getString("role"));
        user.put("email", rs.getString("email"));
        user.put("createdAt", rs.getTimestamp("created_at"));
        // Se tiveres a coluna firebase_uid
        try {
            user.put("firebaseUid", rs.getString("firebase_uid"));
        } catch (SQLException ignored) {
            // coluna opcional
        }
        return user;
    }

}
