package org.netflixpp.controller;

import org.netflixpp.service.AdminService;
import org.netflixpp.service.GcsBackfillService;
import org.netflixpp.service.AuthService;
import org.glassfish.jersey.media.multipart.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.InputStream;
import java.util.Map;
import com.google.firebase.auth.FirebaseToken;
import org.netflixpp.util.FirebaseUtil;

@Path("/admin")
public class AdminController {

    private final AdminService adminService = new AdminService();
    private final GcsBackfillService gcsBackfillService = new GcsBackfillService();
    private final org.netflixpp.service.HlsService hlsService = new org.netflixpp.service.HlsService();
    private final AuthService authService = new AuthService();

    private boolean isAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        try {
            // 1) Validar token Firebase
            FirebaseToken decoded = FirebaseUtil.verifyIdToken(authHeader);
            if (decoded == null) return false;

            String firebaseUid = decoded.getUid();

            // 2) Buscar utilizador interno pela coluna firebase_uid
            Map<String, Object> user = authService.getUserByFirebaseUid(firebaseUid);
            if (user == null) {
                return false;
            }

            // 3) Verificar role na BD
            String role = (String) user.get("role");
            return "admin".equalsIgnoreCase(role);

        } catch (Exception e) {
            return false;
        }
    }

    // TODO: resto dos endpoints /movies, /users, /stats, /logs, etc.
    // continuam exatamente como já tens, todos usando:
    // if (!isAdmin(auth)) { return 403 ... }
}
