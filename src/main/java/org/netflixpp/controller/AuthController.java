package org.netflixpp.controller;

import org.netflixpp.service.AuthService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/auth")
public class AuthController {

    private final AuthService auth = new AuthService();

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String body) {
        try {
            Map<String, String> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, Map.class);
            String username = m.get("username");
            String password = m.get("password");
            String token = auth.login(username, password);
            if (token == null) {
                return Response.status(401).entity("{\"error\":\"invalid_credentials\"}").build();
            }
            return Response.ok(Map.of("token", token)).build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(String body) {
        try {
            Map<String, String> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, Map.class);
            boolean ok = auth.register(m.get("username"), m.get("password"), m.getOrDefault("role","user"), m.get("email"));
            if (!ok) return Response.status(400).entity("{\"error\":\"could_not_create\"}").build();
            return Response.ok("{\"status\":\"created\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }
}
