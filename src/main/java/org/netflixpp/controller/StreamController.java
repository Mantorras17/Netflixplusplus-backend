package org.netflixpp.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;
import java.io.*;
import java.nio.file.*;

@Path("/stream")
public class StreamController {

    @GET
    @Path("/{fileName}")
    public Response streamVideo(@PathParam("fileName") String fileName, @HeaderParam("Range") String rangeHeader) {
        try {
            File file = Paths.get("storage/movies/" + fileName).toFile();
            if (!file.exists()) return Response.status(404).build();

            long fileLength = file.length();
            long start = 0;
            long end = fileLength - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty())
                    end = Long.parseLong(ranges[1]);
            }

            long contentLength = end - start + 1;
            InputStream inputStream = new FileInputStream(file);
            inputStream.skip(start);
            StreamingOutput output = os -> {
                byte[] buffer = new byte[8192];
                long bytesRemaining = contentLength;
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining))) != -1 && bytesRemaining > 0) {
                    os.write(buffer, 0, bytesRead);
                    bytesRemaining -= bytesRead;
                }
                inputStream.close();
            };

            return Response.ok(output)
                    .status(206)
                    .header("Content-Type", "video/mp4")
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength)
                    .header("Content-Length", contentLength)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
