package org.netflixpp;

import org.netflixpp.config.Config;
import org.netflixpp.config.DbConfig;
import org.netflixpp.mesh.MeshServer;
import org.netflixpp.mesh.P2PServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.server.ResourceConfig;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Netflix++ Backend...");

        // Inicializar banco de dados
        System.out.println("Initializing database...");
        initializeDatabase();

        // Criar diretórios de storage
        createStorageDirectories();

        // Diagnóstico de configuração GCS
        logGcsDiagnostics();

        // Diagnóstico do FFmpeg
        logFfmpegDiagnostics();

        // Iniciar servidores Mesh
        System.out.println("Starting Mesh servers...");
        startMeshServers();

        // Iniciar servidor HTTP principal
        System.out.println("Starting HTTP server...");
        startHttpServer();

        System.out.println("Netflix++ Backend started successfully!");
        System.out.println("   HTTP API: http://localhost:" + Config.HTTP_PORT);
        System.out.println("   Mesh HTTP: http://localhost:" + Config.P2P_PORT);
        System.out.println("   Mesh TCP:  port " + (Config.P2P_PORT + 1));

        // Manter a thread principal rodando
        Thread.currentThread().join();
    }

    private static void initializeDatabase() {
        try (var conn = DbConfig.getMariaDB();
             var stmt = conn.createStatement()) {

            // Criar tabela de usuários
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "role VARCHAR(20) DEFAULT 'user'," +
                    "email VARCHAR(100)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Criar tabela de filmes
            stmt.execute("CREATE TABLE IF NOT EXISTS movies (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "title VARCHAR(200) NOT NULL," +
                    "description TEXT," +
                    "category VARCHAR(50)," +
                    "genre VARCHAR(50)," +
                    "year INT," +
                    "duration INT," +
                    "file_path_1080 VARCHAR(500)," +
                    "file_path_360 VARCHAR(500)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Criar tabela de chunks
            stmt.execute("CREATE TABLE IF NOT EXISTS chunks (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "movie_id INT NOT NULL," +
                    "chunk_index INT NOT NULL," +
                    "chunk_hash VARCHAR(64)," +
                    "chunk_size BIGINT," +
                    "peer_count INT DEFAULT 0," +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE)");

            // Criar tabela de histórico de visualizações (agregado por filme)
            // Compatível com o uso atual em StreamService.registerView (INSERT ... ON DUPLICATE KEY UPDATE)
            stmt.execute("CREATE TABLE IF NOT EXISTS watch_history (" +
                    "movie_id INT PRIMARY KEY," +
                    "views INT DEFAULT 0," +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE)");

            // Inserir admin padrão (se não existir)
            stmt.execute("INSERT IGNORE INTO users (username, password, role, email) " +
                    "VALUES ('admin', 'admin123', 'admin', 'admin@netflixpp.com')");

            // Inserir usuário de teste
            stmt.execute("INSERT IGNORE INTO users (username, password, role, email) " +
                    "VALUES ('user', 'user123', 'user', 'user@netflixpp.com')");

            System.out.println("Database initialized");

        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            System.err.println("   Server will continue running but database features may not work");
            System.err.println("   Start MariaDB with: docker-compose up -d mariadb cassandra");
        }
    }

    private static void createStorageDirectories() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(Config.STORAGE_PATH));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(Config.MOVIES_DIR));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(Config.CHUNKS_DIR));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(Config.TEMP_DIR));
            System.out.println("Storage directories created");
        } catch (Exception e) {
            System.err.println("Failed to create storage directories: " + e.getMessage());
        }
    }

    private static void startMeshServers() {
        // Iniciar Mesh HTTP Server em thread separada
        Thread meshHttpThread = new Thread(() -> {
            try {
                MeshServer meshServer = new MeshServer();
                meshServer.start();
                System.out.println("Mesh HTTP Server started on port " + Config.P2P_PORT);
            } catch (Exception e) {
                System.err.println("Mesh HTTP Server failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
        meshHttpThread.setDaemon(true);
        meshHttpThread.start();

        // Iniciar P2P TCP Server em thread separada
        Thread p2pThread = new Thread(() -> {
            try {
                P2PServer p2pServer = new P2PServer();
                p2pServer.start();
                System.out.println("P2P TCP Server started on port " + (Config.P2P_PORT + 1));
            } catch (Exception e) {
                System.err.println("P2P TCP Server failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
        p2pThread.setDaemon(true);
        p2pThread.start();

        // Dar tempo para os servidores iniciarem
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void startHttpServer() {
        try {
            // Criar ResourceConfig do Jersey
            ResourceConfig config = new ResourceConfig();

            // Registrar todos os controllers
            config.register(org.netflixpp.controller.AuthController.class);
            config.register(org.netflixpp.controller.MovieController.class);
            config.register(org.netflixpp.controller.StreamController.class);
            config.register(org.netflixpp.controller.AdminController.class);
            config.register(org.netflixpp.controller.HlsController.class);
            config.register(org.netflixpp.controller.MeshController.class);
            config.register(org.netflixpp.controller.UserController.class);

            // Registrar filters
            config.register(org.netflixpp.filter.CORSFilter.class);
            config.register(org.netflixpp.filter.JWTFilter.class);
            config.register(org.netflixpp.filter.LoggingFilter.class);

            // Registrar features
            config.register(org.glassfish.jersey.jackson.JacksonFeature.class);
            config.register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);

            // Configurar Jetty Server
            Thread serverThread = getThread(config);
            serverThread.start();

            // Dar tempo para o servidor iniciar
            Thread.sleep(1000);

        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Thread getThread(ResourceConfig config) {
        Server server = new Server(Config.HTTP_PORT);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Configurar Jersey Servlet
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(config));
        jerseyServlet.setInitOrder(0);
        context.addServlet(jerseyServlet, "/api/*");

        // Iniciar servidor em thread separada
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
                System.out.println("HTTP Server started on port " + Config.HTTP_PORT);
                server.join();
            } catch (Exception e) {
                System.err.println("HTTP Server failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        return serverThread;
    }

    private static void logGcsDiagnostics() {
        try {
            if (Config.GCS_UPLOAD_ENABLED) {
                System.out.println("[GCS] Upload enabled");
                System.out.println("[GCS] Bucket: " + (Config.GCS_BUCKET_NAME == null ? "<null>" : Config.GCS_BUCKET_NAME));
                System.out.println("[GCS] Chunk path template: " + Config.GCS_CHUNK_PATH_TEMPLATE);
                String credPath = Config.GCS_CREDENTIALS_PATH;
                System.out.println("[GCS] Credentials path: " + credPath);
                if (credPath == null || credPath.isEmpty()) {
                    System.err.println("[GCS][WARN] GCS_CREDENTIALS_PATH is empty. App will try application default credentials.");
                } else {
                    File f = new File(credPath);
                    if (!f.exists()) {
                        System.err.println("[GCS][WARN] Credentials file not found at: " + credPath);
                    }
                }
                if (Config.GCS_BUCKET_NAME == null || Config.GCS_BUCKET_NAME.isEmpty()) {
                    System.err.println("[GCS][WARN] GCS_BUCKET_NAME is not set. Set env var or -DGCS_BUCKET_NAME.");
                }
            } else {
                System.out.println("[GCS] Upload disabled (GCS_UPLOAD_ENABLED=false)");
            }
        } catch (Exception e) {
            System.err.println("[GCS] Diagnostics failed: " + e.getMessage());
        }
    }

    private static void logFfmpegDiagnostics() {
        try {
            ProcessBuilder pb = new ProcessBuilder(Config.FFMPEG_PATH, "-version");
            Process p = pb.start();
            String firstLine = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                firstLine = br.readLine();
            }
            p.waitFor();
            if (p.exitValue() == 0) {
                System.out.println("[FFMPEG] Found: " + (firstLine == null ? "<no output>" : firstLine));
            } else {
                System.err.println("[FFMPEG][WARN] ffmpeg returned non-zero exit code. PATH='" + Config.FFMPEG_PATH + "'");
            }
        } catch (Exception e) {
            System.err.println("[FFMPEG][WARN] ffmpeg not found or not executable at PATH='" + Config.FFMPEG_PATH + "' : " + e.getMessage());
            System.err.println("            Configure via env var or -DFFMPEG_PATH (e.g., C:\\ffmpeg\\bin\\ffmpeg.exe)");
        }
    }
}