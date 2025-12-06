package org.netflixpp.mesh;

import org.netflixpp.config.ServerConfig;
import org.netflixpp.util.HashUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkManager {
    private final Map<String, List<ChunkInfo>> movieChunks = new HashMap<>();

    public ChunkManager() {
        loadExistingChunks();
    }

    private void loadExistingChunks() {
        try {
            Path chunksDir = Paths.get(ServerConfig.getStoragePath(), "chunks");
            if (!Files.exists(chunksDir)) {
                return;
            }

            File[] movieDirs = chunksDir.toFile().listFiles(File::isDirectory);
            if (movieDirs != null) {
                for (File movieDir : movieDirs) {
                    String movieId = movieDir.getName();
                    List<ChunkInfo> chunks = new ArrayList<>();

                    File[] chunkFiles = movieDir.listFiles((dir, name) -> name.startsWith("chunk_") && name.endsWith(".bin"));
                    if (chunkFiles != null) {
                        for (File chunkFile : chunkFiles) {
                            // Extract chunk index from filename "chunk_X.bin"
                            String filename = chunkFile.getName();
                            int chunkIndex = Integer.parseInt(filename.substring(6, filename.length() - 4));

                            String hash = HashUtil.sha256(Files.readAllBytes(chunkFile.toPath()));
                            ChunkInfo chunkInfo = new ChunkInfo(movieId, chunkIndex, hash, chunkFile.length(), chunkFile.getAbsolutePath());
                            chunks.add(chunkInfo);
                        }
                    }

                    movieChunks.put(movieId, chunks);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading existing chunks: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getChunkInfo(String movieId) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (movieChunks.containsKey(movieId)) {
            for (ChunkInfo chunk : movieChunks.get(movieId)) {
                Map<String, Object> chunkMap = new HashMap<>();
                chunkMap.put("index", chunk.getChunkIndex());
                chunkMap.put("hash", chunk.getHash());
                chunkMap.put("size", chunk.getSize());
                chunkMap.put("available", true);
                result.add(chunkMap);
            }
        }

        return result;
    }

    public boolean chunkExists(String movieId, int chunkIndex) {
        if (!movieChunks.containsKey(movieId)) {
            return false;
        }

        return movieChunks.get(movieId).stream()
                .anyMatch(chunk -> chunk.getChunkIndex() == chunkIndex);
    }

    public File getChunkFile(String movieId, int chunkIndex) {
        if (!chunkExists(movieId, chunkIndex)) {
            return null;
        }

        ChunkInfo chunk = movieChunks.get(movieId).stream()
                .filter(c -> c.getChunkIndex() == chunkIndex)
                .findFirst()
                .orElse(null);

        return chunk != null ? new File(chunk.getFilePath()) : null;
    }

    public void addChunk(String movieId, int chunkIndex, byte[] data) throws IOException {
        Path movieDir = Paths.get(ServerConfig.getStoragePath(), "chunks", movieId);
        Files.createDirectories(movieDir);

        Path chunkPath = movieDir.resolve("chunk_" + chunkIndex + ".bin");
        Files.write(chunkPath, data);

        String hash = HashUtil.sha256(data);
        ChunkInfo chunkInfo = new ChunkInfo(movieId, chunkIndex, hash, data.length, chunkPath.toString());

        movieChunks.computeIfAbsent(movieId, k -> new ArrayList<>()).add(chunkInfo);

        System.out.println("✅ Added chunk: " + movieId + "[" + chunkIndex + "] - " + hash);
    }
}