package org.netflixpp.config;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NginxConfig {

    private static final String NGINX_CONF = """
        worker_processes 1;
        error_log logs/nginx_error.log;
        pid logs/nginx.pid;

        events {
            worker_connections 1024;
        }

        http {
            include mime.types;
            default_type application/octet-stream;
            sendfile on;
            keepalive_timeout 65;

            upstream backend_servers {
                server localhost:8080;
            }

            server {
                listen 80;
                server_name localhost;

                location /api/ {
                    proxy_pass http://backend_servers;
                    proxy_set_header Host $host;
                    proxy_set_header X-Real-IP $remote_addr;
                    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                    proxy_set_header X-Forwarded-Proto $scheme;
                }

                location /storage/ {
                    proxy_pass http://backend_servers;
                    proxy_buffering off;
                    proxy_http_version 1.1;
                    proxy_set_header Connection '';
                }

                location / {
                    return 302 /api/;
                }
            }
        }
        """;

    private static final String MIME_TYPES = """
        types {
            text/html html htm shtml;
            text/css css;
            text/xml xml;
            image/gif gif;
            image/jpeg jpeg jpg;
            application/javascript js;
            video/mp4 mp4 m4v;
            video/webm webm;
            video/ogg ogv;
            video/x-flv flv;
            video/x-msvideo avi;
        }
        """;

    // URL oficial (binário Windows). Podes trocar para Linux/macOS se quiseres.
    private static final String NGINX_URL = "https://nginx.org/download/nginx-1.26.0.zip";
    private static final Path NGINX_DIR = Paths.get("nginx");
    private static final Path NGINX_BIN = NGINX_DIR.resolve("nginx-1.26.0/nginx.exe");

    public static void setupNginx() throws IOException {
        Files.createDirectories(NGINX_DIR);
        Files.writeString(NGINX_DIR.resolve("nginx.conf"), NGINX_CONF);
        Files.writeString(NGINX_DIR.resolve("mime.types"), MIME_TYPES);
        System.out.println("NGINX configuration created");
    }

    public static void startNginx() throws IOException, InterruptedException {
        // Se não existir binário, baixa e extrai automaticamente
        if (!Files.exists(NGINX_BIN)) {
            System.out.println("NGINX not found locally — downloading...");
            downloadAndExtractNginx();
        }

        // Verifica se realmente foi baixado
        if (!Files.exists(NGINX_BIN)) {
            throw new IOException("Could not download NGINX automatically.");
        }

        System.out.println("Starting NGINX from local folder...");
        stopNginx(); // Para qualquer instância anterior

        ProcessBuilder pb = new ProcessBuilder(
                NGINX_BIN.toAbsolutePath().toString(),
                "-c", NGINX_DIR.resolve("nginx.conf").toAbsolutePath().toString(),
                "-p", NGINX_DIR.toAbsolutePath().toString()
        );
        pb.directory(NGINX_DIR.toFile());
        pb.redirectErrorStream(true);
        pb.start();

        Thread.sleep(2000);
        System.out.println("NGINX started on port 80 (local mode)");
    }

    private static void downloadAndExtractNginx() {
        try {
            Files.createDirectories(NGINX_DIR);
            Path zipPath = NGINX_DIR.resolve("nginx.zip");
            System.out.println("Downloading NGINX from: " + NGINX_URL);

            try (InputStream in = new URL(NGINX_URL).openStream()) {
                Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("Extracting NGINX...");
            unzip(zipPath, NGINX_DIR);

            System.out.println("NGINX downloaded and extracted to " + NGINX_DIR.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to auto-install NGINX: " + e.getMessage());
        }
    }

    private static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    public static void stopNginx() {
        try {
            new ProcessBuilder(
                    NGINX_BIN.toAbsolutePath().toString(),
                    "-s", "quit",
                    "-p", NGINX_DIR.toAbsolutePath().toString()
            ).start().waitFor();
            Thread.sleep(1000);
        } catch (Exception ignored) {
        }
    }
}
