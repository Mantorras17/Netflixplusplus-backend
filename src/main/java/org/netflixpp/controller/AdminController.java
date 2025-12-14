package org.netflixpp.controller;

import org.netflixpp.service.AdminService;
import org.netflixpp.service.GcsBackfillService;
import org.netflixpp.util.JWTUtil;
import org.glassfish.jersey.media.multipart.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.InputStream;
import java.util.Map;

@Path("/admin")
public class AdminController {

    private final AdminService adminService = new AdminService();
    private final GcsBackfillService gcsBackfillService = new GcsBackfillService();
    private final org.netflixpp.service.HlsService hlsService = new org.netflixpp.service.HlsService();

    private boolean isAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        try {
            String token = authHeader.substring(7);
            return "admin".equals(JWTUtil.getRole(token));
        } catch (Exception e) {
            return false;
        }
    }

    // ========== MOVIE MANAGEMENT ==========

    @POST
    @Path("/movies")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadMovie(
            @HeaderParam("Authorization") String auth,
            @FormDataParam("file") InputStream file,
            @FormDataParam("title") String title,
            @FormDataParam("description") String description,
            @FormDataParam("category") String category,
            @FormDataParam("genre") String genre,
            @FormDataParam("year") int year,
            @FormDataParam("duration") int duration) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> result = adminService.uploadMovie(
                    file, title, description, category, genre, year, duration);

            return Response.ok(result).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/movies/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMovie(
            @HeaderParam("Authorization") String auth,
            @PathParam("id") int id,
            Map<String, Object> updates) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            boolean success = adminService.updateMovie(id, updates);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Movie updated successfully",
                        "movieId", id
                )).build();
            } else {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found"))
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/movies/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMovie(
            @HeaderParam("Authorization") String auth,
            @PathParam("id") int id) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            boolean success = adminService.deleteMovie(id);

            if (success) {
                return Response.ok(Map.of(
                        "status", "Movie deleted successfully",
                        "movieId", id
                )).build();
            } else {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found"))
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/movies/{id}/chunks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateChunks(
            @HeaderParam("Authorization") String auth,
            @PathParam("id") int id) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> result = adminService.generateMovieChunks(id);
            return Response.ok(result).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    // ========== USER MANAGEMENT ==========

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(
            @HeaderParam("Authorization") String auth,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("50") int limit) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> users = adminService.getAllUsers(page, limit);
            return Response.ok(users).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/users/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(
            @HeaderParam("Authorization") String auth,
            @PathParam("id") int userId) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> user = adminService.getUserById(userId);

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
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(
            @HeaderParam("Authorization") String auth,
            Map<String, String> userData) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> result = adminService.createUser(userData);
            return Response.ok(result).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(
            @HeaderParam("Authorization") String auth,
            @PathParam("id") int userId,
            Map<String, Object> updates) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            boolean success = adminService.updateUser(userId, updates);

            if (success) {
                return Response.ok(Map.of(
                        "status", "User updated successfully",
                        "userId", userId
                )).build();
            } else {
                return Response.status(404)
                        .entity(Map.of("error", "User not found"))
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/users/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(
            @HeaderParam("Authorization") String auth,
            @PathParam("id") int userId) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            boolean success = adminService.deleteUser(userId);

            if (success) {
                return Response.ok(Map.of(
                        "status", "User deleted successfully",
                        "userId", userId
                )).build();
            } else {
                return Response.status(404)
                        .entity(Map.of("error", "User not found"))
                        .build();
            }

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/users/{id}/reset-password")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetUserPassword(
            @HeaderParam("Authorization") String auth,
            @PathParam("id") int userId) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            String newPassword = adminService.resetUserPassword(userId);

            return Response.ok(Map.of(
                    "status", "Password reset successfully",
                    "userId", userId,
                    "newPassword", newPassword,
                    "message", "Share this password with the user"
            )).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    // ========== SYSTEM MANAGEMENT ==========

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemStats(@HeaderParam("Authorization") String auth) {
        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> stats = adminService.getSystemStatistics();
            return Response.ok(stats).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/storage")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStorageInfo(@HeaderParam("Authorization") String auth) {
        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> storageInfo = adminService.getStorageInfo();
            return Response.ok(storageInfo).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/logs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogs(
            @HeaderParam("Authorization") String auth,
            @QueryParam("type") @DefaultValue("system") String type,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> logs = adminService.getLogs(type, limit);
            return Response.ok(logs).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/cleanup")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cleanupSystem(@HeaderParam("Authorization") String auth) {
        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> result = adminService.cleanupSystem();
            return Response.ok(result).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    // ========== GCS MANAGEMENT ==========

    @POST
    @Path("/gcs/backfill")
    @Produces(MediaType.APPLICATION_JSON)
    public Response backfillGcs(
            @HeaderParam("Authorization") String auth,
            @QueryParam("movieId") String movieId,
            @QueryParam("resolution") String resolution) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> res = gcsBackfillService.backfill(movieId, resolution);
            return Response.ok(res).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    // ========== HLS MANAGEMENT ==========

    @POST
    @Path("/hls/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateHls(
            @HeaderParam("Authorization") String auth,
            @QueryParam("movieId") int movieId) {

        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> res = hlsService.generateAndUpload(movieId);
            return Response.ok(res).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck(@HeaderParam("Authorization") String auth) {
        if (!isAdmin(auth)) {
            return Response.status(403)
                    .entity(Map.of("error", "Forbidden: Admin access required"))
                    .build();
        }

        try {
            Map<String, Object> health = adminService.getSystemHealth();
            return Response.ok(health).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}