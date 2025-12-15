package org.netflixpp.controller;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.SignUrlOption;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.netflixpp.config.Config;
import org.netflixpp.util.FirebaseUtil;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/hls")
public class HlsController {

    @GET
    @Path("/sign")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sign(@HeaderParam("Authorization") String auth,
                         @QueryParam("path") String requestPath) {
        try {
            // Auth required: Firebase ID token
            boolean authenticated = false;

            if (auth != null && auth.startsWith("Bearer ")) {
                var fb = FirebaseUtil.verifyIdToken(auth);
                authenticated = (fb != null);
            }

            if (!authenticated) {
                return Response.status(401)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            if (requestPath == null || requestPath.isEmpty()) {
                return Response.status(400)
                        .entity(Map.of("error", "Missing path"))
                        .build();
            }

            String objectPath = normalizeObjectPath(requestPath);
            if (objectPath == null) {
                return Response.status(400)
                        .entity(Map.of("error", "Invalid path"))
                        .build();
            }

            // Only allow HLS assets under movies/{movieId}/{resolution}/hls/
            if (!objectPath.startsWith("movies/") || !objectPath.contains("/hls/")) {
                return Response.status(403)
                        .entity(Map.of("error", "Forbidden path"))
                        .build();
            }

            // Build Storage client from configured credentials
            Storage storage;
            if (Config.GCS_CREDENTIALS_PATH != null && !Config.GCS_CREDENTIALS_PATH.isEmpty()) {
                try (FileInputStream fis = new FileInputStream(Config.GCS_CREDENTIALS_PATH)) {
                    storage = StorageOptions.newBuilder()
                            .setCredentials(GoogleCredentials.fromStream(fis))
                            .build()
                            .getService();
                }
            } else {
                storage = StorageOptions.getDefaultInstance().getService();
            }

            BlobInfo blob = BlobInfo.newBuilder(Config.GCS_BUCKET_NAME, objectPath).build();
            URL signed = storage.signUrl(
                    blob,
                    15, TimeUnit.MINUTES,
                    SignUrlOption.withV4Signature()
            );

            // Return signed URL in header for Nginx auth_request consumption
            return Response.ok(Map.of(
                            "status", "ok",
                            "bucket", Config.GCS_BUCKET_NAME,
                            "object", objectPath,
                            "expiresInMinutes", 15
                    ))
                    .header("X-GCS-Signed-Url", signed.toString())
                    .build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private static String normalizeObjectPath(String requestPath) {
        // Remove query string if any
        String p = requestPath;
        int q = p.indexOf('?');
        if (q >= 0) p = p.substring(0, q);

        // Trim leading slashes
        while (p.startsWith("/")) p = p.substring(1);

        // If prefixed by "hls/" as in Nginx public location, drop it
        if (p.startsWith("hls/")) {
            p = p.substring("hls/".length());
        }

        // After normalization we expect paths beginning with "movies/"
        return p;
    }
}
