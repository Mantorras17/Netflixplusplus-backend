package org.netflixpp.controller;

import org.netflixpp.service.AdminService;
import org.netflixpp.util.JWTUtil;
import org.glassfish.jersey.media.multipart.FormDataParam;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;

@Path("/admin")
public class AdminController {

    private final AdminService service = new AdminService();

    private boolean isAdmin(String token) {
        try {
            return "admin".equals(JWTUtil.getRole(token));
        } catch (Exception e) {
            return false;
        }
    }

    // ====== MOVIES CRUD ======

    @POST
    @Path("/movie")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMovie(
            @HeaderParam("Authorization") String token,
            @FormDataParam("file") InputStream file,
            @FormDataParam("title") String title,
            @FormDataParam("description") String description,
            @FormDataParam("category") String category,
            @FormDataParam("genre") String genre,
            @FormDataParam("year") int year,
            @FormDataParam("duration") int duration) {
        if (!isAdmin(token)) return Response.status(403).entity("{\"error\":\"forbidden\"}").build();
        try {
            service.createMovie(file, title, description, category, genre, year, duration);
            return Response.ok("{\"status\":\"created\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @PUT
    @Path("/movie/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMovie(@HeaderParam("Authorization") String token,
                                @PathParam("id") int id,
                                String body) {
        if (!isAdmin(token)) return Response.status(403).entity("{\"error\":\"forbidden\"}").build();
        try {
            Map<String,Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, new TypeReference<Map<String,Object>>(){});
            service.updateMovie(id,
                    (String)m.get("title"),
                    (String)m.get("description"),
                    (String)m.get("category"),
                    (String)m.get("genre"),
                    (int)m.get("year"),
                    (int)m.get("duration"));
            return Response.ok("{\"status\":\"updated\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @DELETE
    @Path("/movie/{id}")
    public Response deleteMovie(@HeaderParam("Authorization") String token, @PathParam("id") int id) {
        if (!isAdmin(token)) return Response.status(403).entity("{\"error\":\"forbidden\"}").build();
        try {
            service.deleteMovie(id);
            return Response.ok("{\"status\":\"deleted\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @GET
    @Path("/movies")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listMovies(@HeaderParam("Authorization") String token) {
        if (!isAdmin(token)) return Response.status(403).build();
        try {
            return Response.ok(service.listMovies()).build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    // ====== USERS CRUD ======

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(@HeaderParam("Authorization") String token) {
        if (!isAdmin(token)) return Response.status(403).entity("{\"error\":\"forbidden\"}").build();
        try {
            return Response.ok(service.listUsers()).build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(@HeaderParam("Authorization") String token, String body) {
        if (!isAdmin(token)) return Response.status(403).build();
        try {
            Map<String,Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, new TypeReference<Map<String,Object>>(){});
            service.createUser((String)m.get("username"), (String)m.get("password"), (String)m.get("role"), (String)m.get("email"));
            return Response.ok("{\"status\":\"created\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @PUT
    @Path("/user/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(@HeaderParam("Authorization") String token, @PathParam("id") int id, String body) {
        if (!isAdmin(token)) return Response.status(403).build();
        try {
            Map<String,Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, new TypeReference<Map<String,Object>>(){});
            service.updateUser(id, (String)m.get("username"), (String)m.get("password"), (String)m.get("role"), (String)m.get("email"));
            return Response.ok("{\"status\":\"updated\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @DELETE
    @Path("/user/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@HeaderParam("Authorization") String token, @PathParam("id") int id) {
        if (!isAdmin(token)) return Response.status(403).build();
        try {
            service.deleteUser(id);
            return Response.ok("{\"status\":\"deleted\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }
}
