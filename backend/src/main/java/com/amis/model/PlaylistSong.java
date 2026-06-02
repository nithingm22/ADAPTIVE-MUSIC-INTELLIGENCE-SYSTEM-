package com.amis.model;

import jakarta.persistence.*;

/**
 * PlaylistSong - Maps songs to playlists (many-to-many relationship).
 */
@Entity
@Table(name = "playlist_songs")
public class PlaylistSong extends BaseEntity {

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    // Position of the song in the playlist (for ordering)
    @Column(name = "position")
    private Integer position;

    // Constructors
    public PlaylistSong() {}

    public PlaylistSong(Long playlistId, Long songId, Integer position) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.position = position;
    }

    // Getters and Setters
    public Long getPlaylistId() { return playlistId; }
    public void setPlaylistId(Long playlistId) { this.playlistId = playlistId; }

    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
