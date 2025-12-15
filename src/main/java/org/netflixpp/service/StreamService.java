package org.netflixpp.service;

import org.netflixpp.config.Config;
import org.netflixpp.config.DbConfig;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.nio.channels.ClosedChannelException;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class StreamService {

    public Response streamMovie(int movieId, String quality, String rangeHeader) {
        try {
            // Buscar caminho do arquivo
            String filePath = getMovieFilePath(movieId, quality);
            if (filePath == null) {
                return Response.status(404).entity("Movie not found").build();
            }

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return Response.status(404).entity("File not found: " + filePath).build();
            }

            File file = path.toFile();
            long fileLength = file.length();
            long start = 0;
            long end = fileLength - 1;

            // Parse Range header (robusto)
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    String range = rangeHeader.substring(6);
                    String[] parts = range.split("-");
                    if (parts.length > 0 && !parts[0].isEmpty()) {
                        start = Math.max(0, Long.parseLong(parts[0]));
                    }
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        end = Long.parseLong(parts[1]);
                    }
                    if (end > fileLength - 1) end = fileLength - 1;
                    if (start > end) {
                        start = 0;
                        end = fileLength - 1;
                    }
                } catch (Exception ignore) {
                    // Cabeçalho inválido: servimos o arquivo completo (200 OK)
                    start = 0;
                    end = fileLength - 1;
                }
            }

            long contentLength = end - start + 1;
            final long finalStart = start;

            StreamingOutput stream = output -> {
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    raf.seek(finalStart);
                    byte[] buffer = new byte[8192];
                    long remaining = contentLength;

                    while (remaining > 0) {
                        int read = raf.read(buffer, 0,
                                (int) Math.min(buffer.length, remaining));
                        if (read == -1) break;
                        output.write(buffer, 0, read);
                        remaining -= read;
                    }
                    try { output.flush(); } catch (IOException ignored) {}
                } catch (org.eclipse.jetty.io.EofException eof) {
                    // Cliente encerrou a conexão (Jetty EoF): tratar como cenário esperado
                    return;
                } catch (ClosedChannelException cce) {
                    // Cliente encerrou a conexão: não tratar como erro
                } catch (IOException ioe) {
                    String msg = ioe.getMessage();
                    if (msg != null && (msg.contains("Broken pipe") || msg.contains("Connection reset"))) {
                        // Desconexão do cliente durante o streaming: esperado
                        return;
                    }
                    // Erro real: propagar para ser tratado pelo framework
                    throw ioe;
                }
            };

            Response.ResponseBuilder rb = Response.ok(stream)
                    .header("Content-Type", "video/mp4")
                    .header("Content-Length", contentLength)
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Disposition", "inline")
                    .header("Cache-Control", "public, max-age=3600");

            if (rangeHeader != null) {
                rb.header("Content-Range",
                                "bytes " + start + "-" + end + "/" + fileLength)
                        .status(206); // Partial Content
            }

            // Registrar visualização
            registerView(movieId);

            return rb.build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity("Streaming error: " + e.getMessage())
                    .build();
        }
    }

    public Response getChunk(String movieId, int chunkId) {
        try {
            Path chunkPath = Paths.get(Config.CHUNKS_DIR, movieId,
                    "chunk_" + chunkId + ".bin");

            if (!Files.exists(chunkPath)) {
                return Response.status(404)
                        .entity("Chunk not found")
                        .build();
            }

            long fileSize = Files.size(chunkPath);

            return Response.ok(chunkPath.toFile())
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Length", fileSize)
                    .header("Content-Disposition",
                            "attachment; filename=\"chunk_" + chunkId + ".bin\"")
                    .build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }

    public Response getChunksInfo(String movieId) {
        try {
            Path chunksDir = Paths.get(Config.CHUNKS_DIR, movieId);

            if (!Files.exists(chunksDir)) {
                return Response.ok(Map.of(
                        "movieId", movieId,
                        "chunks", new ArrayList<>(),
                        "count", 0,
                        "available", false
                )).build();
            }

            List<Map<String, Object>> chunks = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                    chunksDir, "chunk_*.bin")) {

                for (Path chunk : stream) {
                    String fileName = chunk.getFileName().toString();
                    long size = Files.size(chunk);

                    Map<String, Object> chunkInfo = new HashMap<>();
                    chunkInfo.put("filename", fileName);
                    chunkInfo.put("size", size);
                    chunkInfo.put("path", chunk.toString());

                    // Extrair índice do chunk do nome
                    try {
                        String[] parts = fileName.split("_");
                        if (parts.length >= 2) {
                            chunkInfo.put("index", Integer.parseInt(parts[1]));
                        }
                    } catch (Exception e) {
                        // Ignore
                    }

                    chunks.add(chunkInfo);
                }
            }

            // Ordenar por índice
            chunks.sort((a, b) -> {
                int idxA = (int) a.getOrDefault("index", 0);
                int idxB = (int) b.getOrDefault("index", 0);
                return Integer.compare(idxA, idxB);
            });

            Map<String, Object> response = new HashMap<>();
            response.put("movieId", movieId);
            response.put("chunks", chunks);
            response.put("count", chunks.size());
            response.put("totalSize", chunks.stream()
                    .mapToLong(c -> (long) c.get("size")).sum());
            response.put("available", !chunks.isEmpty());

            return Response.ok(response).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    public Response getStreamManifest(String movieId) {
        try {
            Map<String, Object> manifest = new HashMap<>();
            manifest.put("movieId", movieId);
            manifest.put("formats", Arrays.asList("360p", "1080p"));
            manifest.put("chunkSize", Config.CHUNK_SIZE);

            // Verificar se chunks existem
            Path chunksDir = Paths.get(Config.CHUNKS_DIR, movieId);
            if (Files.exists(chunksDir)) {
                long chunkCount = Files.list(chunksDir)
                        .filter(p -> p.getFileName().toString().endsWith(".bin"))
                        .count();
                manifest.put("chunksAvailable", chunkCount);
            } else {
                manifest.put("chunksAvailable", 0);
            }

            // URLs para streaming
            Map<String, String> urls = new HashMap<>();
            urls.put("direct", "/api/stream/movie/" + movieId);
            urls.put("chunked", "/api/stream/chunks/" + movieId);
            manifest.put("urls", urls);

            return Response.ok(manifest).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    public Response getAvailableFormats(String movieId) {
        try {
            List<Map<String, Object>> formats = new ArrayList<>();

            // Verificar 1080p
            Map<String, Object> format1080 = new HashMap<>();
            format1080.put("quality", "1080p");
            format1080.put("resolution", "1920x1080");
            format1080.put("bitrate", "5000kbps");
            format1080.put("codec", "H.264");

            // Verificar 360p
            Map<String, Object> format360 = new HashMap<>();
            format360.put("quality", "360p");
            format360.put("resolution", "640x360");
            format360.put("bitrate", "800kbps");
            format360.put("codec", "H.264");

            formats.add(format1080);
            formats.add(format360);

            return Response.ok(Map.of(
                    "movieId", movieId,
                    "formats", formats,
                    "recommended", "1080p"
            )).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    public Response getPlaybackUrl(String movieId, String quality) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("movieId", movieId);
            response.put("quality", quality);
            response.put("url", "/api/stream/movie/" + movieId + "?quality=" + quality);
            response.put("method", "GET");
            response.put("requiresAuth", true);
            response.put("supportsRange", true);

            return Response.ok(response).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private String getMovieFilePath(int movieId, String quality) throws SQLException {
        System.out.println("getMovieFilePath: id=" + movieId + " quality=" + quality);
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT file_path_1080, file_path_360 FROM movies WHERE id = ?")) {

            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String p1080 = rs.getString("file_path_1080");
                String p360  = rs.getString("file_path_360");
                System.out.println("DB paths: 1080=" + p1080 + " 360=" + p360);
                return "360".equals(quality) ? p360 : p1080;
            } else {
                System.out.println("No movie row for id=" + movieId);
            }
            return null;
        }
    }


    private void registerView(int movieId) {
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO watch_history (movie_id, views) VALUES (?, 1) " +
                             "ON DUPLICATE KEY UPDATE views = views + 1")) {

            stmt.setInt(1, movieId);
            stmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("Failed to register view: " + e.getMessage());
        }
    }
}