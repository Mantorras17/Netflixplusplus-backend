package org.netflixpp.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.netflixpp.config.CassandraConfig;
import com.datastax.oss.driver.api.core.cql.*;

@Path("/mesh")
public class MeshController {

    @GET
    @Path("/chunks/{movie}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listChunks(@PathParam("movie") String movie) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        ResultSet rs = CassandraConfig.getSession().execute(
                SimpleStatement.newInstance("SELECT * FROM mesh_chunks WHERE movie_title = ?", movie));
        for (Row row : rs) {
            Map<String, Object> c = new HashMap<>();
            c.put("index", row.getInt("chunk_index"));
            c.put("sha256", row.getString("sha256"));
            c.put("path", row.getString("path"));
            chunks.add(c);
        }
        return Response.ok(chunks).build();
    }

    @GET
    @Path("/chunk/{movie}/{index}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getChunk(@PathParam("movie") String movie, @PathParam("index") int index) {
        Row row = CassandraConfig.getSession().execute(
                SimpleStatement.newInstance("SELECT path FROM mesh_chunks WHERE movie_title=? AND chunk_index=?", movie, index)
        ).one();

        if (row == null) return Response.status(404).build();
        Path chunkFile = (Path) Paths.get(row.getString("path"));
        if (!Files.exists((java.nio.file.Path) chunkFile)) return Response.status(404).build();

        return Response.ok(((java.nio.file.Path) chunkFile).toFile())
                .header("Content-Disposition", "attachment; filename=\"chunk_" + index + ".bin\"")
                .build();
    }
}
