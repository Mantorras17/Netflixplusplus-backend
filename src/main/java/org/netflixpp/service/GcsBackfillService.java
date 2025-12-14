package org.netflixpp.service;

import org.netflixpp.config.Config;
import org.netflixpp.util.GcsUploader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class GcsBackfillService {

    public Map<String, Object> backfill(String movieIdFilter, String resolution) {
        Map<String, Object> result = new HashMap<>();
        if (!Config.GCS_UPLOAD_ENABLED) {
            result.put("status", "disabled");
            result.put("message", "GCS upload is disabled (GCS_UPLOAD_ENABLED=false)");
            return result;
        }

        List<Map<String, Object>> uploads = new ArrayList<>();
        int skipped = 0;
        int uploaded = 0;
        int failed = 0;

        Path chunksRoot = Paths.get(Config.CHUNKS_DIR);
        if (!Files.exists(chunksRoot)) {
            result.put("status", "ok");
            result.put("message", "Chunks directory does not exist");
            result.put("uploaded", 0);
            result.put("skipped", 0);
            result.put("failed", 0);
            result.put("details", uploads);
            return result;
        }

        try {
            GcsUploader uploader = GcsUploader.getInstance();

            try (DirectoryStream<Path> movieDirs = Files.newDirectoryStream(chunksRoot)) {
                for (Path movieDir : movieDirs) {
                    if (!Files.isDirectory(movieDir)) continue;
                    String movieId = movieDir.getFileName().toString();
                    if (movieIdFilter != null && !movieIdFilter.isEmpty() && !movieIdFilter.equals(movieId)) {
                        continue;
                    }

                    try (DirectoryStream<Path> chunkFiles = Files.newDirectoryStream(movieDir, "*.bin")) {
                        for (Path chunk : chunkFiles) {
                            String fileName = chunk.getFileName().toString();
                            String objectName = renderGcsObjectName(movieId, fileName, resolution);

                            Map<String, Object> entry = new HashMap<>();
                            entry.put("movieId", movieId);
                            entry.put("file", fileName);
                            if (resolution != null && !resolution.isEmpty()) {
                                entry.put("resolution", resolution);
                            }

                            try {
                                boolean exists = uploader.exists(objectName);
                                if (exists) {
                                    skipped++;
                                    entry.put("action", "skipped");
                                } else {
                                    uploader.upload(chunk.toFile(), objectName, "application/octet-stream");
                                    uploaded++;
                                    entry.put("action", "uploaded");
                                }
                            } catch (IOException | RuntimeException e) {
                                failed++;
                                entry.put("action", "failed");
                                entry.put("error", e.getMessage());
                            }
                            uploads.add(entry);
                        }
                    }
                }
            }

            result.put("status", "ok");
            result.put("uploaded", uploaded);
            result.put("skipped", skipped);
            result.put("failed", failed);
            result.put("details", uploads);
            return result;

        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("uploaded", uploaded);
            result.put("skipped", skipped);
            result.put("failed", failed);
            result.put("details", uploads);
            return result;
        }
    }

    private static String renderGcsObjectName(String movieId, String fileName, String resolution) {
        String tpl = Config.GCS_CHUNK_PATH_TEMPLATE;
        String res = (resolution == null || resolution.isEmpty()) ? "unknown" : resolution;
        return tpl
                .replace("{movieId}", movieId)
                .replace("{fileName}", fileName)
                .replace("{resolution}", res);
    }
}
