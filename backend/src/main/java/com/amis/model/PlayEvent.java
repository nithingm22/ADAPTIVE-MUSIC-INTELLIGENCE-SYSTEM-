package com.amis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PlayEvent — Rich play record used by Smart Playlist and Analytics engines.
 *
 * Unlike the simple UserHistory, this captures:
 *  - mood context at time of play
 *  - mood tags on the song (energetic / calm / focus)
 *  - exact timestamp for streak & window calculations
 */
@Entity
@Table(name = "play_events")
public class PlayEvent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "artist", nullable = false)
    private String artist;

    @Column(name = "genre", nullable = false)
    private String genre;

    /**
     * Mood tag on the song: energetic | calm | focus | party | sleep
     * Set by ADMIN when uploading. Used for mood-based filtering.
     */
    @Column(name = "mood_tag")
    private String moodTag;

    /** Mood the user selected when starting this session */
    @Column(name = "session_mood")
    private String sessionMood;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    /** Duration actually listened (seconds) — for listening-hours analytics */
    @Column(name = "listen_duration_seconds")
    private Integer listenDurationSeconds;

    // Constructors
    public PlayEvent() {}

    public PlayEvent(Long userId, Long songId, String artist, String genre,
                     String moodTag, String sessionMood, Integer listenDurationSeconds) {
        this.userId = userId;
        this.songId = songId;
        this.artist = artist;
        this.genre = genre;
        this.moodTag = moodTag;
        this.sessionMood = sessionMood;
        this.playedAt = LocalDateTime.now();
        this.listenDurationSeconds = listenDurationSeconds != null ? listenDurationSeconds : 0;
    }

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getMoodTag() { return moodTag; }
    public void setMoodTag(String moodTag) { this.moodTag = moodTag; }

    public String getSessionMood() { return sessionMood; }
    public void setSessionMood(String sessionMood) { this.sessionMood = sessionMood; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }

    public Integer getListenDurationSeconds() { return listenDurationSeconds; }
    public void setListenDurationSeconds(Integer listenDurationSeconds) {
        this.listenDurationSeconds = listenDurationSeconds;
    }
}
