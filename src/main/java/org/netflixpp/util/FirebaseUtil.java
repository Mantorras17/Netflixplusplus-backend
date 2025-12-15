package org.netflixpp.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.netflixpp.config.Config;

import java.io.FileInputStream;

public class FirebaseUtil {

    private static volatile boolean initialized = false;

    private static void initIfNeeded() {
        if (!Config.FIREBASE_ENABLED) return; // feature flag
        if (initialized) return;

        synchronized (FirebaseUtil.class) {
            if (initialized) return;
            try {
                FirebaseOptions.Builder builder = FirebaseOptions.builder();
                if (Config.FIREBASE_CREDENTIALS_PATH != null &&
                        !Config.FIREBASE_CREDENTIALS_PATH.isEmpty()) {
                    try (FileInputStream fis = new FileInputStream(Config.FIREBASE_CREDENTIALS_PATH)) {
                        builder.setCredentials(GoogleCredentials.fromStream(fis));
                    }
                } else {
                    builder.setCredentials(GoogleCredentials.getApplicationDefault());
                }

                FirebaseOptions options = builder.build();
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }

                initialized = true;
            } catch (Exception e) {
                System.err.println("[FIREBASE][WARN] Failed to initialize Firebase: " + e.getMessage());
            }
        }
    }

    public static FirebaseToken verifyIdToken(String bearerToken) {
        try {
            if (!Config.FIREBASE_ENABLED) return null;
            initIfNeeded();
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) return null;
            String token = bearerToken.substring(7);
            return FirebaseAuth.getInstance().verifyIdToken(token, true);
        } catch (Exception e) {
            System.err.println("[FIREBASE][WARN] Token verification failed: " + e.getMessage());
            return null;
        }
    }
}
