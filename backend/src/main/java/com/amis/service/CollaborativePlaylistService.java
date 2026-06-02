package com.amis.service;

import com.amis.dto.response.PlaylistResponse;
import com.amis.model.PlaylistEdit;

import java.util.List;
import java.util.Map;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * BUSINESS LOGIC 6: Collaborative Playlist Conflict Resolver
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * HOW IT WORKS:
 * 1. Multiple users can add/remove/reorder songs in a shared playlist.
 * 2. Every edit is stored as a PlaylistEdit with a precise timestamp.
 * 3. When merging, edits are sorted by timestamp (oldest first).
 * 4. Conflict detection rules:
 *      CONFLICT TYPE A — Both users ADD the same song:
 *        → Only one entry is kept (deduplication). Both users notified.
 *      CONFLICT TYPE B — One user ADDs, another REMOVEs same song:
 *        → ADD WINS. Song stays. Remover is notified.
 *      CONFLICT TYPE C — Both users ADD different songs at the same position:
 *        → BOTH songs inserted. Position of later-timestamp edit is shifted +1.
 *        → Both collaborators are notified of the position change.
 * 5. Notifications are stored in CollaborativePlaylist.conflictNotifications
 *    so the frontend can display them.
 * 6. Final playlist state is returned after conflict resolution.
 */
public interface CollaborativePlaylistService {

    /** Convert a regular playlist to a collaborative (shared) playlist */
    void makeCollaborative(Long playlistId, List<Long> collaboratorUserIds);

    /** Record a user's edit (ADD / REMOVE / REORDER) */
    PlaylistEdit recordEdit(Long playlistId, Long userId, Long songId, String editType, Integer position);

    /**
     * Merge all pending edits and resolve conflicts.
     * Returns the resolved playlist + conflict notification messages.
     */
    Map<String, Object> mergeAndResolve(Long playlistId, String userEmail);

    /** Get the full edit history for a collaborative playlist */
    List<PlaylistEdit> getEditHistory(Long playlistId);

    /** Get any conflict notifications pending for this playlist */
    List<String> getConflictNotifications(Long playlistId);
}
