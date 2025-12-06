package org.netflixpp;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.netflixpp.config.NginxConfig;
import org.netflixpp.controller.*;
import org.netflixpp.filter.CORSFilter;
import org.netflixpp.filter.JWTFilter;
import org.netflixpp.filter.LoggingFilter;
import org.netflixpp.mesh.SeederServer;

public class Main {

    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Starting Netflix++ Backend Server...");

            // 1. Create storage directories
            createStorageDirectories();

            // 2. Start NGINX reverse proxy
            startNginx();

            // 3. Start Mesh P2P Server
            startMeshServer();

            // 4. Start Jetty HTTP Server with Jersey
            startJettyServer();

            System.out.println("Netflix++ Backend Server started successfully!");
            System.out.println("HTTP: http://localhost:80");
            System.out.println("Mesh: port 9001");

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void createStorageDirectories() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("storage/movies"));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("storage/chunks"));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("logs"));
            System.out.println("Storage directories created");
        } catch (Exception e) {
            System.err.println("Could not create directories: " + e.getMessage());
        }
    }

    private static void startNginx() throws Exception {
        System.out.println("Starting NGINX...");
        NginxConfig.setupNginx();
        NginxConfig.startNginx();
        Thread.sleep(2000);
        System.out.println("NGINX started on port 80");
    }

    private static void startMeshServer() {
        System.out.println("Starting Mesh P2P Server...");
        new Thread(() -> {
            try {
                SeederServer server = new SeederServer(9001);
                server.start();
            } catch (Exception e) {
                System.err.println("Mesh server failed: " + e.getMessage());
            }
        }).start();
        System.out.println("Mesh server started on port 9001");
    }

    private static void startJettyServer() throws Exception {
        System.out.println("Starting Jetty with Jersey...");
        
        // Criação do ResourceConfig para o Jersey
        ResourceConfig config = new ResourceConfig();

        // Register all API controllers
        config.register(AuthController.class);
        config.register(MovieController.class);
        config.register(UserController.class);
        config.register(AdminController.class);
        config.register(StreamController.class);
        config.register(MeshController.class);

        // Register filters
        config.register(CORSFilter.class);
        config.register(JWTFilter.class);
        config.register(LoggingFilter.class);

        // Inicializa o servidor Jetty
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));
        Server server = new Server(5000);
        ServletContextHandler contextHandler = new ServletContextHandler(server, "/");
        contextHandler.addServlet(servlet, "/*");

        // Start server
        server.start();
        System.out.println("Jetty started on port 5000");
        
        // Keep server running
        new Thread(() -> {
            try {
                server.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}

