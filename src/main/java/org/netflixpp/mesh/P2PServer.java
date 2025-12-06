package org.netflixpp.mesh;

import org.netflixpp.config.Config;
import org.netflixpp.util.HashUtil;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class P2PServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running;
    private ChunkManager chunkManager;

    public P2PServer() {
        this.threadPool = Executors.newFixedThreadPool(20);
        this.chunkManager = new ChunkManager();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(Config.P2P_PORT + 1); // Porta diferente do HTTP
        running = true;

        System.out.println("🌀 P2P TCP Server started on port " + (Config.P2P_PORT + 1));

        while (running) {
            Socket clientSocket = serverSocket.accept();
            threadPool.submit(new PeerHandler(clientSocket));
        }
    }

    public void stop() throws IOException {
        running = false;
        threadPool.shutdown();
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    private class PeerHandler implements Runnable {
        private final Socket socket;

        public PeerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                // Ler comando
                String command = dis.readUTF();
                System.out.println("🌀 P2P Command: " + command);

                if ("HELLO".equals(command)) {
                    handleHello(dos);
                } else if (command.startsWith("GET_CHUNKS")) {
                    handleGetChunks(command, dos);
                } else if (command.startsWith("GET_CHUNK")) {
                    handleGetChunk(command, dis, dos);
                } else if ("PING".equals(command)) {
                    handlePing(dos);
                } else {
                    dos.writeUTF("ERROR:Unknown command");
                }

            } catch (Exception e) {
                System.err.println("🌀 P2P Error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        private void handleHello(DataOutputStream dos) throws IOException {
            dos.writeUTF("HELLO:NetflixP2P:" + Config.P2P_PORT);
        }

        private void handleGetChunks(String command, DataOutputStream dos) throws IOException {
            String[] parts = command.split(":");
            if (parts.length < 2) {
                dos.writeUTF("ERROR:Missing movieId");
                return;
            }

            String movieId = parts[1];
            List<String> chunks = chunkManager.getAvailableChunks(movieId);

            dos.writeUTF("CHUNKS:" + chunks.size());
            for (String chunk : chunks) {
                dos.writeUTF(chunk);
            }
        }

        private void handleGetChunk(String command, DataInputStream dis, DataOutputStream dos)
                throws IOException {
            String[] parts = command.split(":");
            if (parts.length < 3) {
                dos.writeUTF("ERROR:Missing parameters");
                return;
            }

            String movieId = parts[1];
            int chunkIndex = Integer.parseInt(parts[2]);

            Path chunkPath = Paths.get(Config.CHUNKS_DIR, movieId,
                    "chunk_" + chunkIndex + ".bin");

            if (!Files.exists(chunkPath)) {
                dos.writeUTF("ERROR:Chunk not found");
                return;
            }

            // Enviar informações do chunk
            long chunkSize = Files.size(chunkPath);
            String hash = HashUtil.calculateFileHash(chunkPath.toString());

            dos.writeUTF("CHUNK_INFO:" + chunkSize + ":" + hash);

            // Aguardar confirmação
            String ack = dis.readUTF();
            if (!"READY".equals(ack)) {
                return;
            }

            // Enviar dados do chunk
            try (InputStream is = Files.newInputStream(chunkPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalSent = 0;

                while (totalSent < chunkSize && (bytesRead = is.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                }
            }

            System.out.println("🌀 Sent chunk " + chunkIndex + " of movie " + movieId);
        }

        private void handlePing(DataOutputStream dos) throws IOException {
            dos.writeUTF("PONG");
        }
    }

    // Método para iniciar como thread separada
    public static void startInBackground() {
        new Thread(() -> {
            try {
                P2PServer server = new P2PServer();
                server.start();
            } catch (IOException e) {
                System.err.println("❌ P2P Server failed: " + e.getMessage());
            }
        }).start();
    }
}