package com.amis.model;

import jakarta.persistence.*;

/**
 * Playlist entity - A named collection of songs owned by a user.
 */
@Entity
@Table(name = "playlists")
public class Playlist extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Optional: mood or genre tag for generated playlists
    @Column(name = "tag")
    private String tag;

    // Constructors
    public Playlist() {}

    public Playlist(String name, Long userId, String tag) {
        this.name = name;
        this.userId = userId;
        this.tag = tag;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
