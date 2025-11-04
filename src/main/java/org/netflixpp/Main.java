package org.netflixpp;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {
    public static void main(String[] args) {
        ResourceConfig config = new ResourceConfig();
        config.packages("org.netflixpp.api"); // Scan dos endpoints

        Server server = JettyHttpContainerFactory.createServer(
                URI.create("http://localhost:8080/"), config
        );

        try {
            System.out.println("🚀 Netflix++ Backend running on http://localhost:8080");
            server.start();
            server.join();
        } catch (Exception e) {
            System.err.println("❌ Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}