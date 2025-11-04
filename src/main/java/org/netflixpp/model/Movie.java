package org.netflixpp.model;

public class Movie {
    private int id;
    private String title;
    private String description;
    private String category;
    private String genre;
    private int year;
    private int duration; // em minutos
    private String filePath1080;
    private String filePath360;

    public Movie() {}

    public Movie(int id, String title, String description, String category, String genre, int year, int duration, String filePath1080, String filePath360) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.genre = genre;
        this.year = year;
        this.duration = duration;
        this.filePath1080 = filePath1080;
        this.filePath360 = filePath360;
    }

    // Getters e setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getFilePath1080() { return filePath1080; }
    public void setFilePath1080(String filePath1080) { this.filePath1080 = filePath1080; }
    public String getFilePath360() { return filePath360; }
    public void setFilePath360(String filePath360) { this.filePath360 = filePath360; }
}
