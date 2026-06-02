package com.amis.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * PlaylistGenerateRequest - Input for dynamic playlist generation.
 */
public class PlaylistGenerateRequest {

    @NotBlank(message = "Playlist name is required")
    private String name;

    // e.g., "chill", "workout", "focus", "party"
    private String mood;

    // Optional genre filter
    private String genre;

    // Number of songs to include
    private int songCount = 20;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }
}
