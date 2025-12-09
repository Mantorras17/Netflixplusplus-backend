package org.netflixpp.config;

import java.io.File;
import java.util.Properties;

public class Config {

    // Configurações do servidor
    public static final int HTTP_PORT = Integer.parseInt(
            System.getenv().getOrDefault("SERVER_PORT", "8080"));

    public static final int P2P_PORT = Integer.parseInt(
            System.getenv().getOrDefault("P2P_PORT", "9001"));

    // Storage
    public static final String STORAGE_PATH =
            System.getenv().getOrDefault("STORAGE_PATH", "./storage");

    public static final String MOVIES_DIR = STORAGE_PATH + "/movies";
    public static final String CHUNKS_DIR = STORAGE_PATH + "/chunks";
    public static final String TEMP_DIR = STORAGE_PATH + "/temp";

    // Chunks P2P
    public static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10MB

    public static final String JWT_SECRET =
            System.getenv().getOrDefault("JWT_SECRET",
                    "netflixpp-super-secret-key-32-chars-long-2024!");

    public static final long JWT_EXPIRATION = 24 * 60 * 60 * 1000;

    // FFMPEG
    public static final String FFMPEG_PATH = "ffmpeg";

    static {
        // Criar diretórios necessários
        new File(MOVIES_DIR).mkdirs();
        new File(CHUNKS_DIR).mkdirs();
        new File(TEMP_DIR).mkdirs();
        System.out.println("Storage directories created");
    }

    public static Properties getProperties() {
        Properties props = new Properties();
        props.setProperty("http.port", String.valueOf(HTTP_PORT));
        props.setProperty("p2p.port", String.valueOf(P2P_PORT));
        props.setProperty("chunk.size", String.valueOf(CHUNK_SIZE));
        return props;
    }
}