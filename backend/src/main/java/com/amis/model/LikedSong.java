package com.amis.model;

import jakarta.persistence.*;

/**
 * LikedSong - Tracks songs that a user has liked/favorited.
 */
@Entity
@Table(name = "liked_songs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "song_id"}))
public class LikedSong extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    // Constructors
    public LikedSong() {}

    public LikedSong(Long userId, Long songId) {
        this.userId = userId;
        this.songId = songId;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }
}
