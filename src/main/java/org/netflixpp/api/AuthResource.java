package org.netflixpp.api;

import org.netflixpp.service.AuthService;
import org.netflixpp.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/auth")
public class AuthResource {

    private AuthService authService = new AuthService();
    private ObjectMapper mapper = new ObjectMapper();

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String credentials) {
        try {
            Map<String, String> creds = mapper.readValue(credentials, Map.class);
            String username = creds.get("username");
            String password = creds.get("password");

            if (authService.authenticate(username, password)) {
                return Response.ok("{\"token\": \"jwt-token-" + username + "\", \"user\": \"" + username + "\"}")
                        .build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Invalid credentials\"}")
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid request\"}")
                    .build();
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(String userData) {
        try {
            User newUser = mapper.readValue(userData, User.class);

            if (authService.register(newUser)) {
                return Response.ok("{\"message\": \"User created successfully\", \"username\": \"" + newUser.getUsername() + "\"}")
                        .build();
            } else {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\": \"Username already exists\"}")
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid user data\"}")
                    .build();
        }
    }
}