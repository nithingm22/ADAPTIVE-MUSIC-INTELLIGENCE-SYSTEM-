package com.amis.model;

import jakarta.persistence.*;

/**
 * Song entity - Represents a music track in AMIS.
 * playCount is used by the trending and recommendation engines.
 */
@Entity
@Table(name = "songs")
public class Song extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "artist", nullable = false)
    private String artist;

    @Column(name = "genre", nullable = false)
    private String genre;

    // Duration in seconds
    @Column(name = "duration")
    private Integer duration;

    // Total number of plays across all users
    @Column(name = "play_count", nullable = false)
    private Long playCount = 0L;

    // Album art URL (optional - can point to external image)
    @Column(name = "album_art_url")
    private String albumArtUrl;

    // Audio file URL or streaming URL
    @Column(name = "audio_url")
    private String audioUrl;

    // Constructors
    public Song() {}

    public Song(String title, String artist, String genre, Integer duration) {
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.duration = duration;
        this.playCount = 0L;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Long getPlayCount() { return playCount; }
    public void setPlayCount(Long playCount) { this.playCount = playCount; }

    public String getAlbumArtUrl() { return albumArtUrl; }
    public void setAlbumArtUrl(String albumArtUrl) { this.albumArtUrl = albumArtUrl; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
}
