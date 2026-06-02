package com.amis.model;

import jakarta.persistence.*;

/**
 * CollaborativePlaylist — Marks a playlist as shared/collaborative.
 * Stores the list of collaborator user IDs as a comma-separated string
 * (simple approach suitable for this project scope).
 */
@Entity
@Table(name = "collaborative_playlists")
public class CollaborativePlaylist extends BaseEntity {

    @Column(name = "playlist_id", nullable = false, unique = true)
    private Long playlistId;

    /** Comma-separated user IDs who can edit: "1,3,7" */
    @Column(name = "collaborator_ids", columnDefinition = "TEXT")
    private String collaboratorIds;

    /**
     * Pending notifications for conflict-resolved edits.
     * Format: "userId:songId:action;..." (simple string log)
     */
    @Column(name = "conflict_notifications", columnDefinition = "TEXT")
    private String conflictNotifications;

    // Constructors
    public CollaborativePlaylist() {}

    public CollaborativePlaylist(Long playlistId, String collaboratorIds) {
        this.playlistId = playlistId;
        this.collaboratorIds = collaboratorIds;
    }

    // Getters & Setters
    public Long getPlaylistId() { return playlistId; }
    public void setPlaylistId(Long playlistId) { this.playlistId = playlistId; }

    public String getCollaboratorIds() { return collaboratorIds; }
    public void setCollaboratorIds(String collaboratorIds) { this.collaboratorIds = collaboratorIds; }

    public String getConflictNotifications() { return conflictNotifications; }
    public void setConflictNotifications(String conflictNotifications) {
        this.conflictNotifications = conflictNotifications;
    }
}
