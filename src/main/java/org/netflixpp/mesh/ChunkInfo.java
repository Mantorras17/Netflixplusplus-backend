package org.netflixpp.mesh;

public class ChunkInfo {
    private String movieId;
    private int chunkIndex;
    private String hash;
    private long size;
    private String filePath;

    public ChunkInfo() {}

    public ChunkInfo(String movieId, int chunkIndex, String hash, long size, String filePath) {
        this.movieId = movieId;
        this.chunkIndex = chunkIndex;
        this.hash = hash;
        this.size = size;
        this.filePath = filePath;
    }

    // Getters and Setters
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public String toString() {
        return "ChunkInfo{" +
                "movieId='" + movieId + '\'' +
                ", chunkIndex=" + chunkIndex +
                ", hash='" + hash + '\'' +
                ", size=" + size +
                '}';
    }
}