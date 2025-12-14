package org.netflixpp.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.netflixpp.config.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class GcsUploader {

    private static volatile GcsUploader INSTANCE;
    private final Storage storage;
    private final String bucket;

    private GcsUploader() {
        this.bucket = Config.GCS_BUCKET_NAME;
        try {
            GoogleCredentials credentials;
            if (Config.GCS_CREDENTIALS_PATH != null && !Config.GCS_CREDENTIALS_PATH.isEmpty()) {
                try (FileInputStream fis = new FileInputStream(Config.GCS_CREDENTIALS_PATH)) {
                    credentials = GoogleCredentials.fromStream(fis);
                }
            } else {
                credentials = GoogleCredentials.getApplicationDefault();
            }

            this.storage = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize GCS credentials: " + e.getMessage(), e);
        }
    }

    public static GcsUploader getInstance() {
        if (INSTANCE == null) {
            synchronized (GcsUploader.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GcsUploader();
                }
            }
        }
        return INSTANCE;
    }

    public void upload(File file, String objectName, String contentType) throws IOException {
        Objects.requireNonNull(file, "file");
        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalStateException("GCS bucket name is not configured. Set env var GCS_BUCKET_NAME.");
        }
        if (!file.exists()) {
            throw new IOException("File not found: " + file);
        }

        byte[] data = Files.readAllBytes(file.toPath());
        BlobId blobId = BlobId.of(bucket, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType != null ? contentType : "application/octet-stream")
                .build();

        int attempts = 0;
        while (attempts < 3) {
            try {
                storage.create(blobInfo, data);
                return;
            } catch (com.google.cloud.storage.StorageException se) {
                attempts++;
                if (attempts >= 3) throw se;
                try { Thread.sleep(300L * attempts); } catch (InterruptedException ignored) {}
            }
        }
        // If we somehow exit the loop without returning or throwing
        throw new com.google.cloud.storage.StorageException(500, "Failed to upload after retries");
    }

    public boolean exists(String objectName) {
        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalStateException("GCS bucket name is not configured. Set env var GCS_BUCKET_NAME.");
        }
        BlobId id = BlobId.of(bucket, objectName);
        try {
            return storage.get(id) != null;
        } catch (com.google.cloud.storage.StorageException se) {
            // If we cannot determine, return false to attempt upload; caller may handle errors
            return false;
        }
    }
}
