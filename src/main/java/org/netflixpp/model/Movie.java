package org.netflixpp.model;

public class Movie {
    private int id;
    private String title;
    private String description;
    private String genre;
    private int duration; // em minutos

    // Construtores
    public Movie() {}

    public Movie(int id, String title, String description, String genre, int duration) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.duration = duration;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}