package org.netflixpp.controller;

import org.netflixpp.service.StreamService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;

@Path("/stream")
public class StreamController {

    private final StreamService streamService = new StreamService();

    // NÃO serve mais MP4 direto; só devolve info de stream/HLS
    // /stream/movie/{id} pode ser removido ou só redirecionar para playback-url

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
}
