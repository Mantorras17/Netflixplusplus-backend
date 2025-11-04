package org.netflixpp.service;

import jakarta.ws.rs.core.StreamingOutput;
import org.netflixpp.config.MariaDBConfig;
import org.netflixpp.model.Movie;
import java.io.*;
import java.nio.file.*;
import java.sql.*;

public class StreamService {

    public File getMovieFile(String title, String resolution) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT file_path_1080, file_path_360 FROM movies WHERE title = ?")) {
            st.setString(1, title);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    if ("1080".equals(resolution)) {
                        return new File(rs.getString("file_path_1080"));
                    } else if ("360".equals(resolution)) {
                        return new File(rs.getString("file_path_360"));
                    }
                }
            }
        }
        return null; // Filme não encontrado
    }

    public StreamingOutput streamMovie(File file, long start, long end) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(start);
        return os -> {
            byte[] buffer = new byte[4096];
            long remaining = end - start + 1;
            try (OutputStream out = os) {
                int bytesRead;
                while (remaining > 0 && (bytesRead = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    out.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                out.flush();
            } finally {
                raf.close();
            }
        };
    }
}
