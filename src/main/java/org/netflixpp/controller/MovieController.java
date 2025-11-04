package org.netflixpp.controller;

import jakarta.ws.rs.Path;
import org.netflixpp.model.Movie;
import org.netflixpp.service.MovieService;
import org.netflixpp.util.JWTUtil;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;

@Path("/movies")
public class MovieController {

    private final MovieService movieService = new MovieService();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAll() {
        try {
            List<Movie> movies = movieService.listMovies();
            return Response.ok(movies).build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMovie(@PathParam("id") int id) {
        try {
            List<Movie> all = movieService.listMovies();
            for (Movie m : all) if (m.getId() == id) return Response.ok(m).build();
            return Response.status(404).entity("{\"error\":\"not_found\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }

    // stream: /movies/{id}/stream?res=1080 or res=360
    @GET
    @Path("/{id}/stream")
    public Response streamMovie(@PathParam("id") int id, @QueryParam("res") @DefaultValue("1080") String res,
                                @HeaderParam("Range") String range) {
        try {
            Movie movie = movieService.listMovies().stream().filter(m -> m.getId() == id).findFirst().orElse(null);
            if (movie == null) return Response.status(404).entity("{\"error\":\"not_found\"}").build();

            String path = "1080".equals(res) || "1080p".equals(res) ? movie.getFilePath1080() : movie.getFilePath360();
            Path file = (Path) Paths.get(path);
            if (!Files.exists((java.nio.file.Path) file)) return Response.status(404).entity("{\"error\":\"file_not_found\"}").build();

            long length = Files.size((java.nio.file.Path) file);
            long from = 0, to = length - 1;
            if (range != null && range.startsWith("bytes=")) {
                String[] parts = range.substring(6).split("-");
                from = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isEmpty()) to = Long.parseLong(parts[1]);
            }
            if (from < 0) from = 0;
            if (to >= length) to = length - 1;
            long contentLength = to - from + 1;

            final RandomAccessFile raf = new RandomAccessFile(((java.nio.file.Path) file).toFile(), "r");
            raf.seek(from);
            StreamingOutput stream = output -> {
                byte[] buf = new byte[4096];
                long remaining = contentLength;
                try (OutputStream out = output) {
                    int read;
                    while (remaining > 0 && (read = raf.read(buf, 0, (int)Math.min(buf.length, remaining))) != -1) {
                        out.write(buf, 0, read);
                        remaining -= read;
                    }
                    out.flush();
                } finally {
                    raf.close();
                }
            };

            Response.ResponseBuilder rb = Response.status(range == null ? 200 : 206)
                    .entity(stream)
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Length", String.valueOf(contentLength))
                    .header("Content-Type", "video/mp4");
            if (range != null) {
                rb.header("Content-Range", String.format("bytes %d-%d/%d", from, to, length));
            }
            return rb.build();

        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build();
        }
    }
}
