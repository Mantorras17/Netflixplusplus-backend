package org.netflixpp.service;

import org.netflixpp.model.Movie;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovieService {
    // Simulação de base de dados em memória
    private List<Movie> movies = new ArrayList<>();
    private int nextId = 1;

    public MovieService() {
        // Dados de exemplo dos filmes do PDF
        addMovie(new Movie(0, "Big Buck Bunny", "Comedy, Animation", "Animation", 10));
        addMovie(new Movie(0, "The Letter, Lego movie", "Comedy, Animation, LEGO", "Animation", 6));
        addMovie(new Movie(0, "Charlie Chaplin's The Vagabond", "Comedy", "Comedy", 24));
        addMovie(new Movie(0, "Night of the Living Dead", "Sci-Fi / Horror", "Horror", 95));
        addMovie(new Movie(0, "Popeye the Sailor Meets Ali Baba's Forty Thieves", "Comedy, Animation", "Animation", 16));
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies);
    }

    public Optional<Movie> getMovieById(int id) {
        return movies.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();
    }

    public Movie addMovie(Movie movie) {
        movie.setId(nextId++);
        movies.add(movie);
        return movie;
    }

    public boolean deleteMovie(int id) {
        return movies.removeIf(movie -> movie.getId() == id);
    }

    public List<String> getStreamQualities(int movieId) {
        // Simulação - depois gera links reais
        List<String> qualities = new ArrayList<>();
        qualities.add("360p");
        qualities.add("1080p");
        return qualities;
    }
}