package org.netflixpp.service;

import org.netflixpp.config.DbConfig;
import org.netflixpp.mesh.ChunkManager;
import java.sql.*;
import java.util.*;

public class MovieService {

    private final ChunkManager chunkManager = new ChunkManager();

    public List<Map<String, Object>> getAllMovies() throws SQLException {
        return getMoviesWithQuery("SELECT * FROM movies ORDER BY created_at DESC");
    }

    public List<Map<String, Object>> getFeaturedMovies() throws SQLException {
        return getMoviesWithQuery(
                "SELECT * FROM movies ORDER BY RAND() LIMIT 10");
    }

    public List<Map<String, Object>> getMoviesByCategory(String category) throws SQLException {
        return getMoviesWithQuery(
                "SELECT * FROM movies WHERE category = ? ORDER BY title", category);
    }

    public List<Map<String, Object>> getMoviesByGenre(String genre) throws SQLException {
        return getMoviesWithQuery(
                "SELECT * FROM movies WHERE genre = ? ORDER BY title", genre);
    }

    public List<Map<String, Object>> getRecentMovies(int limit) throws SQLException {
        return getMoviesWithQuery(
                "SELECT * FROM movies ORDER BY created_at DESC LIMIT ?", limit);
    }

    public Map<String, Object> getMovieById(int id) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM movies WHERE id = ?")) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractMovieFromResultSet(rs);
            }
        }
        return null;
    }

    public Map<String, Object> getMovieWithDetails(int id) throws SQLException {
        Map<String, Object> movie = getMovieById(id);
        if (movie != null) {
            // Adicionar informações de chunks se disponível
            String movieId = "movie_" + id;
            Map<String, Object> chunkInfo = chunkManager.getMovieChunkInfo(movieId);
            movie.put("chunks", chunkInfo.get("chunks"));
            movie.put("chunkCount", chunkInfo.get("count"));

            // Adicionar estatísticas de visualização
            movie.put("views", getMovieViewCount(id));
            movie.put("averageRating", getMovieAverageRating(id));
        }
        return movie;
    }

    public List<Map<String, Object>> searchMovies(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return getAllMovies();
        }

        String searchTerm = "%" + query + "%";
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM movies WHERE title LIKE ? OR description LIKE ? " +
                             "OR category LIKE ? OR genre LIKE ? ORDER BY title")) {

            for (int i = 1; i <= 4; i++) {
                stmt.setString(i, searchTerm);
            }

            ResultSet rs = stmt.executeQuery();
            List<Map<String, Object>> movies = new ArrayList<>();

            while (rs.next()) {
                movies.add(extractMovieFromResultSet(rs));
            }

            return movies;
        }
    }

    public List<String> getAllCategories() throws SQLException {
        List<String> categories = new ArrayList<>();

        try (Connection conn = DbConfig.getMariaDB();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT DISTINCT category FROM movies WHERE category IS NOT NULL ORDER BY category")) {

            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }

        return categories;
    }

    public List<String> getAllGenres() throws SQLException {
        List<String> genres = new ArrayList<>();

        try (Connection conn = DbConfig.getMariaDB();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT DISTINCT genre FROM movies WHERE genre IS NOT NULL ORDER BY genre")) {

            while (rs.next()) {
                genres.add(rs.getString("genre"));
            }
        }

        return genres;
    }

    public int createMovie(String title, String description, String category,
                           String genre, int year, int duration) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO movies (title, description, category, genre, year, duration) " +
                             "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setString(3, category);
            stmt.setString(4, genre);
            stmt.setInt(5, year);
            stmt.setInt(6, duration);

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

            return -1;
        }
    }

    public boolean updateMovie(int id, String title, String description, String category,
                               String genre, Integer year, Integer duration) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE movies SET ");
        List<Object> params = new ArrayList<>();

        if (title != null) {
            sql.append("title = ?, ");
            params.add(title);
        }
        if (description != null) {
            sql.append("description = ?, ");
            params.add(description);
        }
        if (category != null) {
            sql.append("category = ?, ");
            params.add(category);
        }
        if (genre != null) {
            sql.append("genre = ?, ");
            params.add(genre);
        }
        if (year != null) {
            sql.append("year = ?, ");
            params.add(year);
        }
        if (duration != null) {
            sql.append("duration = ?, ");
            params.add(duration);
        }

        if (params.isEmpty()) {
            return false;
        }

        // Remover última vírgula
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(id);

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteMovie(int id) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM movies WHERE id = ?")) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateMovieFilePaths(int id, String path1080, String path360) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE movies SET file_path_1080 = ?, file_path_360 = ? WHERE id = ?")) {

            stmt.setString(1, path1080);
            stmt.setString(2, path360);
            stmt.setInt(3, id);

            return stmt.executeUpdate() > 0;
        }
    }

    public void recordMovieView(int movieId, int userId) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO watch_history (user_id, movie_id) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE watched_at = CURRENT_TIMESTAMP, views = views + 1")) {

            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            stmt.executeUpdate();
        }
    }

    public int getMovieViewCount(int movieId) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) as view_count FROM watch_history WHERE movie_id = ?")) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("view_count");
            }
            return 0;
        }
    }

    public double getMovieAverageRating(int movieId) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT AVG(rating) as avg_rating FROM ratings WHERE movie_id = ?")) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
            return 0.0;
        }
    }

    public Map<String, Object> getMovieStatistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = DbConfig.getMariaDB();
             Statement stmt = conn.createStatement()) {

            // Total de filmes
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM movies");
            if (rs.next()) stats.put("totalMovies", rs.getInt("total"));

            // Filmes por categoria
            rs = stmt.executeQuery(
                    "SELECT category, COUNT(*) as count FROM movies " +
                            "WHERE category IS NOT NULL GROUP BY category ORDER BY count DESC");

            List<Map<String, Object>> byCategory = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("category", rs.getString("category"));
                item.put("count", rs.getInt("count"));
                byCategory.add(item);
            }
            stats.put("moviesByCategory", byCategory);

            // Filmes por ano
            rs = stmt.executeQuery(
                    "SELECT year, COUNT(*) as count FROM movies " +
                            "WHERE year IS NOT NULL GROUP BY year ORDER BY year DESC");

            List<Map<String, Object>> byYear = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("year", rs.getInt("year"));
                item.put("count", rs.getInt("count"));
                byYear.add(item);
            }
            stats.put("moviesByYear", byYear);

            // Total de visualizações
            rs = stmt.executeQuery("SELECT SUM(views) as total_views FROM watch_history");
            if (rs.next()) stats.put("totalViews", rs.getInt("total_views"));

        }

        return stats;
    }

    // Métodos privados auxiliares
    private List<Map<String, Object>> getMoviesWithQuery(String query, Object... params)
            throws SQLException {

        List<Map<String, Object>> movies = new ArrayList<>();

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                movies.add(extractMovieFromResultSet(rs));
            }
        }

        return movies;
    }

    private Map<String, Object> extractMovieFromResultSet(ResultSet rs) throws SQLException {
        Map<String, Object> movie = new HashMap<>();
        movie.put("id", rs.getInt("id"));
        movie.put("title", rs.getString("title"));
        movie.put("description", rs.getString("description"));
        movie.put("category", rs.getString("category"));
        movie.put("genre", rs.getString("genre"));
        movie.put("year", rs.getInt("year"));
        movie.put("duration", rs.getInt("duration"));
        movie.put("filePath1080", rs.getString("file_path_1080"));
        movie.put("filePath360", rs.getString("file_path_360"));
        movie.put("createdAt", rs.getTimestamp("created_at"));
        return movie;
    }
}