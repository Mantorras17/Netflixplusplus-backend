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
    public static final String TEMP_DIR   = STORAGE_PATH + "/temp";

    // Chunks P2P
    public static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10MB

    public static final String JWT_SECRET =
            System.getenv().getOrDefault("JWT_SECRET",
                    "netflixpp-super-secret-key-32-chars-long-2024!");

    public static final long JWT_EXPIRATION = 24 * 60 * 60 * 1000;

    // Helper para ler config de sysprop/env
    private static String getCfg(String key, String def) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isEmpty()) return sys;
        return System.getenv().getOrDefault(key, def);
    }

    // FFMPEG
    public static final String FFMPEG_PATH        = getCfg("FFMPEG_PATH", "ffmpeg");

    // HLS (HTTP Live Streaming)
    public static final String HLS_DIR            = STORAGE_PATH + "/hls";
    public static final int    HLS_SEGMENT_TIME   = Integer.parseInt(getCfg("HLS_SEGMENT_TIME", "10"));
    public static final String HLS_SEGMENT_PATTERN = getCfg("HLS_SEGMENT_PATTERN", "seg_%05d.ts");

    // Google Cloud Storage
    public static final boolean GCS_UPLOAD_ENABLED = Boolean.parseBoolean(
            getCfg("GCS_UPLOAD_ENABLED", "true"));

    // Bucket padrão
    public static final String GCS_BUCKET_NAME =
            getCfg("GCS_BUCKET_NAME", "netflixpp-2526");

    public static final String GCS_CREDENTIALS_PATH =
            getCfg("GCS_CREDENTIALS_PATH",
                    "C:\\Users\\User\\Downloads\\service-account.json");

    // Template do caminho dos CHUNKS no bucket
    // Tokens suportados: {movieId}, {fileName}, {resolution}
    public static final String GCS_CHUNK_PATH_TEMPLATE =
            getCfg("GCS_CHUNK_PATH_TEMPLATE", "movies/{movieId}/{resolution}/{fileName}");

    // Firebase
    public static final boolean FIREBASE_ENABLED = Boolean.parseBoolean(
            getCfg("FIREBASE_ENABLED", "false"));

    public static final String FIREBASE_CREDENTIALS_PATH = getCfg(
            "FIREBASE_CREDENTIALS_PATH",
            getCfg("GCS_CREDENTIALS_PATH", ""));

    // Base URL opcional para servir HLS (usada pelo StreamService).
    // Em dev, normalmente deixas vazia e usas path relativo.
    public static final String STREAM_BASE_URL =
            getCfg("STREAM_BASE_URL", ""); // ex: "https://api.netflixpp.com" em prod

    static {
        // Criar diretórios necessários
        new File(MOVIES_DIR).mkdirs();
        new File(CHUNKS_DIR).mkdirs();
        new File(TEMP_DIR).mkdirs();
        new File(HLS_DIR).mkdirs();
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
