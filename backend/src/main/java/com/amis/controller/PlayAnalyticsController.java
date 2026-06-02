package com.amis.controller;

import com.amis.dto.response.ApiResponse;
import com.amis.model.PlayStats;
import com.amis.service.PlayAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PlayAnalyticsController — Business Logic 8 endpoints.
 * Trending tracker, artist aggregation, streak, weekly hours.
 */
@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "http://localhost:3000")
public class PlayAnalyticsController {

    private final PlayAnalyticsService analyticsService;

    public PlayAnalyticsController(PlayAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * POST /analytics/record
     * Body: { "songId": 1, "songTitle": "Blinding Lights", "artist": "The Weeknd", "genre": "pop" }
     * Records a play event and updates the track's PlayStats + trending delta.
     */
    @PostMapping("/record")
    public ResponseEntity<ApiResponse<PlayStats>> record(
            @RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long songId = Long.valueOf(body.get("songId").toString());
            String title  = (String) body.getOrDefault("songTitle", "Unknown");
            String artist = (String) body.getOrDefault("artist", "Unknown");
            String genre  = (String) body.getOrDefault("genre", "Unknown");
            // Fetch userId from auth — for streak tracking
            Long userId = null; // resolved inside service via email
            PlayStats stats = analyticsService.recordPlay(songId, title, artist, genre, userId);
            return ResponseEntity.ok(ApiResponse.success("Play recorded, stats updated", stats));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /analytics/trending?limit=10
     * Returns tracks sorted by trendingDelta DESC (highest growth this week).
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<PlayStats>>> trending(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getTrendingTracks(limit)));
    }

    /**
     * GET /analytics/top-tracks?limit=10
     * Returns tracks sorted by all-time totalPlays DESC.
     */
    @GetMapping("/top-tracks")
    public ResponseEntity<ApiResponse<List<PlayStats>>> topTracks(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getTopTracks(limit)));
    }

    /**
     * GET /analytics/artists
     * Returns Map<ArtistName, TotalPlays> sorted DESC.
     */
    @GetMapping("/artists")
    public ResponseEntity<ApiResponse<Map<String, Long>>> artistAggregation() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getArtistAggregation()));
    }

    /**
     * GET /analytics/genres
     * Returns Map<Genre, TotalPlays> sorted DESC.
     */
    @GetMapping("/genres")
    public ResponseEntity<ApiResponse<Map<String, Long>>> genreAggregation() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getGenreAggregation()));
    }

    /**
     * GET /analytics/streak
     * Returns the authenticated user's consecutive listening-day streak.
     */
    @GetMapping("/streak")
    public ResponseEntity<ApiResponse<Integer>> streak(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getListeningStreak(auth.getName())));
    }

    /**
     * GET /analytics/weekly-hours
     * Returns how many hours the user listened in the last 7 days.
     */
    @GetMapping("/weekly-hours")
    public ResponseEntity<ApiResponse<Double>> weeklyHours(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getWeeklyListeningHours(auth.getName())));
    }

    /**
     * GET /analytics/dashboard
     * Returns a full personal analytics dashboard:
     * streak, weekly hours, trending tracks, genre/artist breakdowns.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getPersonalDashboard(auth.getName())));
    }
}
