package com.amis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PlaylistEdit — Records every change a user makes to a shared playlist.
 *
 * The Collaborative Conflict Resolver replays these edits in
 * timestamp order to merge concurrent modifications correctly.
 *
 * Edit types:
 *   ADD      — user added a song at a given position
 *   REMOVE   — user removed a song
 *   REORDER  — user moved a song to a different position
 */
@Entity
@Table(name = "playlist_edits")
public class PlaylistEdit extends BaseEntity {

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    /** ADD | REMOVE | REORDER */
    @Column(name = "edit_type", nullable = false)
    private String editType;

    /** Position in playlist (used for ADD and REORDER) */
    @Column(name = "position")
    private Integer position;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    /**
     * Whether this edit was part of a conflict resolution.
     * Flagged edits trigger a notification to all collaborators.
     */
    @Column(name = "conflict_flagged")
    private boolean conflictFlagged = false;

    // Constructors
    public PlaylistEdit() {}

    public PlaylistEdit(Long playlistId, Long userId, Long songId, String editType, Integer position) {
        this.playlistId = playlistId;
        this.userId = userId;
        this.songId = songId;
        this.editType = editType;
        this.position = position;
        this.editedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getPlaylistId() { return playlistId; }
    public void setPlaylistId(Long playlistId) { this.playlistId = playlistId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }

    public String getEditType() { return editType; }
    public void setEditType(String editType) { this.editType = editType; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public boolean isConflictFlagged() { return conflictFlagged; }
    public void setConflictFlagged(boolean conflictFlagged) { this.conflictFlagged = conflictFlagged; }
}
