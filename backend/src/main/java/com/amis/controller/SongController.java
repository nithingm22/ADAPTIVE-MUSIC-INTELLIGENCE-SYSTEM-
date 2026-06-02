package com.amis.controller;

import com.amis.dto.request.SongRequest;
import com.amis.dto.response.ApiResponse;
import com.amis.model.Song;
import com.amis.service.SongService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SongController - Manages song CRUD, search, and trending.
 * POST/PUT/DELETE: ADMIN only (enforced by SecurityConfig)
 * GET: Any authenticated user
 */
@RestController
@RequestMapping("/songs")
@CrossOrigin(origins = "http://localhost:3000")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    /** POST /songs - Create a new song (ADMIN only) */
    @PostMapping
    public ResponseEntity<ApiResponse<Song>> createSong(@Valid @RequestBody SongRequest request) {
        try {
            Song song = songService.createSong(request);
            return ResponseEntity.ok(ApiResponse.success("Song created", song));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** GET /songs - Get all songs */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Song>>> getAllSongs() {
        return ResponseEntity.ok(ApiResponse.success(songService.getAllSongs()));
    }

    /** GET /songs/{id} - Get a single song */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Song>> getSong(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(songService.getSongById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** PUT /songs/{id} - Update a song (ADMIN only) */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Song>> updateSong(
            @PathVariable Long id, @Valid @RequestBody SongRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Song updated", songService.updateSong(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** DELETE /songs/{id} - Delete a song (ADMIN only) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSong(@PathVariable Long id) {
        try {
            songService.deleteSong(id);
            return ResponseEntity.ok(ApiResponse.success("Song deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** GET /songs/trending - Get trending songs */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<Song>>> getTrending() {
        return ResponseEntity.ok(ApiResponse.success(songService.getTrendingSongs()));
    }

    /** GET /songs/genres - Get all unique genres */
    @GetMapping("/genres")
    public ResponseEntity<ApiResponse<List<String>>> getGenres() {
        return ResponseEntity.ok(ApiResponse.success(songService.getAllGenres()));
    }

    /** GET /search?query=... - Search songs */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Song>>> search(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success(songService.searchSongs(query)));
    }
}
