package org.netflixpp.controller;

import org.netflixpp.service.AuthService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;

@Path("/auth")
public class AuthController {

    private final AuthService authService = new AuthService();

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            if (username == null || password == null) {
                return Response.status(400)
                        .entity(Map.of("error", "Username and password required"))
                        .build();
            }

            String token = authService.login(username, password);
            if (token == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid credentials"))
                        .build();
            }

            Map<String, Object> user = authService.getUserByToken(token);

            return Response.ok(Map.of(
                    "token", token,
                    "user", user
            )).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(Map<String, String> userData) {
        try {
            boolean success = authService.register(
                    userData.get("username"),
                    userData.get("password"),
                    userData.get("email")
            );

            if (success) {
                return Response.ok(Map.of(
                        "status", "User created successfully",
                        "message", "Please login with your credentials"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Username already exists"))
                        .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(
            @HeaderParam("Authorization") String authHeader,
            Map<String, String> passwords) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = org.netflixpp.util.JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            String oldPassword = passwords.get("oldPassword");
            String newPassword = passwords.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                return Response.status(400)
                        .entity(Map.of("error", "Old and new password required"))
                        .build();
            }

            boolean success = authService.changePassword(username, oldPassword, newPassword);

            if (success) {
                return Response.ok(Map.of("status", "Password changed successfully")).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Old password is incorrect"))
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/reset-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return Response.status(400)
                        .entity(Map.of("error", "Email required"))
                        .build();
            }

            boolean success = authService.resetPassword(email);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Password reset email sent",
                        "message", "Check your email for reset instructions"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Email not found"))
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            Map<String, Object> user = authService.getUserByToken(token);

            if (user == null) {
                return Response.status(404)
                        .entity(Map.of("error", "User not found"))
                        .build();
            }

            return Response.ok(user).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@HeaderParam("Authorization") String authHeader) {
        // Em produção: invalidar token no servidor
        // Por enquanto apenas resposta de sucesso
        return Response.ok(Map.of(
                "status", "Logged out successfully",
                "message", "Token should be discarded client-side"
        )).build();
    }

    @GET
    @Path("/validate-token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateToken(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("valid", false, "error", "No token provided"))
                        .build();
            }

            String token = authHeader.substring(7);
            boolean valid = org.netflixpp.util.JWTUtil.validateToken(token);

            if (valid) {
                String username = org.netflixpp.util.JWTUtil.getUsername(token);
                String role = org.netflixpp.util.JWTUtil.getRole(token);

                return Response.ok(Map.of(
                        "valid", true,
                        "username", username,
                        "role", role,
                        "message", "Token is valid"
                )).build();
            } else {
                return Response.status(401)
                        .entity(Map.of("valid", false, "error", "Token is invalid or expired"))
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("valid", false, "error", e.getMessage()))
                    .build();
        }
    }
}