package org.netflixpp.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.netflixpp.service.AuthService;
import org.netflixpp.util.FirebaseUtil;
import com.google.firebase.auth.FirebaseToken;

import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    private final AuthService authService = new AuthService();

    /**
     * Endpoint usado pela app após login Firebase.
     * A app envia o ID token no header Authorization: Bearer <idToken>.
     * O backend valida o token e cria/retorna o utilizador interno associado ao firebaseUid.
     */
    @GET
    @Path("/me")
    public Response getCurrentUser(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Missing or invalid Authorization header"))
                        .build();
            }

            FirebaseToken decoded = FirebaseUtil.verifyIdToken(authHeader);
            if (decoded == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Invalid or expired Firebase ID token"))
                        .build();
            }

            String firebaseUid = decoded.getUid();
            String email = decoded.getEmail();
            String name = decoded.getName();

            // Cria ou atualiza o user interno com base no Firebase UID
            Map<String, Object> user = authService.getOrCreateUserFromFirebase(firebaseUid, email, name);

            if (user == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Failed to load or create user"))
                        .build();
            }

            return Response.ok(user).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Endpoint opcional para o CMS/web verificar se o token ainda é válido.
     * Útil para health-check de sessão do lado cliente.
     */
    @GET
    @Path("/validate-token")
    public Response validateToken(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("valid", false, "error", "No token provided"))
                        .build();
            }

            FirebaseToken decoded = FirebaseUtil.verifyIdToken(authHeader);
            if (decoded == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("valid", false, "error", "Token is invalid or expired"))
                        .build();
            }

            String firebaseUid = decoded.getUid();
            String email = decoded.getEmail();
            String name = decoded.getName();

            return Response.ok(Map.of(
                    "valid", true,
                    "firebaseUid", firebaseUid,
                    "email", email,
                    "name", name,
                    "message", "Token is valid"
            )).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("valid", false, "error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Logout passa a ser apenas semântico: o cliente descarta o token Firebase.
     * Não há blacklist de tokens aqui.
     */
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String authHeader) {
        // Em fluxo Firebase puro, o logout é feito no cliente (FirebaseAuth.signOut()).
        // Aqui podes só devolver sucesso.
        return Response.ok(Map.of(
                "status", "Logged out successfully",
                "message", "Token should be discarded client-side"
        )).build();
    }

    /**
     * Reset de password deve ser feito pelo próprio Firebase (sendPasswordResetEmail).
     * Se quiseres manter um proxy, podes expor algo aqui que chame a API do Firebase Admin
     * ou deixar o cliente falar direto com Firebase.
     */
    @POST
    @Path("/reset-password")
    public Response resetPassword(Map<String, Object> request) {
        // Recomendado: fazer direto na app via FirebaseAuth.sendPasswordResetEmail(email).
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(Map.of("error", "Password reset is handled by Firebase"))
                .build();
    }

    /**
     * Endpoints de login/register por username/password foram removidos
     * porque a autenticação é feita exclusivamente pelo Firebase.
     * Se ainda precisares deles para outra interface (ex.: web antiga),
     * mantém noutra controller separada.
     */
}
