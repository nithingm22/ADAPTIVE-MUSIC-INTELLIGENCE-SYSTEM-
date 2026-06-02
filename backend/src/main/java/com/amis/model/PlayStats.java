package com.amis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PlayStats — Aggregated play analytics for a single track (global, not per-user).
 *
 * The Trending Tracker uses this to compare:
 *   plays in last 7 days  vs  plays in prior 7 days  →  trending delta
 *
 * Also stores artist and album for group-level aggregation.
 */
@Entity
@Table(name = "play_stats")
public class PlayStats extends BaseEntity {

    @Column(name = "song_id", nullable = false, unique = true)
    private Long songId;

    @Column(name = "song_title")
    private String songTitle;

    @Column(name = "artist")
    private String artist;

    @Column(name = "album")
    private String album;

    @Column(name = "genre")
    private String genre;

    /** All-time total plays */
    @Column(name = "total_plays", nullable = false)
    private long totalPlays = 0;

    /** Plays in the current 7-day window (reset weekly) */
    @Column(name = "plays_this_week", nullable = false)
    private long playsThisWeek = 0;

    /** Plays in the previous 7-day window */
    @Column(name = "plays_last_week", nullable = false)
    private long playsLastWeek = 0;

    /**
     * Trending delta = playsThisWeek - playsLastWeek
     * Positive = growing, Negative = declining
     * Recomputed on every play event.
     */
    @Column(name = "trending_delta")
    private long trendingDelta = 0;

    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;

    /** When the current weekly window started */
    @Column(name = "week_window_start")
    private LocalDateTime weekWindowStart;

    // Constructors
    public PlayStats() {}

    public PlayStats(Long songId, String songTitle, String artist, String genre) {
        this.songId = songId;
        this.songTitle = songTitle;
        this.artist = artist;
        this.genre = genre;
        this.weekWindowStart = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }

    public String getSongTitle() { return songTitle; }
    public void setSongTitle(String songTitle) { this.songTitle = songTitle; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public long getTotalPlays() { return totalPlays; }
    public void setTotalPlays(long totalPlays) { this.totalPlays = totalPlays; }

    public long getPlaysThisWeek() { return playsThisWeek; }
    public void setPlaysThisWeek(long playsThisWeek) { this.playsThisWeek = playsThisWeek; }

    public long getPlaysLastWeek() { return playsLastWeek; }
    public void setPlaysLastWeek(long playsLastWeek) { this.playsLastWeek = playsLastWeek; }

    public long getTrendingDelta() { return trendingDelta; }
    public void setTrendingDelta(long trendingDelta) { this.trendingDelta = trendingDelta; }

    public LocalDateTime getLastPlayedAt() { return lastPlayedAt; }
    public void setLastPlayedAt(LocalDateTime lastPlayedAt) { this.lastPlayedAt = lastPlayedAt; }

    public LocalDateTime getWeekWindowStart() { return weekWindowStart; }
    public void setWeekWindowStart(LocalDateTime weekWindowStart) { this.weekWindowStart = weekWindowStart; }
}
