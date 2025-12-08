package org.netflixpp.mesh;

import org.netflixpp.config.Config;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class MeshServer {
    private HttpServer server;
    private ChunkManager chunkManager;
    private Map<String, List<String>> activePeers; // peerId -> [address, chunks]

    public MeshServer() {
        this.chunkManager = new ChunkManager();
        this.activePeers = new ConcurrentHashMap<>();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(Config.P2P_PORT), 0);

        // Endpoints do mesh
        server.createContext("/chunks", this::handleChunks);
        server.createContext("/peer", this::handlePeer);
        server.createContext("/health", this::handleHealth);
        server.createContext("/download", this::handleDownload);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("Mesh HTTP Server started on port " + Config.P2P_PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleChunks(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            if (parts.length < 3) {
                sendResponse(exchange, 400, "{\"error\":\"Missing movieId\"}");
                return;
            }

            String movieId = parts[2];
            List<String> chunks = chunkManager.getAvailableChunks(movieId);

            Map<String, Object> response = new HashMap<>();
            response.put("movieId", movieId);
            response.put("chunks", chunks);
            response.put("count", chunks.size());
            response.put("chunkSize", Config.CHUNK_SIZE);

            sendResponse(exchange, 200, toJson(response));

        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handlePeer(HttpExchange exchange) throws IOException {
        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Registrar peer
                String body = readBody(exchange);
                Map<String, String> data = parseJson(body);

                String peerId = data.get("peerId");
                String address = data.get("address");
                String chunks = data.get("chunks");

                List<String> peerInfo = new ArrayList<>();
                peerInfo.add(address);
                if (chunks != null) {
                    peerInfo.add(chunks);
                }

                activePeers.put(peerId, peerInfo);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "registered");
                response.put("peerId", peerId);
                response.put("totalPeers", activePeers.size());

                sendResponse(exchange, 200, toJson(response));

            } else if ("GET".equals(exchange.getRequestMethod())) {
                // Listar peers
                Map<String, Object> response = new HashMap<>();
                response.put("peers", new ArrayList<>(activePeers.keySet()));
                response.put("count", activePeers.size());

                sendResponse(exchange, 200, toJson(response));
            }

        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleDownload(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            String movieId = params.get("movieId");
            String chunkIndex = params.get("chunk");

            if (movieId == null || chunkIndex == null) {
                sendResponse(exchange, 400, "{\"error\":\"Missing parameters\"}");
                return;
            }

            Path chunkPath = Paths.get(Config.CHUNKS_DIR, movieId,
                    "chunk_" + chunkIndex + ".bin");

            if (!Files.exists(chunkPath)) {
                sendResponse(exchange, 404, "{\"error\":\"Chunk not found\"}");
                return;
            }

            // Enviar arquivo
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, Files.size(chunkPath));

            try (OutputStream os = exchange.getResponseBody();
                 InputStream is = Files.newInputStream(chunkPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, "{\"status\":\"healthy\",\"peers\":" + activePeers.size() + "}");
    }

    // Métodos auxiliares
    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        try {
            json = json.replace("{", "").replace("}", "").replace("\"", "");
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    map.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        } catch (Exception e) {
            // JSON simples
        }
        return map;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
        }
        return params;
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof List) {
                sb.append("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append("\"").append(list.get(i)).append("\"");
                }
                sb.append("]");
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }
}