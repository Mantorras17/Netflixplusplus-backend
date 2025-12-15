package org.netflixpp.service;

import org.netflixpp.config.Config;
import org.netflixpp.config.DbConfig;

import jakarta.ws.rs.core.Response;

import java.sql.*;
import java.util.*;

public class StreamService {

    private static final String STREAM_BASE_URL = Config.STREAM_BASE_URL;

    public Response getStreamManifest(String movieId) throws SQLException {
        Map<String, Object> manifest = new HashMap<>();

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, title, file_path_1080, file_path_360 " +
                             "FROM movies WHERE id = ?")) {

            stmt.setInt(1, Integer.parseInt(movieId));
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found"))
                        .build();
            }

            manifest.put("movieId", movieId);
            manifest.put("title", rs.getString("title"));

            List<Map<String, Object>> variants = new ArrayList<>();

            String fp1080 = rs.getString("file_path_1080");
            String fp360  = rs.getString("file_path_360");

            if (fp360 != null && !fp360.isBlank()) {
                variants.add(Map.of(
                        "quality", "360",
                        "type", "hls",
                        "url", buildHlsUrl(movieId, "360")
                ));
            }
            if (fp1080 != null && !fp1080.isBlank()) {
                variants.add(Map.of(
                        "quality", "1080",
                        "type", "hls",
                        "url", buildHlsUrl(movieId, "1080")
                ));
            }

            manifest.put("variants", variants);
        }

        return Response.ok(manifest).build();
    }

    public Response getAvailableFormats(String movieId) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT file_path_1080, file_path_360 FROM movies WHERE id = ?")) {

            stmt.setInt(1, Integer.parseInt(movieId));
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found"))
                        .build();
            }

            List<String> formats = new ArrayList<>();
            String fp1080 = rs.getString("file_path_1080");
            String fp360  = rs.getString("file_path_360");

            if (fp360 != null && !fp360.isBlank()) formats.add("360");
            if (fp1080 != null && !fp1080.isBlank()) formats.add("1080");

            result.put("movieId", movieId);
            result.put("formats", formats);
            result.put("type", "hls");
        }

        return Response.ok(result).build();
    }

    public Response getPlaybackUrl(String movieId, String quality) throws SQLException {
        // Validar se o filme existe
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id FROM movies WHERE id = ?")) {

            stmt.setInt(1, Integer.parseInt(movieId));
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found"))
                        .build();
            }
        }

        String url = buildHlsUrl(movieId, quality);

        Map<String, Object> body = new HashMap<>();
        body.put("movieId", movieId);
        body.put("quality", quality);
        body.put("type", "hls");
        body.put("url", url);

        return Response.ok(body).build();
    }

    private String buildHlsUrl(String movieId, String quality) {
        String res = quality.equals("360") ? "360p" : quality + "p";
        // Tem de bater com o padrão do HlsService.uploadOutputs:
        // "movies/movie_{id}/{res}/hls/index.m3u8"
        String path = String.format("/hls/movies/movie_%s/%s/hls/index.m3u8", movieId, res);

        return (STREAM_BASE_URL == null || STREAM_BASE_URL.isBlank())
                ? path
                : STREAM_BASE_URL + path;
    }
}
