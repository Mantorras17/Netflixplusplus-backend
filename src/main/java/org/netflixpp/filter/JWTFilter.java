package org.netflixpp.filter;

import org.netflixpp.util.JWTUtil;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = ctx.getUriInfo().getPath();

        System.out.println("🔍 [DEBUG] JWTFilter checking path: " + path);

        // Lista de endpoints PÚBLICOS que NÃO precisam de autenticação
        List<String> publicEndpoints = Arrays.asList(
                "auth/login",
                "auth/register",
                "mesh/"
        );

        // Verificar se o path atual é um endpoint público
        boolean isPublic = publicEndpoints.stream()
                .anyMatch(publicPath -> path.startsWith(publicPath));

        if (isPublic) {
            System.out.println("✅ [DEBUG] Allowing public endpoint: " + path);
            return; // Não requer autenticação
        }

        System.out.println("🔒 [DEBUG] Requiring auth for: " + path);

        // Extrair token apenas para endpoints protegidos
        String authHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ [DEBUG] Missing or invalid auth header");
            abort(ctx, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!JWTUtil.validateToken(token)) {
            System.out.println("❌ [DEBUG] Invalid or expired token");
            abort(ctx, "Invalid or expired token");
            return;
        }

        // Adicionar informações do usuário ao contexto
        ctx.setProperty("username", JWTUtil.getUsername(token));
        ctx.setProperty("role", JWTUtil.getRole(token));
        System.out.println("✅ [DEBUG] User authenticated: " + JWTUtil.getUsername(token));
    }

    private void abort(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"" + message + "\"}")
                .build());
    }
}