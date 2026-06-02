package com.amis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * SongRequest - Data for creating or updating a song (ADMIN only).
 */
public class SongRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Artist is required")
    private String artist;

    @NotBlank(message = "Genre is required")
    private String genre;

    @Positive(message = "Duration must be positive")
    private Integer duration;

    private String albumArtUrl;
    private String audioUrl;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getAlbumArtUrl() { return albumArtUrl; }
    public void setAlbumArtUrl(String albumArtUrl) { this.albumArtUrl = albumArtUrl; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
}
