package org.netflixpp.service;

import org.netflixpp.config.MariaDBConfig;
import org.netflixpp.config.CassandraConfig;
import org.netflixpp.model.Movie;

import java.sql.*;
import java.nio.file.*;
import java.util.*;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

public class MovieService {

    public List<Movie> listMovies() throws Exception {
        List<Movie> list = new ArrayList<>();
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT * FROM movies");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                list.add(new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getString("genre"),
                        rs.getInt("year"),
                        rs.getInt("duration"),
                        rs.getString("file_path_1080"),
                        rs.getString("file_path_360")
                ));
            }
        }
        return list;
    }

    public Movie getMovie(int id) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT * FROM movies WHERE id=?")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new Movie(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("category"),
                            rs.getString("genre"),
                            rs.getInt("year"),
                            rs.getInt("duration"),
                            rs.getString("file_path_1080"),
                            rs.getString("file_path_360")
                    );
                }
            }
        }
        return null;
    }

    public void uploadMovieAdmin(java.io.InputStream file, String title, String description,
                                 String category, String genre, int year, int duration) throws Exception {

        Files.createDirectories(Paths.get("storage/movies/"));
        String safe = title.replaceAll("[^a-zA-Z0-9_\\- ]", "");
        String filename = safe.replace(" ", "_") + "_1080p.mp4";
        Path out = Paths.get("storage/movies/" + filename);
        Files.copy(file, out, StandardCopyOption.REPLACE_EXISTING);

        Path low = Paths.get("storage/movies/" + safe.replace(" ", "_") + "_360p.mp4");
        generate360p(out, low);

        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement(
                     "INSERT INTO movies (title, description, category, genre, year, duration, file_path_1080, file_path_360) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            st.setString(1, title);
            st.setString(2, description);
            st.setString(3, category);
            st.setString(4, genre);
            st.setInt(5, year);
            st.setInt(6, duration);
            st.setString(7, out.toString());
            st.setString(8, low.toString());
            st.executeUpdate();
        }

        CassandraConfig.getSession().execute(SimpleStatement.newInstance(
                "INSERT INTO movie_chunks (movie_title, total_chunks, resolution1080, resolution360) VALUES (?, ?, ?, ?)",
                title, 0, out.toString(), low.toString()
        ));
    }

    public void updateMovie(int id, String title, String description, String category, String genre, int year, int duration) throws Exception {
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement(
                     "UPDATE movies SET title=?, description=?, category=?, genre=?, year=?, duration=? WHERE id=?")) {
            st.setString(1, title);
            st.setString(2, description);
            st.setString(3, category);
            st.setString(4, genre);
            st.setInt(5, year);
            st.setInt(6, duration);
            st.setInt(7, id);
            st.executeUpdate();
        }
    }

    public void deleteMovie(int id) throws Exception {
        Movie m = getMovie(id);
        if (m == null) return;
        if (m.getFilePath1080() != null) Files.deleteIfExists(Paths.get(m.getFilePath1080()));
        if (m.getFilePath360() != null) Files.deleteIfExists(Paths.get(m.getFilePath360()));
        try (Connection conn = MariaDBConfig.getConnection();
             PreparedStatement st = conn.prepareStatement("DELETE FROM movies WHERE id=?")) {
            st.setInt(1, id);
            st.executeUpdate();
        }
    }

    private void generate360p(Path in, Path out) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", in.toString(),
                "-vf", "scale=-2:360",
                "-c:v", "libx264", "-preset", "fast", "-crf", "28",
                "-c:a", "aac", "-b:a", "96k",
                out.toString()
        );
        pb.inheritIO();
        Process p = pb.start();
        if (p.waitFor() != 0) throw new RuntimeException("ffmpeg failed");
    }
}
