package com.amis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * OfflineTrack — Represents a song downloaded for offline playback.
 *
 * The Offline Download Manager uses this to:
 *  - Track how much quota a user has consumed
 *  - Identify least-recently-played offline tracks for eviction
 *  - Prioritize downloads: active-playlist > recently-played > liked
 */
@Entity
@Table(name = "offline_tracks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "song_id"}))
public class OfflineTrack extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    /** Simulated file size in MB (stored as metadata) */
    @Column(name = "size_mb", nullable = false)
    private double sizeMb;

    /** Priority when downloading: 1=active-playlist, 2=recently-played, 3=liked */
    @Column(name = "priority")
    private int priority;

    /** Status: QUEUED | DOWNLOADED | EVICTED */
    @Column(name = "status", nullable = false)
    private String status = "QUEUED";

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    /** Last time this offline track was actually played */
    @Column(name = "last_played_offline")
    private LocalDateTime lastPlayedOffline;

    // Constructors
    public OfflineTrack() {}

    public OfflineTrack(Long userId, Long songId, double sizeMb, int priority) {
        this.userId = userId;
        this.songId = songId;
        this.sizeMb = sizeMb;
        this.priority = priority;
        this.status = "QUEUED";
    }

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }

    public double getSizeMb() { return sizeMb; }
    public void setSizeMb(double sizeMb) { this.sizeMb = sizeMb; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(LocalDateTime downloadedAt) { this.downloadedAt = downloadedAt; }

    public LocalDateTime getLastPlayedOffline() { return lastPlayedOffline; }
    public void setLastPlayedOffline(LocalDateTime lastPlayedOffline) {
        this.lastPlayedOffline = lastPlayedOffline;
    }
}
