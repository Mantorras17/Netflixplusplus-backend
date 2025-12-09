package org.netflixpp.mesh;

import org.netflixpp.config.Config;
import org.netflixpp.util.HashUtil;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {
    private Map<String, List<ChunkInfo>> movieChunks;
    private Map<String, Set<String>> chunkPeers; // chunkId -> [peer addresses]

    public ChunkManager() {
        this.movieChunks = new ConcurrentHashMap<>();
        this.chunkPeers = new ConcurrentHashMap<>();
        loadExistingChunks();
    }

    private void loadExistingChunks() {
        try {
            Path chunksDir = Paths.get(Config.CHUNKS_DIR);
            if (!Files.exists(chunksDir)) {
                return;
            }

            try (DirectoryStream<Path> movieDirs = Files.newDirectoryStream(chunksDir)) {
                for (Path movieDir : movieDirs) {
                    if (Files.isDirectory(movieDir)) {
                        String movieId = movieDir.getFileName().toString();
                        List<ChunkInfo> chunks = new ArrayList<>();

                        try (DirectoryStream<Path> chunkFiles = Files.newDirectoryStream(movieDir, "*.bin")) {
                            for (Path chunkFile : chunkFiles) {
                                String fileName = chunkFile.getFileName().toString();
                                int chunkIndex = extractChunkIndex(fileName);
                                long size = Files.size(chunkFile);
                                String hash = HashUtil.calculateFileHash(chunkFile.toString());

                                chunks.add(new ChunkInfo(chunkIndex, hash, size, true));
                            }
                        }

                        // Ordenar por índice
                        chunks.sort(Comparator.comparingInt(ChunkInfo::getIndex));
                        movieChunks.put(movieId, chunks);
                    }
                }
            }

            System.out.println("Loaded chunks for " + movieChunks.size() + " movies");

        } catch (IOException e) {
            System.err.println("Error loading chunks: " + e.getMessage());
        }
    }

    public List<String> splitMovieIntoChunks(String movieFilePath, String movieId) throws IOException {
        Path moviePath = Paths.get(movieFilePath);
        Path chunksDir = Paths.get(Config.CHUNKS_DIR, movieId);
        Files.createDirectories(chunksDir);

        List<String> chunkFiles = new ArrayList<>();
        List<ChunkInfo> chunkInfos = new ArrayList<>();

        try (InputStream is = Files.newInputStream(moviePath)) {
            byte[] buffer = new byte[Config.CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = is.read(buffer)) != -1) {
                // Salvar chunk
                String chunkName = "chunk_" + chunkIndex + ".bin";
                Path chunkPath = chunksDir.resolve(chunkName);

                try (OutputStream os = Files.newOutputStream(chunkPath)) {
                    os.write(buffer, 0, bytesRead);
                }

                // Calcular hash
                String hash = HashUtil.calculateFileHash(chunkPath.toString());

                // Renomear com hash parcial
                String hashedName = "chunk_" + chunkIndex + "_" + hash.substring(0, 8) + ".bin";
                Path hashedPath = chunksDir.resolve(hashedName);
                Files.move(chunkPath, hashedPath);

                // Criar info do chunk
                ChunkInfo info = new ChunkInfo(chunkIndex, hash, bytesRead, true);
                chunkInfos.add(info);

                chunkFiles.add(hashedName);
                chunkIndex++;
            }
        }

        // Salvar no mapa
        movieChunks.put(movieId, chunkInfos);

        System.out.println("Split movie into " + chunkFiles.size() + " chunks");
        return chunkFiles;
    }

    public List<String> getAvailableChunks(String movieId) {
        List<ChunkInfo> chunks = movieChunks.get(movieId);
        if (chunks == null) {
            return new ArrayList<>();
        }

        List<String> chunkNames = new ArrayList<>();
        for (ChunkInfo chunk : chunks) {
            if (chunk.isAvailable()) {
                chunkNames.add("chunk_" + chunk.getIndex() + "_" +
                        chunk.getHash().substring(0, 8) + ".bin");
            }
        }

        return chunkNames;
    }

    public ChunkInfo getChunkInfo(String movieId, int chunkIndex) {
        List<ChunkInfo> chunks = movieChunks.get(movieId);
        if (chunks == null) return null;

        for (ChunkInfo chunk : chunks) {
            if (chunk.getIndex() == chunkIndex) {
                return chunk;
            }
        }

        return null;
    }

    public File getChunkFile(String movieId, int chunkIndex) {
        Path chunksDir = Paths.get(Config.CHUNKS_DIR, movieId);

        // Procurar arquivo pelo índice
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunksDir,
                "chunk_" + chunkIndex + "_*.bin")) {

            for (Path chunkPath : stream) {
                return chunkPath.toFile();
            }

        } catch (IOException e) {
            // Arquivo não encontrado
        }

        // Fallback: nome simples
        Path chunkPath = chunksDir.resolve("chunk_" + chunkIndex + ".bin");
        return chunkPath.toFile();
    }

    public void registerPeerForChunk(String chunkId, String peerAddress) {
        chunkPeers.computeIfAbsent(chunkId, k -> ConcurrentHashMap.newKeySet())
                .add(peerAddress);
    }

    public Set<String> getPeersForChunk(String chunkId) {
        return chunkPeers.getOrDefault(chunkId, Collections.emptySet());
    }

    public Map<String, Object> getMovieChunkInfo(String movieId) {
        Map<String, Object> info = new HashMap<>();
        info.put("movieId", movieId);

        List<ChunkInfo> chunks = movieChunks.get(movieId);
        if (chunks == null) {
            info.put("chunks", new ArrayList<>());
            info.put("count", 0);
            return info;
        }

        List<Map<String, Object>> chunkList = new ArrayList<>();
        for (ChunkInfo chunk : chunks) {
            Map<String, Object> chunkMap = new HashMap<>();
            chunkMap.put("index", chunk.getIndex());
            chunkMap.put("hash", chunk.getHash());
            chunkMap.put("size", chunk.getSize());
            chunkMap.put("available", chunk.isAvailable());

            String chunkId = movieId + "_" + chunk.getIndex();
            chunkMap.put("peers", getPeersForChunk(chunkId).size());

            chunkList.add(chunkMap);
        }

        info.put("chunks", chunkList);
        info.put("count", chunks.size());
        info.put("totalSize", chunks.stream().mapToLong(ChunkInfo::getSize).sum());

        return info;
    }

    private int extractChunkIndex(String fileName) {
        try {
            // Formato: chunk_X_hash.bin ou chunk_X.bin
            String[] parts = fileName.split("_");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    // Classe interna para informações do chunk
    public static class ChunkInfo {
        private int index;
        private String hash;
        private long size;
        private boolean available;

        public ChunkInfo(int index, String hash, long size, boolean available) {
            this.index = index;
            this.hash = hash;
            this.size = size;
            this.available = available;
        }

        public int getIndex() { return index; }
        public String getHash() { return hash; }
        public long getSize() { return size; }
        public boolean isAvailable() { return available; }

        public void setAvailable(boolean available) { this.available = available; }

        @Override
        public String toString() {
            return String.format("Chunk%d[%s] %d bytes", index,
                    hash.substring(0, 8), size);
        }
    }
}