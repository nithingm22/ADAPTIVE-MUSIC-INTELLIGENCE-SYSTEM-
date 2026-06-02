package com.amis.controller;

import com.amis.dto.response.ApiResponse;
import com.amis.model.Song;
import com.amis.service.SmartPlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SmartPlaylistController — Business Logic 5 endpoints.
 *
 * Generates mood + history-aware playlists and exposes
 * the user's preference frequency maps.
 */
@RestController
@RequestMapping("/smart-playlist")
@CrossOrigin(origins = "http://localhost:3000")
public class SmartPlaylistController {

    private final SmartPlaylistService smartPlaylistService;

    public SmartPlaylistController(SmartPlaylistService smartPlaylistService) {
        this.smartPlaylistService = smartPlaylistService;
    }

    /**
     * POST /smart-playlist/generate
     * Body: { "mood": "energetic", "limit": 15 }
     * Returns: ranked list of songs matching mood + user preferences
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<Song>>> generate(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            String mood  = (String) body.getOrDefault("mood", "energetic");
            int    limit = body.containsKey("limit") ? (int) body.get("limit") : 15;
            List<Song> songs = smartPlaylistService.generateSmartPlaylist(auth.getName(), mood, limit);
            return ResponseEntity.ok(ApiResponse.success("Smart playlist generated", songs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /smart-playlist/preferences
     * Returns: { "genres": {pop: 42, rock: 28}, "artists": {...} }
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Long>>>> getPreferences(Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    smartPlaylistService.getUserPreferenceMap(auth.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /smart-playlist/play-event
     * Body: { "songId": 1, "sessionMood": "energetic", "listenDurationSeconds": 180 }
     * Records a rich play event for use in the Smart Playlist and Analytics engines.
     */
    @PostMapping("/play-event")
    public ResponseEntity<ApiResponse<String>> recordPlayEvent(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long songId = Long.valueOf(body.get("songId").toString());
            String mood = (String) body.getOrDefault("sessionMood", "general");
            Integer duration = body.containsKey("listenDurationSeconds")
                    ? Integer.valueOf(body.get("listenDurationSeconds").toString()) : null;
            smartPlaylistService.recordPlayEvent(auth.getName(), songId, mood, duration);
            return ResponseEntity.ok(ApiResponse.success("Play event recorded"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
