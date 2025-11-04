package org.netflixpp.service;

import org.netflixpp.config.CassandraConfig;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public class MeshService {

    private static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10 MB

    public void generateChunks(String title, String videoPath) throws Exception {
        Path input = Paths.get(videoPath);
        byte[] buffer = new byte[CHUNK_SIZE];
        int index = 0;

        Files.createDirectories(Paths.get("storage/chunks/" + title));
        try (InputStream in = Files.newInputStream(input)) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                Path chunkFile = Paths.get("storage/chunks/" + title + "/chunk_" + index + ".bin");
                try (OutputStream out = Files.newOutputStream(chunkFile)) {
                    out.write(buffer, 0, bytesRead);
                }
                String hash = sha256(chunkFile);
                CassandraConfig.getSession().execute(SimpleStatement.newInstance(
                        "INSERT INTO mesh_chunks (movie_title, chunk_index, sha256, path) VALUES (?, ?, ?, ?)",
                        title, index, hash, chunkFile.toString()
                ));
                index++;
            }
        }

        CassandraConfig.getSession().execute(SimpleStatement.newInstance(
                "INSERT INTO movie_chunks (movie_title, total_chunks) VALUES (?, ?)",
                title, index
        ));
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] data = Files.readAllBytes(path);
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
