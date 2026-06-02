package com.amis.controller;

import com.amis.dto.response.ApiResponse;
import com.amis.model.Song;
import com.amis.model.User;
import com.amis.model.UserHistory;
import com.amis.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UserController - Listening history, liked songs, and user personalization.
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** POST /user/history - Record that user played a song */
    @PostMapping("/history")
    public ResponseEntity<ApiResponse<UserHistory>> recordPlay(
            @RequestBody Map<String, Long> body, Authentication auth) {
        try {
            UserHistory history = userService.recordPlay(auth.getName(), body.get("songId"));
            return ResponseEntity.ok(ApiResponse.success("Play recorded", history));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** GET /user/history - Get recently played songs */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Song>>> getHistory(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(userService.getRecentlyPlayed(auth.getName())));
    }

    /** POST /user/like - Like a song */
    @PostMapping("/like")
    public ResponseEntity<ApiResponse<String>> likeSong(
            @RequestBody Map<String, Long> body, Authentication auth) {
        try {
            userService.likeSong(auth.getName(), body.get("songId"));
            return ResponseEntity.ok(ApiResponse.success("Song liked"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** DELETE /user/like/{songId} - Unlike a song */
    @DeleteMapping("/like/{songId}")
    public ResponseEntity<ApiResponse<String>> unlikeSong(
            @PathVariable Long songId, Authentication auth) {
        try {
            userService.unlikeSong(auth.getName(), songId);
            return ResponseEntity.ok(ApiResponse.success("Song unliked"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** GET /user/likes - Get all liked songs */
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<List<Song>>> getLikedSongs(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(userService.getLikedSongs(auth.getName())));
    }

    /** GET /user/likes/{songId} - Check if a song is liked */
    @GetMapping("/likes/{songId}")
    public ResponseEntity<ApiResponse<Boolean>> isSongLiked(
            @PathVariable Long songId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(userService.isSongLiked(auth.getName(), songId)));
    }

    /**
     * GET /user/me — returns the current user's profile including subscriptionTier.
     * Call this after a successful payment to refresh the session tier without re-logging in.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(Authentication auth) {
        try {
            com.amis.model.User user = userService.getAllUsers().stream()
                    .filter(u -> u.getEmail().equals(auth.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Map<String, Object> profile = new java.util.LinkedHashMap<>();
            profile.put("id",               user.getId());
            profile.put("name",             user.getName());
            profile.put("email",            user.getEmail());
            profile.put("role",             user.getRole().name());
            profile.put("subscriptionTier", user.getSubscriptionTier());
            return ResponseEntity.ok(ApiResponse.success(profile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** GET /admin/users - Get all users (ADMIN only) */
    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }
}
