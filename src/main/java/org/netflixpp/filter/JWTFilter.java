package org.netflixpp.filter;


import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class JWTFilter implements ContainerRequestFilter {

    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestContext)) {
            return;
        }

        // Get the Authorization header from the request
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Validate the Authorization header
        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortWithUnauthorized(requestContext, "Missing or invalid Authorization header");
            return;
        }

        // Extract the token from the Authorization header
        String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();

        try {
            // Validate the token (simplified - in production use proper JWT validation)
            validateToken(token);

        } catch (Exception e) {
            abortWithUnauthorized(requestContext, "Invalid token: " + e.getMessage());
        }
    }

    private boolean isPublicEndpoint(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();

        // Public endpoints (no authentication required)
        return path.startsWith("stream/") && "GET".equals(method) ||
                path.startsWith("mesh/chunks") && "GET".equals(method) ||
                path.startsWith("mesh/peers") && "GET".equals(method) ||
                path.endsWith("health") && "GET".equals(method);
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return authorizationHeader != null &&
                authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    private void validateToken(String token) throws Exception {
        // Simplified token validation
        // In production, use proper JWT validation with secret key
        if (token == null || token.isEmpty()) {
            throw new Exception("Empty token");
        }

        // Basic token format check (you should use a JWT library)
        if (token.length() < 10) {
            throw new Exception("Token too short");
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\": \"" + message + "\"}")
                .build());
    }
}