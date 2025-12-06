package org.netflixpp.service;

import org.netflixpp.config.ServerConfig;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class StreamService {

    public Response streamFile(String filename, String rangeHeader) {
        try {
            Path filePath = Paths.get(ServerConfig.getStoragePath(), "movies", filename);
            File file = filePath.toFile();

            if (!file.exists()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"File not found: " + filename + "\"}")
                        .build();
            }

            long fileLength = file.length();
            long start = 0;
            long end = fileLength - 1;

            // Parse Range header if present
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            }

            long contentLength = end - start + 1;

            final long startPos = start;
            final long contentLen = contentLength;

            // Create streaming output
            StreamingOutput stream = output -> {
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    raf.seek(startPos);

                    byte[] buffer = new byte[8192];
                    long remaining = contentLen;
                    int read;

                    while (remaining > 0 && (read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        output.write(buffer, 0, read);
                        remaining -= read;
                    }
                    output.flush();
                }
            };

            Response.ResponseBuilder responseBuilder = Response.ok(stream)
                    .header("Content-Type", "video/mp4")
                    .header("Content-Length", contentLength)
                    .header("Accept-Ranges", "bytes");

            if (rangeHeader != null) {
                responseBuilder.header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength)
                        .status(Response.Status.PARTIAL_CONTENT);
            }

            return responseBuilder.build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Streaming error: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getMovieInfo(String movieId) {
        try {
            Map<String, Object> response = new HashMap<>();
            Path moviePath = Paths.get(ServerConfig.getStoragePath(), "movies", movieId + ".mp4");
            File movieFile = moviePath.toFile();

            if (movieFile.exists()) {
                response.put("movieId", movieId);
                response.put("filename", movieId + ".mp4");
                response.put("size", movieFile.length());
                response.put("available", true);

                // Check if chunks exist
                Path chunksDir = Paths.get(ServerConfig.getStoragePath(), "chunks", movieId);
                if (Files.exists(chunksDir)) {
                    String[] chunks = chunksDir.toFile().list();
                    response.put("chunksAvailable", chunks != null ? chunks.length : 0);
                } else {
                    response.put("chunksAvailable", 0);
                }
            } else {
                response.put("available", false);
                response.put("error", "Movie not found");
            }

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Error getting movie info: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response listAvailableMovies() {
        try {
            Map<String, Object> response = new HashMap<>();
            Path moviesDir = Paths.get(ServerConfig.getStoragePath(), "movies");
            File[] movieFiles = moviesDir.toFile().listFiles((dir, name) ->
                    name.endsWith(".mp4") || name.endsWith(".m4v"));

            if (movieFiles != null) {
                String[] movies = new String[movieFiles.length];
                for (int i = 0; i < movieFiles.length; i++) {
                    movies[i] = movieFiles[i].getName();
                }
                response.put("movies", movies);
                response.put("count", movies.length);
            } else {
                response.put("movies", new String[0]);
                response.put("count", 0);
            }

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Error listing movies: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}