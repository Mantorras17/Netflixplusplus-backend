package org.netflixpp.service;

import org.netflixpp.config.ServerConfig;
import org.netflixpp.mesh.ChunkManager;
import org.netflixpp.mesh.MeshConstants;

import jakarta.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshService {
    private final ChunkManager chunkManager = new ChunkManager();
    private final Map<String, String> activePeers = new HashMap<>(); // peerId -> address

    public Response getChunkList(String movieId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("movieId", movieId);

            // Get chunk information from ChunkManager
            List<Map<String, Object>> chunks = chunkManager.getChunkInfo(movieId);
            response.put("chunks", chunks);
            response.put("totalChunks", chunks.size());
            response.put("chunkSize", MeshConstants.CHUNK_SIZE);

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Error getting chunk list: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getChunk(String movieId, int chunkIndex) {
        try {
            Path chunkPath = Paths.get(ServerConfig.getStoragePath(), "chunks", movieId,
                    "chunk_" + chunkIndex + ".bin");
            File chunkFile = chunkPath.toFile();

            if (!chunkFile.exists()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Chunk not found\"}")
                        .build();
            }

            return Response.ok(chunkFile)
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"chunk_" + chunkIndex + ".bin\"")
                    .build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Error serving chunk: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getActivePeers() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("peers", new ArrayList<>(activePeers.values()));
            response.put("count", activePeers.size());
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Error getting peers: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response registerPeer(String peerInfo) {
        try {
            // Simple peer registration
            // In a real implementation, parse peerInfo JSON and store peer details
            String peerId = "peer_" + System.currentTimeMillis();
            activePeers.put(peerId, peerInfo);

            Map<String, Object> response = new HashMap<>();
            response.put("peerId", peerId);
            response.put("status", "registered");
            response.put("totalPeers", activePeers.size());

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Error registering peer: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}