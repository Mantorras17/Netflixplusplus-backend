package org.netflixpp.service;

import org.netflixpp.config.Config;
import org.netflixpp.config.DbConfig;
import org.netflixpp.util.GcsUploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class HlsService {

    public Map<String, Object> generateAndUpload(int movieId) {
        Map<String, Object> result = new HashMap<>();
        result.put("movieId", movieId);

        try {
            // 1) Obter caminhos de arquivos do DB
            Map<String, String> files = getMovieFiles(movieId);
            String file1080 = files.get("1080");
            String file360 = files.get("360");
            if (file1080 == null && file360 == null) {
                return error(result, "Movie not found or file paths missing");
            }

            // 2) Gerar saídas locais HLS
            Path baseOut = Paths.get(Config.HLS_DIR, "movie_" + movieId);
            Files.createDirectories(baseOut);

            Map<String, Object> details = new LinkedHashMap<>();

            if (file1080 != null) {
                Map<String, Object> d1080 = processOne("1080p", file1080, baseOut.resolve("1080p"));
                details.put("1080p", d1080);
            }
            if (file360 != null) {
                Map<String, Object> d360 = processOne("360p", file360, baseOut.resolve("360p"));
                details.put("360p", d360);
            }

            result.put("status", "generated");
            result.put("details", details);

            // 3) Upload para GCS (se habilitado)
            if (Config.GCS_UPLOAD_ENABLED) {
                Map<String, Object> up = uploadOutputs(movieId, baseOut);
                result.put("gcsUpload", up);
            } else {
                result.put("gcsUpload", Map.of(
                        "status", "skipped",
                        "reason", "GCS_UPLOAD_ENABLED=false"
                ));
            }

            return result;

        } catch (Exception e) {
            return error(result, e.getMessage());
        }
    }

    private Map<String, Object> error(Map<String, Object> base, String message) {
        base.put("status", "error");
        base.put("error", message);
        return base;
    }

    private Map<String, Object> processOne(String resolution, String inputFile, Path outDir) throws IOException, InterruptedException {
        Files.createDirectories(outDir);
        String indexName = "index.m3u8";
        Path indexPath = outDir.resolve(indexName);
        Path segmentPattern = outDir.resolve(Config.HLS_SEGMENT_PATTERN);

        // Montar comando ffmpeg
        List<String> cmd = new ArrayList<>();
        cmd.add(Config.FFMPEG_PATH);
        cmd.add("-i");
        cmd.add(inputFile);
        cmd.add("-codec:");
        cmd.add("copy");
        cmd.add("-start_number");
        cmd.add("0");
        cmd.add("-hls_time");
        cmd.add(String.valueOf(Config.HLS_SEGMENT_TIME));
        cmd.add("-hls_list_size");
        cmd.add("0");
        cmd.add("-hls_segment_filename");
        cmd.add(segmentPattern.toString());
        cmd.add("-f");
        cmd.add("hls");
        cmd.add(indexPath.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // Capturar algumas linhas de log para depuração
        List<String> ffmpegLog = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            int lines = 0;
            while ((line = br.readLine()) != null && lines < 2000) {
                ffmpegLog.add(line);
                lines++;
            }
        }
        int exit = p.waitFor();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("resolution", resolution);
        info.put("input", inputFile);
        info.put("outputDir", outDir.toString());
        info.put("exitCode", exit);

        if (exit != 0 || !Files.exists(indexPath)) {
            info.put("status", "failed");
            info.put("ffmpegLogTail", tail(ffmpegLog, 50));
        } else {
            // Contagem de arquivos gerados
            long m3u8 = Files.list(outDir).filter(pth -> pth.getFileName().toString().endsWith(".m3u8")).count();
            long ts = Files.list(outDir).filter(pth -> pth.getFileName().toString().endsWith(".ts")).count();
            info.put("status", "ok");
            info.put("m3u8", m3u8);
            info.put("ts", ts);
        }
        return info;
    }

    private Map<String, Object> uploadOutputs(int movieId, Path baseOut) {
        Map<String, Object> upload = new LinkedHashMap<>();
        int uploaded = 0, skipped = 0, failed = 0;
        List<Map<String, Object>> files = new ArrayList<>();
        try {
            GcsUploader up = GcsUploader.getInstance();

            // Percorrer 1080p e 360p se existirem
            for (String res : new String[]{"1080p", "360p"}) {
                Path dir = baseOut.resolve(res);
                if (!Files.exists(dir)) continue;
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                    for (Path f : stream) {
                        if (!Files.isRegularFile(f)) continue;
                        String name = f.getFileName().toString();
                        String ct = name.endsWith(".m3u8") ? "application/vnd.apple.mpegurl" :
                                (name.endsWith(".ts") ? "video/mp2t" : "application/octet-stream");

                        String objectName = "movies/movie_" + movieId + "/" + res + "/hls/" + name;
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("file", name);
                        entry.put("resolution", res);
                        entry.put("object", objectName);
                        try {
                            boolean exists = up.exists(objectName);
                            if (exists) {
                                skipped++;
                                entry.put("action", "skipped");
                            } else {
                                up.upload(f.toFile(), objectName, ct);
                                uploaded++;
                                entry.put("action", "uploaded");
                            }
                        } catch (IOException | RuntimeException e) {
                            failed++;
                            entry.put("action", "failed");
                            entry.put("error", e.getMessage());
                        }
                        files.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            upload.put("status", "error");
            upload.put("error", e.getMessage());
        }

        upload.put("status", "ok");
        upload.put("uploaded", uploaded);
        upload.put("skipped", skipped);
        upload.put("failed", failed);
        upload.put("files", files);
        return upload;
    }

    private Map<String, String> getMovieFiles(int movieId) throws Exception {
        Map<String, String> map = new HashMap<>();
        try (Connection conn = DbConfig.getMariaDB();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT file_path_1080, file_path_360 FROM movies WHERE id = ?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("1080", rs.getString("file_path_1080"));
                    map.put("360", rs.getString("file_path_360"));
                }
            }
        }
        return map;
    }

    private static List<String> tail(List<String> lines, int n) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();
        int from = Math.max(0, lines.size() - n);
        return lines.subList(from, lines.size());
    }
}
