package org.netflixpp.controller;

import org.netflixpp.service.StreamService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/stream")
public class StreamController {
    private final StreamService streamService = new StreamService();

    @GET
    @Path("/{filename}")
    public Response streamFile(
            @PathParam("filename") String filename,
            @HeaderParam("Range") String rangeHeader) {
        return streamService.streamFile(filename, rangeHeader);
    }

    @GET
    @Path("/movie/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMovieInfo(@PathParam("movieId") String movieId) {
        return streamService.getMovieInfo(movieId);
    }

    @GET
    @Path("/movies")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listMovies() {
        return streamService.listAvailableMovies();
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok("{\"status\":\"healthy\"}").build();
    }
}