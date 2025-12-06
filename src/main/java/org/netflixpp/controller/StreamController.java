package org.netflixpp.controller;

import org.netflixpp.service.StreamService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;

@Path("/stream")
public class StreamController {

    private final StreamService streamService = new StreamService();

    @GET
    @Path("/movie/{id}")
    @Produces("video/mp4")
    public Response streamMovie(
            @PathParam("id") int movieId,
            @QueryParam("quality") @DefaultValue("1080") String quality,
            @HeaderParam("Range") String rangeHeader) {

        return streamService.streamMovie(movieId, quality, rangeHeader);
    }

    @GET
    @Path("/chunk/{movieId}/{chunkId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getChunk(
            @PathParam("movieId") String movieId,
            @PathParam("chunkId") int chunkId) {

        try {
            return streamService.getChunk(movieId, chunkId);
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/chunks/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChunksInfo(@PathParam("movieId") String movieId) {
        try {
            return streamService.getChunksInfo(movieId);
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/manifest/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStreamManifest(@PathParam("movieId") String movieId) {
        try {
            return streamService.getStreamManifest(movieId);
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        return Response.ok(Map.of(
                "status", "healthy",
                "service", "stream",
                "timestamp", System.currentTimeMillis()
        )).build();
    }

    @GET
    @Path("/available-formats/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableFormats(@PathParam("movieId") String movieId) {
        try {
            return streamService.getAvailableFormats(movieId);
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/playback-url/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlaybackUrl(
            @PathParam("movieId") String movieId,
            @QueryParam("quality") @DefaultValue("1080") String quality) {

        try {
            return streamService.getPlaybackUrl(movieId, quality);
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}