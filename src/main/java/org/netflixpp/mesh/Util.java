package org.netflixpp.mesh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Util {

    public static List<byte[]> splitFileIntoChunks(File file, int chunkSize) throws IOException {
        List<byte[]> chunks = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    public static void createChunksForMovie(String movieId, File movieFile, ChunkManager chunkManager) throws IOException {
        List<byte[]> chunks = splitFileIntoChunks(movieFile, MeshConstants.CHUNK_SIZE);

        for (int i = 0; i < chunks.size(); i++) {
            chunkManager.addChunk(movieId, i, chunks.get(i));
        }

        System.out.println("✅ Created " + chunks.size() + " chunks for movie: " + movieId);
    }

    public static boolean isChunkedMovie(String movieId) {
        Path chunksDir = Paths.get("storage/chunks", movieId);
        return Files.exists(chunksDir) && chunksDir.toFile().list().length > 0;
    }
}