package org.netflixpp.service;

import java.io.InputStream;
import java.util.List;
import org.netflixpp.model.Movie;
import org.netflixpp.model.User;

public class AdminService {

    private final MovieService movieService = new MovieService();
    private final UserService userService = new UserService();

    // MOVIES
    public List<Movie> listMovies() throws Exception { return movieService.listMovies(); }
    public void createMovie(InputStream file, String t, String d, String c, String g, int y, int dur) throws Exception {
        movieService.uploadMovieAdmin(file, t, d, c, g, y, dur);
    }
    public void updateMovie(int id, String t, String d, String c, String g, int y, int dur) throws Exception {
        movieService.updateMovie(id, t, d, c, g, y, dur);
    }
    public void deleteMovie(int id) throws Exception { movieService.deleteMovie(id); }

    // USERS
    public List<User> listUsers() throws Exception { return userService.listUsers(); }
    public void createUser(String u, String p, String r, String e) throws Exception { userService.createUser(u, p, r, e); }
    public void updateUser(int id, String u, String p, String r, String e) throws Exception { userService.updateUser(id, u, p, r, e); }
    public void deleteUser(int id) throws Exception { userService.deleteUser(id); }
}
