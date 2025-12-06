package org.netflixpp.service;

import org.netflixpp.config.Config;
import java.io.File;
import java.nio.file.*;
import java.util.*;

public class MeshService {

    private final Map<String, List<String>> activePeers = new HashMap<>();

    public Map<String, Object> getChunkInfo(String movieId) {
        Map<String, Object> info = new HashMap<>();
        info.put("movieId", movieId);

        try {
            Path chunksDir = Paths.get(Config.CHUNKS_DIR, movieId);
            if (!Files.exists(chunksDir)) {
                info.put("chunks", new ArrayList<>());
                info.put("count", 0);
                return info;
            }

            List<String> chunks = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunksDir, "*.bin")) {
                for (Path chunk : stream) {
                    chunks.add(chunk.getFileName().toString());
                }
            }

            info.put("chunks", chunks);
            info.put("count", chunks.size());
            info.put("chunkSize", Config.CHUNK_SIZE);

        } catch (Exception e) {
            info.put("error", e.getMessage());
        }

        return info;
    }

    public Map<String, Object> getActivePeers() {
        Map<String, Object> response = new HashMap<>();
        response.put("peers", new ArrayList<>(activePeers.keySet()));
        response.put("count", activePeers.size());
        return response;
    }

    public void registerPeer(String peerId, String address, String chunks) {
        List<String> peerInfo = new ArrayList<>();
        peerInfo.add(address);
        if (chunks != null) {
            peerInfo.add(chunks);
        }
        activePeers.put(peerId, peerInfo);
    }

    public File getChunk(String movieId, int chunkIndex) {
        Path chunkPath = Paths.get(Config.CHUNKS_DIR, movieId, "chunk_" + chunkIndex + ".bin");
        return chunkPath.toFile();
    }
}