package org.netflixpp.filter;

import com.google.firebase.auth.FirebaseToken;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.netflixpp.util.FirebaseUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = ctx.getUriInfo().getPath();
        System.out.println("🔍 [DEBUG] Auth filter checking path: " + path);

        // Endpoints PÚBLICOS (ajusta conforme tua API)
        List<String> publicEndpoints = Arrays.asList(
                "auth/validate-token",  // pode ser protegido se quiseres
                "auth/reset-password",
                "mesh/",
                "stream/health",
                "hls/public"
        );

        boolean isPublic = publicEndpoints.stream()
                .anyMatch(publicPath -> path.startsWith(publicPath));

        if (isPublic) {
            System.out.println("✅ [DEBUG] Allowing public endpoint: " + path);
            return;
        }

        System.out.println("🔒 [DEBUG] Requiring auth for: " + path);

        String authHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ [DEBUG] Missing or invalid auth header");
            abort(ctx, "Missing or invalid Authorization header");
            return;
        }

        FirebaseToken decoded = FirebaseUtil.verifyIdToken(authHeader);
        if (decoded == null) {
            System.out.println("❌ [DEBUG] Invalid or expired Firebase ID token");
            abort(ctx, "Invalid or expired token");
            return;
        }

        // Coloca dados do user no contexto para controllers/serviços
        ctx.setProperty("firebaseUid", decoded.getUid());
        ctx.setProperty("email", decoded.getEmail());
        ctx.setProperty("name", decoded.getName());

        System.out.println("✅ [DEBUG] User authenticated (uid): " + decoded.getUid());
    }

    private void abort(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"" + message + "\"}")
                .build());
    }
}
