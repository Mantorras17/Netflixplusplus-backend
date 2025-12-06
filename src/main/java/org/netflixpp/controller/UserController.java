package org.netflixpp.controller;

import org.netflixpp.service.UserService;
import org.netflixpp.util.JWTUtil;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;

@Path("/user")
public class UserController {

    private final UserService userService = new UserService();

    @GET
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProfile(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            Map<String, Object> profile = userService.getUserProfile(username);

            if (profile == null) {
                return Response.status(404)
                        .entity(Map.of("error", "User not found"))
                        .build();
            }

            return Response.ok(profile).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(
            @HeaderParam("Authorization") String authHeader,
            Map<String, String> updates) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            // Não permitir mudar username via este endpoint
            if (updates.containsKey("username")) {
                return Response.status(400)
                        .entity(Map.of("error", "Cannot change username"))
                        .build();
            }

            boolean success = userService.updateUserProfile(username, updates);

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
    @Path("/watch-history")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWatchHistory(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            Map<String, Object> history = userService.getWatchHistory(username, page, limit);
            return Response.ok(history).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/watch-history")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addToWatchHistory(
            @HeaderParam("Authorization") String authHeader,
            Map<String, Object> watchData) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            Integer movieId = (Integer) watchData.get("movieId");
            Integer progress = (Integer) watchData.get("progress");

            if (movieId == null) {
                return Response.status(400)
                        .entity(Map.of("error", "Movie ID is required"))
                        .build();
            }

            boolean success = userService.addToWatchHistory(username, movieId, progress);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Added to watch history"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Failed to add to watch history"))
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/watch-history/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFromWatchHistory(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("movieId") int movieId) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            boolean success = userService.removeFromWatchHistory(username, movieId);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Removed from watch history"
                )).build();
            } else {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found in watch history"))
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFavorites(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            return Response.ok(userService.getFavorites(username)).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/favorites/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFavorite(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("movieId") int movieId) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            boolean success = userService.addFavorite(username, movieId);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Added to favorites"
                )).build();
            } else {
                return Response.status(400)
                        .entity(Map.of("error", "Failed to add to favorites"))
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFavorite(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("movieId") int movieId) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            boolean success = userService.removeFavorite(username, movieId);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Removed from favorites"
                )).build();
            } else {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found in favorites"))
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecommendations(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("limit") @DefaultValue("10") int limit) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            return Response.ok(userService.getRecommendations(username, limit)).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/activity")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserActivity(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("days") @DefaultValue("7") int days) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

            return Response.ok(userService.getUserActivity(username, days)).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/rating/{movieId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rateMovie(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("movieId") int movieId,
            Map<String, Integer> ratingData) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAccount(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            String username = JWTUtil.getUsername(token);

            if (username == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid token"))
                        .build();
            }

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