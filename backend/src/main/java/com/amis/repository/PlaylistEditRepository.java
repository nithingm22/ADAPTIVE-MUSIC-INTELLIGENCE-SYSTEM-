package com.amis.repository;

import com.amis.model.PlaylistEdit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * PlaylistEditRepository — Audit log for collaborative playlist edits.
 */
public interface PlaylistEditRepository extends JpaRepository<PlaylistEdit, Long> {

    // Get all edits for a playlist, oldest first (for merge replay)
    List<PlaylistEdit> findByPlaylistIdOrderByEditedAtAsc(Long playlistId);

    // Edits by a specific user on a specific playlist
    List<PlaylistEdit> findByPlaylistIdAndUserId(Long playlistId, Long userId);

    // Check if a specific song has a pending ADD (used for conflict detection)
    boolean existsByPlaylistIdAndSongIdAndEditType(Long playlistId, Long songId, String editType);

    // Get conflict-flagged edits for notification display
    List<PlaylistEdit> findByPlaylistIdAndConflictFlagged(Long playlistId, boolean flagged);
}
