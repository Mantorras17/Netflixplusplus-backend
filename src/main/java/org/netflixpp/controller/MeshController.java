package org.netflixpp.controller;

import org.netflixpp.service.MeshService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/mesh")
public class MeshController {
    private final MeshService meshService = new MeshService();

    @GET
    @Path("/chunks/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChunkList(@PathParam("movieId") String movieId) {
        return meshService.getChunkList(movieId);
    }

    @GET
    @Path("/chunk/{movieId}/{chunkIndex}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getChunk(
            @PathParam("movieId") String movieId,
            @PathParam("chunkIndex") int chunkIndex) {
        return meshService.getChunk(movieId, chunkIndex);
    }

    @GET
    @Path("/peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActivePeers() {
        return meshService.getActivePeers();
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerPeer(String peerInfo) {
        return meshService.registerPeer(peerInfo);
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok("{\"status\":\"healthy\"}").build();
    }
}