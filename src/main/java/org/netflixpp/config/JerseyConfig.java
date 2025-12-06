package org.netflixpp.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.netflixpp.filter.CORSFilter;
import org.netflixpp.filter.JWTFilter;
import org.netflixpp.filter.LoggingFilter;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        // Register controllers
        register(org.netflixpp.controller.StreamController.class);
        register(org.netflixpp.controller.MeshController.class);

        // Register filters (ORDER MATTERS!)
        register(LoggingFilter.class);    // First - log all requests
        register(JWTFilter.class);        // Second - authentication
        register(CORSFilter.class);       // Third - CORS headers

        // JSON support
        register(org.glassfish.jersey.jackson.JacksonFeature.class);

        // Multipart support for file uploads
        register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    }
}