package com.amis.controller;

import com.amis.dto.request.PlaylistGenerateRequest;
import com.amis.dto.request.PlaylistRequest;
import com.amis.dto.response.ApiResponse;
import com.amis.dto.response.PlaylistResponse;
import com.amis.service.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PlaylistController - Manage playlists and dynamic generation.
 */
@RestController
@RequestMapping("/playlists")
@CrossOrigin(origins = "http://localhost:3000")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    /** POST /playlists - Create a new playlist */
    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistResponse>> create(
            @Valid @RequestBody PlaylistRequest request, Authentication auth) {
        try {
            PlaylistResponse response = playlistService.createPlaylist(auth.getName(), request);
            return ResponseEntity.ok(ApiResponse.success("Playlist created", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** GET /playlists - Get current user's playlists */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaylistResponse>>> getUserPlaylists(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(playlistService.getUserPlaylists(auth.getName())));
    }

    /** GET /playlists/{id} - Get a specific playlist */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistResponse>> getPlaylist(
            @PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.success(playlistService.getPlaylistById(id, auth.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /playlists/{id}/songs - Add a song to a playlist */
    @PostMapping("/{id}/songs")
    public ResponseEntity<ApiResponse<String>> addSong(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            Authentication auth) {
        try {
            playlistService.addSongToPlaylist(id, body.get("songId"), auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Song added to playlist"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** DELETE /playlists/{id}/songs/{songId} - Remove a song */
    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<ApiResponse<String>> removeSong(
            @PathVariable Long id, @PathVariable Long songId, Authentication auth) {
        try {
            playlistService.removeSongFromPlaylist(id, songId, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Song removed from playlist"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** POST /playlists/generate - AI-powered playlist generation */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<PlaylistResponse>> generate(
            @Valid @RequestBody PlaylistGenerateRequest request, Authentication auth) {
        try {
            PlaylistResponse response = playlistService.generatePlaylist(auth.getName(), request);
            return ResponseEntity.ok(ApiResponse.success("Playlist generated", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** DELETE /playlists/{id} - Delete a playlist */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePlaylist(
            @PathVariable Long id, Authentication auth) {
        try {
            playlistService.deletePlaylist(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Playlist deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
