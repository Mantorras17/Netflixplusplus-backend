package org.netflixpp.controller;

import com.google.firebase.auth.FirebaseToken;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.netflixpp.service.AuthService;
import org.netflixpp.service.UserService;
import org.netflixpp.util.FirebaseUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();

    // Utilitário: resolve user interno a partir do header Authorization (Firebase)
    private Map<String, Object> resolveUserFromAuth(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        FirebaseToken decoded = FirebaseUtil.verifyIdToken(authHeader);
        if (decoded == null) {
            return null;
        }

        String firebaseUid = decoded.getUid();
        return authService.getUserByFirebaseUid(firebaseUid);
    }

    @GET
    @Path("/me")
    public Response getProfile(@HeaderParam("Authorization") String authHeader) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            return Response.ok(user).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/profile")
    public Response updateProfile(
            @HeaderParam("Authorization") String authHeader,
            Map<String, Object> profileData) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String username = (String) user.get("username");
            boolean success = userService.updateUserProfile(username, profileData);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Profile updated successfully"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Failed to update profile"))
                        .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/favorites")
    public Response getFavorites(@HeaderParam("Authorization") String authHeader) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String username = (String) user.get("username");
            Map<String, Object> result = userService.getFavorites(username);

            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/favorites/{movieId}")
    public Response addFavorite(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("movieId") int movieId) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String username = (String) user.get("username");
            boolean success = userService.addFavorite(username, movieId);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Added to favorites"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Failed to add favorite"))
                        .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/favorites/{movieId}")
    public Response removeFavorite(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("movieId") int movieId) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String username = (String) user.get("username");
            boolean success = userService.removeFavorite(username, movieId);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Removed from favorites"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Failed to remove favorite"))
                        .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/recommendations")
    public Response getRecommendations(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("limit") @DefaultValue("10") int limit) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String username = (String) user.get("username");
            Map<String, Object> result = userService.getRecommendations(username, limit);

            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/activity")
    public Response getUserActivity(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("days") @DefaultValue("30") int days) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String username = (String) user.get("username");
            Map<String, Object> result = userService.getUserActivity(username, days);

            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/ratings/{movieId}")
    public Response rateMovie(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("movieId") int movieId,
            Map<String, Integer> ratingData) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String username = (String) user.get("username");

            Integer rating = ratingData.get("rating");
            if (rating == null || rating < 1 || rating > 5) {
                return Response.status(400)
                        .entity(Map.of("error", "Rating must be between 1 and 5"))
                        .build();
            }

            boolean success = userService.rateMovie(username, movieId, rating);
            if (success) {
                return Response.ok(Map.of(
                        "status", "Rating submitted"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Failed to submit rating"))
                        .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/account")
    public Response deleteAccount(@HeaderParam("Authorization") String authHeader) {
        try {
            Map<String, Object> user = resolveUserFromAuth(authHeader);
            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String username = (String) user.get("username");
            boolean success = userService.deleteAccount(username);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Account deleted successfully",
                        "message", "We're sorry to see you go"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Failed to delete account"))
                        .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
