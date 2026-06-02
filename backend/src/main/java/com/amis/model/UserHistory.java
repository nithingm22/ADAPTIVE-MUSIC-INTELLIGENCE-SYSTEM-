package com.amis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UserHistory - Records every song a user plays.
 * Used by the recommendation engine for recency scoring.
 */
@Entity
@Table(name = "user_history")
public class UserHistory extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    // Constructors
    public UserHistory() {}

    public UserHistory(Long userId, Long songId) {
        this.userId = userId;
        this.songId = songId;
        this.playedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
}
