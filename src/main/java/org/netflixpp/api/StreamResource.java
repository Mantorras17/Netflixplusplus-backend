package org.netflixpp.api;

import org.netflixpp.service.MovieService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/stream")
public class StreamResource {

    private MovieService movieService = new MovieService();

    @GET
    @Path("/movie/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStreamInfo(@PathParam("id") int movieId) {
        return movieService.getMovieById(movieId)
                .map(movie -> {
                    String streamInfo = "{" +
                            "\"movieId\": " + movie.getId() + "," +
                            "\"title\": \"" + movie.getTitle() + "\"," +
                            "\"qualities\": [" +
                            "  {\"quality\": \"360p\", \"url\": \"/stream/movie/" + movieId + "/360p\"}," +
                            "  {\"quality\": \"1080p\", \"url\": \"/stream/movie/" + movieId + "/1080p\"}" +
                            "]" +
                            "}";
                    return Response.ok(streamInfo).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Movie not found\"}")
                        .build());
    }

    @GET
    @Path("/movie/{id}/{quality}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStreamUrl(@PathParam("id") int movieId,
                                 @PathParam("quality") String quality) {
        // Simulação - depois implementa com links reais e mesh P2P
        String streamUrl = "{" +
                "\"movieId\": " + movieId + "," +
                "\"quality\": \"" + quality + "\"," +
                "\"url\": \"http://localhost:8080/videos/movie" + movieId + "_" + quality + ".mp4\"," +
                "\"meshPeers\": [\"peer1:9090\", \"peer2:9090\"]," + // Simulação peers P2P
                "\"chunks\": [\"chunk1\", \"chunk2\", \"chunk3\"]" + // Simulação chunks
                "}";

        return Response.ok(streamUrl).build();
    }

    @GET
    @Path("/chunk/{chunkId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getChunk(@PathParam("chunkId") String chunkId) {
        // Simulação - depois implementa com sistema real de chunks
        byte[] chunkData = ("Chunk data for: " + chunkId).getBytes();
        return Response.ok(chunkData)
                .header("Content-Type", "application/octet-stream")
                .build();
    }
}