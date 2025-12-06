package org.netflixpp.filter;

import org.netflixpp.util.JWTUtil;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTFilter implements ContainerRequestFilter {

    private static final String[] PUBLIC_PATHS = {
            "/auth/",
            "/movies",
            "/mesh/chunks/"
    };

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = ctx.getUriInfo().getPath();

        // Verificar se é endpoint público
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return;
            }
        }

        // Extrair token
        String authHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            abort(ctx, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!JWTUtil.validateToken(token)) {
            abort(ctx, "Invalid or expired token");
            return;
        }

        // Adicionar informações ao contexto
        ctx.setProperty("username", JWTUtil.getUsername(token));
        ctx.setProperty("role", JWTUtil.getRole(token));
    }

    private void abort(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"" + message + "\"}")
                .build());
    }
}