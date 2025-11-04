package org.netflixpp.api;

import org.netflixpp.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/movies")
public class MovieResource {

    private MovieService movieService = new MovieService();
    private ObjectMapper mapper = new ObjectMapper();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMovies() {
        try {
            String moviesJson = mapper.writeValueAsString(movieService.getAllMovies());
            return Response.ok(moviesJson).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error retrieving movies\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMovie(@PathParam("id") int id) {
        return movieService.getMovieById(id)
                .map(movie -> {
                    try {
                        String movieJson = mapper.writeValueAsString(movie);
                        return Response.ok(movieJson).build();
                    } catch (Exception e) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Movie not found\"}")
                        .build());
    }
}