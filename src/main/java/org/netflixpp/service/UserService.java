package org.netflixpp.service;

import org.netflixpp.config.DbConfig;
import java.sql.*;
import java.util.*;


public class UserService {

    public Map<String, Object> getUserProfile(String username) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, role, email, created_at FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> profile = new HashMap<>();
                profile.put("id", rs.getInt("id"));
                profile.put("username", rs.getString("username"));
                profile.put("role", rs.getString("role"));
                profile.put("email", rs.getString("email"));
                profile.put("createdAt", rs.getTimestamp("created_at"));

                // Adicionar estatísticas
                addUserStatistics(rs.getInt("id"), profile);

                return profile;
            }
        }
        return null;
    }

    public boolean updateUserProfile(String username, Map<String, String> updates)
            throws SQLException {

        if (updates.isEmpty()) return false;

        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> params = new ArrayList<>();

        if (updates.containsKey("email")) {
            sql.append("email = ?, ");
            params.add(updates.get("email"));
        }
        if (updates.containsKey("password")) {
            sql.append("password = ?, ");
            params.add(updates.get("password")); // Em produção: hash!
        }

        if (params.isEmpty()) return false;

        // Remover última vírgula
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE username = ?");
        params.add(username);

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            return stmt.executeUpdate() > 0;
        }
    }

    public Map<String, Object> getWatchHistory(String username, int page, int limit)
            throws SQLException {

        Map<String, Object> result = new HashMap<>();
        int offset = (page - 1) * limit;

        try (Connection conn = DbConfig.getMariaDB()) {
            // Total de itens
            try (PreparedStatement countStmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM watch_history wh " +
                            "JOIN users u ON wh.user_id = u.id WHERE u.username = ?")) {

                countStmt.setString(1, username);
                ResultSet rs = countStmt.executeQuery();
                if (rs.next()) {
                    result.put("total", rs.getInt("total"));
                }
            }

            // Histórico
            List<Map<String, Object>> history = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT m.id, m.title, m.category, m.genre, wh.watched_at, wh.progress " +
                            "FROM watch_history wh " +
                            "JOIN movies m ON wh.movie_id = m.id " +
                            "JOIN users u ON wh.user_id = u.id " +
                            "WHERE u.username = ? " +
                            "ORDER BY wh.watched_at DESC LIMIT ? OFFSET ?")) {

                stmt.setString(1, username);
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("movieId", rs.getInt("id"));
                    entry.put("title", rs.getString("title"));
                    entry.put("category", rs.getString("category"));
                    entry.put("genre", rs.getString("genre"));
                    entry.put("watchedAt", rs.getTimestamp("watched_at"));
                    entry.put("progress", rs.getInt("progress"));
                    history.add(entry);
                }
            }

            result.put("history", history);
            result.put("page", page);
            result.put("limit", limit);
            result.put("pages", (int) Math.ceil((double) (int) result.get("total") / limit));
        }

        return result;
    }

    public boolean addToWatchHistory(String username, int movieId, Integer progress)
            throws SQLException {

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO watch_history (user_id, movie_id, progress) " +
                             "SELECT u.id, ?, ? FROM users u WHERE u.username = ? " +
                             "ON DUPLICATE KEY UPDATE watched_at = CURRENT_TIMESTAMP, " +
                             "progress = COALESCE(?, progress), views = views + 1")) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, progress != null ? progress : 0);
            stmt.setString(3, username);
            stmt.setInt(4, progress != null ? progress : 0);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeFromWatchHistory(String username, int movieId) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE wh FROM watch_history wh " +
                             "JOIN users u ON wh.user_id = u.id " +
                             "WHERE u.username = ? AND wh.movie_id = ?")) {

            stmt.setString(1, username);
            stmt.setInt(2, movieId);

            return stmt.executeUpdate() > 0;
        }
    }

    public Map<String, Object> getFavorites(String username) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT m.id, m.title, m.description, m.category, m.genre, " +
                             "f.added_at FROM favorites f " +
                             "JOIN movies m ON f.movie_id = m.id " +
                             "JOIN users u ON f.user_id = u.id " +
                             "WHERE u.username = ? ORDER BY f.added_at DESC")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> favorites = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> movie = new HashMap<>();
                movie.put("id", rs.getInt("id"));
                movie.put("title", rs.getString("title"));
                movie.put("description", rs.getString("description"));
                movie.put("category", rs.getString("category"));
                movie.put("genre", rs.getString("genre"));
                movie.put("addedAt", rs.getTimestamp("added_at"));
                favorites.add(movie);
            }

            result.put("favorites", favorites);
            result.put("count", favorites.size());
        }

        return result;
    }

    public boolean addFavorite(String username, int movieId) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO favorites (user_id, movie_id) " +
                             "SELECT u.id, ? FROM users u WHERE u.username = ? " +
                             "ON DUPLICATE KEY UPDATE added_at = CURRENT_TIMESTAMP")) {

            stmt.setInt(1, movieId);
            stmt.setString(2, username);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeFavorite(String username, int movieId) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE f FROM favorites f " +
                             "JOIN users u ON f.user_id = u.id " +
                             "WHERE u.username = ? AND f.movie_id = ?")) {

            stmt.setString(1, username);
            stmt.setInt(2, movieId);

            return stmt.executeUpdate() > 0;
        }
    }

    public Map<String, Object> getRecommendations(String username, int limit)
            throws SQLException {

        Map<String, Object> result = new HashMap<>();

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT m.* FROM movies m " +
                             "WHERE m.category IN ( " +
                             "  SELECT DISTINCT m2.category FROM watch_history wh " +
                             "  JOIN movies m2 ON wh.movie_id = m2.id " +
                             "  JOIN users u ON wh.user_id = u.id " +
                             "  WHERE u.username = ? " +
                             ") " +
                             "AND m.id NOT IN ( " +
                             "  SELECT wh.movie_id FROM watch_history wh " +
                             "  JOIN users u ON wh.user_id = u.id " +
                             "  WHERE u.username = ? " +
                             ") " +
                             "ORDER BY RAND() LIMIT ?")) {

            stmt.setString(1, username);
            stmt.setString(2, username);
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> recommendations = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> movie = new HashMap<>();
                movie.put("id", rs.getInt("id"));
                movie.put("title", rs.getString("title"));
                movie.put("description", rs.getString("description"));
                movie.put("category", rs.getString("category"));
                movie.put("genre", rs.getString("genre"));
                movie.put("year", rs.getInt("year"));
                movie.put("duration", rs.getInt("duration"));
                recommendations.add(movie);
            }

            result.put("recommendations", recommendations);
            result.put("count", recommendations.size());
            result.put("basedOn", "watch history");
        }

        return result;
    }

    public Map<String, Object> getUserActivity(String username, int days) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT DATE(watched_at) as date, COUNT(*) as views, " +
                             "SUM(CASE WHEN progress >= 90 THEN 1 ELSE 0 END) as completed " +
                             "FROM watch_history wh " +
                             "JOIN users u ON wh.user_id = u.id " +
                             "WHERE u.username = ? AND watched_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                             "GROUP BY DATE(watched_at) ORDER BY date DESC")) {

            stmt.setString(1, username);
            stmt.setInt(2, days);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> activity = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> day = new HashMap<>();
                day.put("date", rs.getDate("date"));
                day.put("views", rs.getInt("views"));
                day.put("completed", rs.getInt("completed"));
                activity.add(day);
            }

            result.put("activity", activity);
            result.put("days", days);
            result.put("totalViews", activity.stream()
                    .mapToInt(d -> (int) d.get("views"))
                    .sum());
            result.put("totalCompleted", activity.stream()
                    .mapToInt(d -> (int) d.get("completed"))
                    .sum());
        }

        return result;
    }

    public boolean rateMovie(String username, int movieId, int rating) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO ratings (user_id, movie_id, rating) " +
                             "SELECT u.id, ?, ? FROM users u WHERE u.username = ? " +
                             "ON DUPLICATE KEY UPDATE rating = ?, rated_at = CURRENT_TIMESTAMP")) {

            stmt.setInt(1, movieId);
            stmt.setInt(2, rating);
            stmt.setString(3, username);
            stmt.setInt(4, rating);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteAccount(String username) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        }
    }

    // Métodos auxiliares
    private void addUserStatistics(int userId, Map<String, Object> profile) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB()) {
            // Total de visualizações
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total_views, " +
                            "COUNT(DISTINCT movie_id) as unique_movies, " +
                            "MAX(watched_at) as last_watched " +
                            "FROM watch_history WHERE user_id = ?")) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    profile.put("totalViews", rs.getInt("total_views"));
                    profile.put("uniqueMovies", rs.getInt("unique_movies"));
                    profile.put("lastWatched", rs.getTimestamp("last_watched"));
                }
            }

            // Total de favoritos
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total_favorites FROM favorites WHERE user_id = ?")) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    profile.put("totalFavorites", rs.getInt("total_favorites"));
                }
            }

            // Categoria favorita
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT m.category, COUNT(*) as count FROM watch_history wh " +
                            "JOIN movies m ON wh.movie_id = m.id " +
                            "WHERE wh.user_id = ? AND m.category IS NOT NULL " +
                            "GROUP BY m.category ORDER BY count DESC LIMIT 1")) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    profile.put("favoriteCategory", rs.getString("category"));
                }
            }
        }
    }
}