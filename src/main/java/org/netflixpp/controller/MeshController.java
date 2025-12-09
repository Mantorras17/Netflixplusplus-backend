package org.netflixpp.controller;

import org.netflixpp.service.MeshService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;

@Path("/mesh")
public class MeshController {

    private final MeshService meshService = new MeshService();

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(Map.of("status", "healthy", "service", "mesh")).build();
    }

    @GET
    @Path("/chunks/{movieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChunks(@PathParam("movieId") String movieId) {
        try {
            Map<String, Object> chunks = meshService.getChunkInfo(movieId);
            return Response.ok(chunks).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPeers() {
        try {
            return Response.ok(meshService.getActivePeers()).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/peer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerPeer(Map<String, String> peerInfo) {
        try {
            meshService.registerPeer(
                    peerInfo.get("peerId"),
                    peerInfo.get("address"),
                    peerInfo.get("chunks")
            );
            return Response.ok(Map.of("status", "Peer registered")).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}