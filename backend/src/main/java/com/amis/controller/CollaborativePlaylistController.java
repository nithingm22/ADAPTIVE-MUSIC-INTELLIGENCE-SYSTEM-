package com.amis.controller;

import com.amis.dto.response.ApiResponse;
import com.amis.model.PlaylistEdit;
import com.amis.model.User;
import com.amis.repository.UserRepository;
import com.amis.service.CollaborativePlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CollaborativePlaylistController — Business Logic 6 endpoints.
 *
 * FIX: recordEdit() now reads the authenticated user's ID from the JWT
 *      instead of requiring it in the request body. This means:
 *        • You don't need to know or type your own user ID
 *        • You cannot accidentally (or intentionally) record an edit
 *          as a different user
 *      Simply be logged in as the right user before calling /edit.
 */
@RestController
@RequestMapping("/collaborative")
@CrossOrigin(origins = "http://localhost:3000")
public class CollaborativePlaylistController {

    private final CollaborativePlaylistService collaborativeService;
    private final UserRepository               userRepository;

    public CollaborativePlaylistController(CollaborativePlaylistService collaborativeService,
                                           UserRepository userRepository) {
        this.collaborativeService = collaborativeService;
        this.userRepository       = userRepository;
    }

    /**
     * POST /collaborative/{playlistId}/make-shared
     * Body: { "collaboratorIds": [2, 3] }
     *
     * Makes a playlist collaborative. Pass the user IDs of everyone
     * who will be editing it together.
     *
     * Tip: use GET /songs to see all song IDs and GET /users to see
     *      all user IDs before starting.
     */
    @PostMapping("/{playlistId}/make-shared")
    public ResponseEntity<ApiResponse<String>> makeShared(
            @PathVariable Long playlistId,
            @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> rawIds = (List<Integer>) body.get("collaboratorIds");
            List<Long> ids = rawIds.stream().map(Long::valueOf).toList();
            collaborativeService.makeCollaborative(playlistId, ids);
            return ResponseEntity.ok(ApiResponse.success(
                    "Playlist #" + playlistId + " is now collaborative. Collaborators: " + ids));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /collaborative/{playlistId}/edit
     * Body: { "songId": 3, "editType": "ADD", "position": 2 }
     *
     * Records a single edit as the currently logged-in user.
     * Valid editType values: ADD, REMOVE, REORDER
     *
     * FIX: userId is no longer needed in the body — it is read from
     *      your login token automatically.
     */
    @PostMapping("/{playlistId}/edit")
    public ResponseEntity<ApiResponse<PlaylistEdit>> recordEdit(
            @PathVariable Long playlistId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            // Derive userId from the JWT — no need to pass it in the body
            Long userId = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                    .getId();

            Long    songId = Long.valueOf(body.get("songId").toString());
            String  type   = (String) body.get("editType");
            Integer pos    = body.containsKey("position")
                    ? Integer.valueOf(body.get("position").toString()) : null;

            PlaylistEdit edit = collaborativeService.recordEdit(playlistId, userId, songId, type, pos);
            return ResponseEntity.ok(ApiResponse.success("Edit recorded", edit));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /collaborative/{playlistId}/merge
     *
     * Merges all saved edits, resolves any conflicts, and saves the
     * clean result. Returns the resolved playlist + plain-English
     * summary of every conflict that was fixed.
     */
    @PostMapping("/{playlistId}/merge")
    public ResponseEntity<ApiResponse<Map<String, Object>>> merge(
            @PathVariable Long playlistId, Authentication auth) {
        try {
            Map<String, Object> result =
                    collaborativeService.mergeAndResolve(playlistId, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Conflicts resolved", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /collaborative/{playlistId}/history
     * Returns the full timestamped edit log for a shared playlist.
     */
    @GetMapping("/{playlistId}/history")
    public ResponseEntity<ApiResponse<List<PlaylistEdit>>> getHistory(
            @PathVariable Long playlistId) {
        return ResponseEntity.ok(ApiResponse.success(
                collaborativeService.getEditHistory(playlistId)));
    }

    /**
     * GET /collaborative/{playlistId}/notifications
     * Returns plain-English conflict resolution messages from the last merge.
     */
    @GetMapping("/{playlistId}/notifications")
    public ResponseEntity<ApiResponse<List<String>>> getNotifications(
            @PathVariable Long playlistId) {
        return ResponseEntity.ok(ApiResponse.success(
                collaborativeService.getConflictNotifications(playlistId)));
    }
}
