package com.amis.service;

import com.amis.model.OfflineTrack;

import java.util.List;
import java.util.Map;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * BUSINESS LOGIC 7: Offline Download Manager & Storage Optimizer
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * HOW IT WORKS:
 *
 * QUOTA MAP (defined in implementation):
 *   Map<SubscriptionTier, OfflineStorageLimitMB>:
 *     FREE    → 500 MB
 *     PREMIUM → 2048 MB (2 GB)
 *     FAMILY  → 5120 MB (5 GB)
 *
 * DOWNLOAD PRIORITY (when user queues a song):
 *   Priority 1 — Song is in one of the user's ACTIVE playlists
 *   Priority 2 — Song was recently played (last 7 days)
 *   Priority 3 — Song is liked by the user
 *   Priority 4 — Manually requested (default)
 *
 * QUOTA ENFORCEMENT (before downloading):
 *   1. Compute used MB = SUM(sizeMb) for all DOWNLOADED tracks
 *   2. If used + newTrack.sizeMb > quota:
 *        → Find least-recently-played DOWNLOADED tracks
 *        → Evict (mark EVICTED) until enough space is freed
 *        → Then proceed with download
 *   3. Mark track as DOWNLOADED with downloadedAt timestamp
 *
 * EVICTION ORDER:
 *   Sort DOWNLOADED tracks by lastPlayedOffline ASC (null = never played = evict first)
 */
public interface OfflineDownloadService {

    /** Queue a song for offline download. Handles priority assignment and quota. */
    OfflineTrack queueDownload(String userEmail, Long songId);

    /** Process the queue: download next batch, evict if necessary */
    List<OfflineTrack> processDownloadQueue(String userEmail);

    /** Get all offline tracks for a user (by status) */
    List<OfflineTrack> getOfflineTracks(String userEmail, String status);

    /** Remove a song from offline storage */
    void removeOfflineTrack(String userEmail, Long songId);

    /** Get storage usage summary */
    Map<String, Object> getStorageSummary(String userEmail);

    /** Mark an offline track as played (updates eviction order) */
    void markPlayedOffline(String userEmail, Long songId);
}
