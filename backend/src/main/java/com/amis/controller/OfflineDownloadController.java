package com.amis.controller;

import com.amis.dto.response.ApiResponse;
import com.amis.model.OfflineTrack;
import com.amis.service.OfflineDownloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OfflineDownloadController — Business Logic 7 endpoints.
 * Queue downloads, process quota, get storage summary.
 */
@RestController
@RequestMapping("/offline")
@CrossOrigin(origins = "http://localhost:3000")
public class OfflineDownloadController {

    private final OfflineDownloadService offlineService;

    public OfflineDownloadController(OfflineDownloadService offlineService) {
        this.offlineService = offlineService;
    }

    /**
     * POST /offline/queue
     * Body: { "songId": 5 }
     * Queues a song for offline download with automatic priority assignment.
     */
    @PostMapping("/queue")
    public ResponseEntity<ApiResponse<OfflineTrack>> queue(
            @RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long songId = Long.valueOf(body.get("songId").toString());
            OfflineTrack track = offlineService.queueDownload(auth.getName(), songId);
            return ResponseEntity.ok(ApiResponse.success("Song queued for download", track));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /offline/process
     * Processes the download queue: applies quota checks and eviction.
     * Returns list of newly downloaded tracks.
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<List<OfflineTrack>>> process(Authentication auth) {
        try {
            List<OfflineTrack> downloaded = offlineService.processDownloadQueue(auth.getName());
            return ResponseEntity.ok(ApiResponse.success(
                    downloaded.size() + " track(s) downloaded", downloaded));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /offline/tracks?status=DOWNLOADED
     * Returns offline tracks filtered by status (QUEUED / DOWNLOADED / EVICTED).
     */
    @GetMapping("/tracks")
    public ResponseEntity<ApiResponse<List<OfflineTrack>>> getTracks(
            @RequestParam(required = false) String status, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offlineService.getOfflineTracks(auth.getName(), status)));
    }

    /**
     * DELETE /offline/tracks/{songId}
     * Removes a song from offline storage (marks EVICTED).
     */
    @DeleteMapping("/tracks/{songId}")
    public ResponseEntity<ApiResponse<String>> remove(
            @PathVariable Long songId, Authentication auth) {
        try {
            offlineService.removeOfflineTrack(auth.getName(), songId);
            return ResponseEntity.ok(ApiResponse.success("Track removed from offline storage"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /offline/storage
     * Returns storage usage summary: quota, used MB, available MB, track counts.
     */
    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> storageSummary(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offlineService.getStorageSummary(auth.getName())));
    }

    /**
     * POST /offline/played
     * Body: { "songId": 3 }
     * Marks an offline track as played (updates LRU eviction order).
     */
    @PostMapping("/played")
    public ResponseEntity<ApiResponse<String>> markPlayed(
            @RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long songId = Long.valueOf(body.get("songId").toString());
            offlineService.markPlayedOffline(auth.getName(), songId);
            return ResponseEntity.ok(ApiResponse.success("Marked as played offline"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
