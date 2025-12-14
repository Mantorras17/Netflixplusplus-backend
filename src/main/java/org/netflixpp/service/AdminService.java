package org.netflixpp.service;

import org.netflixpp.config.Config;
import org.netflixpp.config.DbConfig;
import org.netflixpp.mesh.ChunkManager;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class AdminService {

    private final ChunkManager chunkManager = new ChunkManager();

    // ========== MOVIE MANAGEMENT ==========

    public Map<String, Object> uploadMovie(InputStream fileStream, String title,
                                           String description, String category, String genre, int year, int duration)
            throws Exception {

        Map<String, Object> result = new HashMap<>();

        // Salvar arquivo 1080p
        String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "_");
        String fileName1080 = safeTitle + "_1080p.mp4";
        Path path1080 = Paths.get(Config.MOVIES_DIR, fileName1080);

        Files.copy(fileStream, path1080, StandardCopyOption.REPLACE_EXISTING);

        // Gerar versão 360p
        String fileName360 = safeTitle + "_360p.mp4";
        Path path360 = Paths.get(Config.MOVIES_DIR, fileName360);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    Config.FFMPEG_PATH, "-i", path1080.toString(),
                    "-vf", "scale=-2:360", "-c:v", "libx264", "-preset", "fast",
                    "-c:a", "aac", path360.toString());
            Process p = pb.start();
            p.waitFor();

            if (p.exitValue() != 0) {
                throw new RuntimeException("FFmpeg conversion failed");
            }

        } catch (Exception e) {
            // Fallback: copiar o mesmo arquivo
            Files.copy(path1080, path360, StandardCopyOption.REPLACE_EXISTING);
        }

        // Salvar no banco
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO movies (title, description, category, genre, year, " +
                             "duration, file_path_1080, file_path_360) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setString(3, category);
            stmt.setString(4, genre);
            stmt.setInt(5, year);
            stmt.setInt(6, duration);
            stmt.setString(7, path1080.toString());
            stmt.setString(8, path360.toString());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            int movieId = -1;
            if (rs.next()) {
                movieId = rs.getInt(1);
            }

            result.put("movieId", movieId);
            result.put("title", title);
            result.put("file1080", path1080.toString());
            result.put("file360", path360.toString());
            result.put("status", "uploaded");

            // Gerar chunks para P2P e enviar ao GCS para ambas resoluções
            // Usamos movieIds distintos localmente para evitar colisão de nomes de arquivos:
            //   movie_<id>_1080p e movie_<id>_360p
            String movieHash1080 = "movie_" + movieId + "_1080p";
            String movieHash360 = "movie_" + movieId + "_360p";

            List<String> chunks1080 = chunkManager.splitMovieIntoChunks(
                    path1080.toString(), movieHash1080);
            List<String> chunks360 = chunkManager.splitMovieIntoChunks(
                    path360.toString(), movieHash360);

            result.put("chunksGenerated1080", chunks1080.size());
            result.put("chunkIds1080", chunks1080);
            result.put("chunksGenerated360", chunks360.size());
            result.put("chunkIds360", chunks360);

        }

        return result;
    }

    public boolean updateMovie(int id, Map<String, Object> updates) throws SQLException {
        if (updates.isEmpty()) return false;

        StringBuilder sql = new StringBuilder("UPDATE movies SET ");
        List<Object> params = new ArrayList<>();

        if (updates.containsKey("title")) {
            sql.append("title = ?, ");
            params.add(updates.get("title"));
        }
        if (updates.containsKey("description")) {
            sql.append("description = ?, ");
            params.add(updates.get("description"));
        }
        if (updates.containsKey("category")) {
            sql.append("category = ?, ");
            params.add(updates.get("category"));
        }
        if (updates.containsKey("genre")) {
            sql.append("genre = ?, ");
            params.add(updates.get("genre"));
        }
        if (updates.containsKey("year")) {
            sql.append("year = ?, ");
            params.add(updates.get("year"));
        }
        if (updates.containsKey("duration")) {
            sql.append("duration = ?, ");
            params.add(updates.get("duration"));
        }

        if (params.isEmpty()) return false;

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

    public boolean deleteMovie(int id) throws Exception {
        try (Connection conn = DbConfig.getMariaDB()) {
            // Buscar caminhos dos arquivos
            String path1080 = null, path360 = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT file_path_1080, file_path_360 FROM movies WHERE id = ?")) {

                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    path1080 = rs.getString("file_path_1080");
                    path360 = rs.getString("file_path_360");
                }
            }

            // Deletar arquivos
            if (path1080 != null) Files.deleteIfExists(Paths.get(path1080));
            if (path360 != null) Files.deleteIfExists(Paths.get(path360));

            // Deletar chunks P2P
            String movieHash = "movie_" + id;
            Path chunksDir = Paths.get(Config.CHUNKS_DIR, movieHash);
            if (Files.exists(chunksDir)) {
                Files.walk(chunksDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            // Deletar do banco
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM movies WHERE id = ?")) {
                stmt.setInt(1, id);
                return stmt.executeUpdate() > 0;
            }
        }
    }

    public Map<String, Object> generateMovieChunks(int movieId) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT file_path_1080 FROM movies WHERE id = ?")) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new Exception("Movie not found");
            }

            String filePath = rs.getString("file_path_1080");
            if (filePath == null) {
                throw new Exception("Movie file not found");
            }

            String movieHash = "movie_" + movieId;
            List<String> chunks = chunkManager.splitMovieIntoChunks(filePath, movieHash);

            result.put("movieId", movieId);
            result.put("chunksGenerated", chunks.size());
            result.put("chunks", chunks);
            result.put("status", "success");

        }

        return result;
    }

    // ========== USER MANAGEMENT ==========

    public Map<String, Object> getAllUsers(int page, int limit) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        int offset = (page - 1) * limit;

        try (Connection conn = DbConfig.getMariaDB()) {
            // Total de usuários
            try (PreparedStatement countStmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM users")) {

                ResultSet rs = countStmt.executeQuery();
                if (rs.next()) {
                    result.put("total", rs.getInt("total"));
                }
            }

            // Lista de usuários
            List<Map<String, Object>> users = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, username, role, email, created_at FROM users " +
                            "ORDER BY created_at DESC LIMIT ? OFFSET ?")) {

                stmt.setInt(1, limit);
                stmt.setInt(2, offset);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("username", rs.getString("username"));
                    user.put("role", rs.getString("role"));
                    user.put("email", rs.getString("email"));
                    user.put("createdAt", rs.getTimestamp("created_at"));
                    users.add(user);
                }
            }

            result.put("users", users);
            result.put("page", page);
            result.put("limit", limit);
            result.put("pages", (int) Math.ceil((double) (int) result.get("total") / limit));
        }

        return result;
    }

    public Map<String, Object> getUserById(int userId) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, role, email, created_at FROM users WHERE id = ?")) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("username", rs.getString("username"));
                user.put("role", rs.getString("role"));
                user.put("email", rs.getString("email"));
                user.put("createdAt", rs.getTimestamp("created_at"));

                // Adicionar estatísticas do usuário
                addUserStatistics(userId, user);

                return user;
            }
        }
        return null;
    }

    public Map<String, Object> createUser(Map<String, String> userData) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        String username = userData.get("username");
        String password = userData.get("password");
        String email = userData.get("email");
        String role = userData.getOrDefault("role", "user");

        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password are required");
        }

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email != null ? email : "");
            stmt.setString(4, role);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            int userId = -1;
            if (rs.next()) {
                userId = rs.getInt(1);
            }

            result.put("userId", userId);
            result.put("username", username);
            result.put("email", email);
            result.put("role", role);
            result.put("status", "created");

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                throw new IllegalArgumentException("Username already exists");
            }
            throw e;
        }

        return result;
    }

    public boolean updateUser(int userId, Map<String, Object> updates) throws SQLException {
        if (updates.isEmpty()) return false;

        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> params = new ArrayList<>();

        if (updates.containsKey("username")) {
            sql.append("username = ?, ");
            params.add(updates.get("username"));
        }
        if (updates.containsKey("email")) {
            sql.append("email = ?, ");
            params.add(updates.get("email"));
        }
        if (updates.containsKey("role")) {
            sql.append("role = ?, ");
            params.add(updates.get("role"));
        }
        if (updates.containsKey("password")) {
            sql.append("password = ?, ");
            params.add(updates.get("password"));
        }

        if (params.isEmpty()) return false;

        // Remover última vírgula
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(userId);

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM users WHERE id = ?")) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public String resetUserPassword(int userId) throws SQLException {
        String newPassword = generateRandomPassword();

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE users SET password = ? WHERE id = ?")) {

            stmt.setString(1, newPassword);
            stmt.setInt(2, userId);

            if (stmt.executeUpdate() > 0) {
                return newPassword;
            } else {
                throw new SQLException("User not found");
            }
        }
    }

    // ========== SYSTEM MANAGEMENT ==========

    public Map<String, Object> getSystemStatistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = DbConfig.getMariaDB();
             Statement stmt = conn.createStatement()) {

            // Total de usuários
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM users");
            if (rs.next()) stats.put("totalUsers", rs.getInt("total"));

            // Usuários por role
            rs = stmt.executeQuery(
                    "SELECT role, COUNT(*) as count FROM users GROUP BY role");
            List<Map<String, Object>> usersByRole = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("role", rs.getString("role"));
                item.put("count", rs.getInt("count"));
                usersByRole.add(item);
            }
            stats.put("usersByRole", usersByRole);

            // Total de filmes
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM movies");
            if (rs.next()) stats.put("totalMovies", rs.getInt("total"));

            // Visualizações totais
            rs = stmt.executeQuery("SELECT SUM(views) as total FROM watch_history");
            if (rs.next()) stats.put("totalViews", rs.getInt("total"));

            // Armazenamento
            stats.put("storage", getStorageInfo());

            // Sistema
            stats.put("systemTime", new Date());
            stats.put("jvmMemory",
                    Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            stats.put("jvmMaxMemory", Runtime.getRuntime().maxMemory());

        }

        return stats;
    }

    public Map<String, Object> getStorageInfo() {
        Map<String, Object> storage = new HashMap<>();

        try {
            // Tamanho do diretório de filmes
            Path moviesDir = Paths.get(Config.MOVIES_DIR);
            long moviesSize = 0;
            if (Files.exists(moviesDir)) {
                moviesSize = Files.walk(moviesDir)
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try { return Files.size(p); }
                            catch (IOException e) { return 0; }
                        })
                        .sum();
            }

            // Tamanho do diretório de chunks
            Path chunksDir = Paths.get(Config.CHUNKS_DIR);
            long chunksSize = 0;
            if (Files.exists(chunksDir)) {
                chunksSize = Files.walk(chunksDir)
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try { return Files.size(p); }
                            catch (IOException e) { return 0; }
                        })
                        .sum();
            }

            storage.put("moviesSize", moviesSize);
            storage.put("chunksSize", chunksSize);
            storage.put("totalSize", moviesSize + chunksSize);
            storage.put("moviesCount", countFilesInDirectory(moviesDir));
            storage.put("chunksCount", countFilesInDirectory(chunksDir));

        } catch (Exception e) {
            storage.put("error", e.getMessage());
        }

        return storage;
    }

    public Map<String, Object> getLogs(String type, int limit) {
        Map<String, Object> logs = new HashMap<>();

        // Em produção, ler de arquivos de log
        // Por enquanto, retornar log simulado
        List<Map<String, Object>> logEntries = new ArrayList<>();

        for (int i = 0; i < Math.min(limit, 10); i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("timestamp", new Date(System.currentTimeMillis() - i * 60000));
            entry.put("level", i % 3 == 0 ? "ERROR" : i % 3 == 1 ? "WARN" : "INFO");
            entry.put("message", "Log entry " + (i + 1) + " for type: " + type);
            entry.put("source", "AdminService");
            logEntries.add(entry);
        }

        logs.put("type", type);
        logs.put("entries", logEntries);
        logs.put("count", logEntries.size());

        return logs;
    }

    public Map<String, Object> cleanupSystem() {
        Map<String, Object> result = new HashMap<>();
        List<String> cleaned = new ArrayList<>();

        try {
            // Limpar diretório temp
            Path tempDir = Paths.get(Config.TEMP_DIR);
            if (Files.exists(tempDir)) {
                long deleted = Files.walk(tempDir)
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            try {
                                return Files.getLastModifiedTime(p).toMillis() <
                                        System.currentTimeMillis() - 24 * 60 * 60 * 1000;
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .map(p -> {
                            try { Files.delete(p); return p.getFileName().toString(); }
                            catch (IOException e) { return null; }
                        })
                        .filter(Objects::nonNull)
                        .count();

                cleaned.add("temp files: " + deleted);
            }

            result.put("cleaned", cleaned);
            result.put("status", "success");

        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "healthy");
        health.put("timestamp", new Date());
        health.put("service", "Netflix++ Admin");
        health.put("version", "1.0.0");

        // Verificar banco de dados
        try (Connection conn = DbConfig.getMariaDB()) {
            health.put("database", "connected");
        } catch (Exception e) {
            health.put("database", "disconnected");
            health.put("databaseError", e.getMessage());
        }

        // Verificar storage
        try {
            Path storageDir = Paths.get(Config.STORAGE_PATH);
            health.put("storage", Files.exists(storageDir) ? "available" : "unavailable");
        } catch (Exception e) {
            health.put("storage", "error");
            health.put("storageError", e.getMessage());
        }

        return health;
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void addUserStatistics(int userId, Map<String, Object> user) throws SQLException {
        try (Connection conn = DbConfig.getMariaDB()) {
            // Total de visualizações
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as views FROM watch_history WHERE user_id = ?")) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    user.put("totalViews", rs.getInt("views"));
                }
            }

            // Última visualização
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT MAX(watched_at) as last_watched FROM watch_history WHERE user_id = ?")) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    user.put("lastWatched", rs.getTimestamp("last_watched"));
                }
            }
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random rnd = new Random();

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(rnd.nextInt(chars.length())));
        }

        return password.toString();
    }

    private long countFilesInDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return 0;

        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .count();
    }
}