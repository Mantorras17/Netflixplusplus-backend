package org.netflixpp.controller;

import org.netflixpp.service.AuthService;
import org.netflixpp.service.MovieService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;
import java.util.stream.Collectors;

@Path("/movies")
public class MovieController {

    private final MovieService movieService = new MovieService();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMovies(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @QueryParam("sort") @DefaultValue("newest") String sort) {

        try {
            List<Map<String, Object>> movies;

            switch (sort) {
                case "title":
                    movies = movieService.getAllMovies().stream()
                            .sorted((a, b) -> ((String) a.get("title"))
                                    .compareToIgnoreCase((String) b.get("title")))
                            .collect(Collectors.toList());
                    break;
                case "year":
                    movies = movieService.getAllMovies().stream()
                            .sorted((a, b) -> ((Integer) b.get("year"))
                                    .compareTo((Integer) a.get("year")))
                            .collect(Collectors.toList());
                    break;
                case "views":
                    // Ordenar por visualizações (se houver)
                    movies = movieService.getAllMovies();
                    break;
                default: // newest
                    movies = movieService.getAllMovies();
            }

            // Paginação
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, movies.size());

            if (start >= movies.size()) {
                return Response.ok(new ArrayList<>()).build();
            }

            List<Map<String, Object>> paginated = movies.subList(start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("movies", paginated);
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", movies.size());
            response.put("pages", (int) Math.ceil((double) movies.size() / limit));

            return Response.ok(response).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/featured")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFeaturedMovies() {
        try {
            List<Map<String, Object>> movies = movieService.getFeaturedMovies();
            return Response.ok(movies).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/recent")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecentMovies(@QueryParam("limit") @DefaultValue("10") int limit) {
        try {
            List<Map<String, Object>> movies = movieService.getRecentMovies(limit);
            return Response.ok(movies).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMovie(@PathParam("id") int id) {
        try {
            Map<String, Object> movie = movieService.getMovieWithDetails(id);
            if (movie == null) {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found"))
                        .build();
            }
            return Response.ok(movie).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/stream-info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMovieStreamInfo(@PathParam("id") int id) {
        try {
            Map<String, Object> movie = movieService.getMovieById(id);
            if (movie == null) {
                return Response.status(404)
                        .entity(Map.of("error", "Movie not found"))
                        .build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("title", movie.get("title"));
            response.put("availableQualities", Arrays.asList("360p", "1080p"));
            response.put("duration", movie.get("duration"));
            response.put("hasChunks", movie.get("filePath1080") != null);

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMovies(@QueryParam("q") String query) {
        try {
            List<Map<String, Object>> results = movieService.searchMovies(query);
            return Response.ok(results).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/category/{category}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMoviesByCategory(@PathParam("category") String category) {
        try {
            List<Map<String, Object>> movies = movieService.getMoviesByCategory(category);
            return Response.ok(movies).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/genre/{genre}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMoviesByGenre(@PathParam("genre") String genre) {
        try {
            List<Map<String, Object>> movies = movieService.getMoviesByGenre(genre);
            return Response.ok(movies).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllCategories() {
        try {
            List<String> categories = movieService.getAllCategories();
            return Response.ok(categories).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/genres")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllGenres() {
        try {
            List<String> genres = movieService.getAllGenres();
            return Response.ok(genres).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics() {
        try {
            Map<String, Object> stats = movieService.getMovieStatistics();
            return Response.ok(stats).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/view")
    @Produces(MediaType.APPLICATION_JSON)
    public Response recordView(
            @PathParam("id") int movieId,
            @HeaderParam("Authorization") String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            String token = authHeader.substring(7);
            Map<String, Object> user = new AuthService().getUserByToken(token);

            if (user == null) {
                return Response.status(401)
                        .entity(Map.of("error", "Invalid user"))
                        .build();
            }

            int userId = (int) user.get("id");
            movieService.recordMovieView(movieId, userId);

            return Response.ok(Map.of("status", "View recorded")).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}