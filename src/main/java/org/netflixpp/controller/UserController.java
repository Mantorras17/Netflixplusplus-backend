package org.netflixpp.controller;

import org.netflixpp.service.UserService;
import org.netflixpp.util.JWTUtil;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
public class UserController {

    private final UserService userService = new UserService();

    private boolean isAdmin(String authHeader) {
        try {
            String role = JWTUtil.getRole(authHeader);
            return "admin".equals(role);
        } catch (Exception e) {
            return false;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(@HeaderParam("Authorization") String auth) {
        if (!isAdmin(auth)) return Response.status(403).entity("{\"error\":\"forbidden\"}").build();
        try {
            return Response.ok(userService.listUsers()).build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response me(@HeaderParam("Authorization") String auth) {
        try {
            String username = JWTUtil.getUsername(auth);
            var u = userService.getUserByUsername(username);
            if (u == null) return Response.status(404).entity("{\"error\":\"not_found\"}").build();
            return Response.ok(u).build();
        } catch (Exception e) {
            return Response.status(401).entity("{\"error\":\"invalid_token\"}").build();
        }
    }
}

