package com.amis.service.impl;

import com.amis.model.OfflineTrack;
import com.amis.model.Song;
import com.amis.model.User;
import com.amis.repository.*;
import com.amis.service.OfflineDownloadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * OfflineDownloadServiceImpl — Business Logic 7
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * QUOTA MAP (subscription tier → storage limit in MB):
 *   FREE    → 500 MB
 *   PREMIUM → 2048 MB  (2 GB)
 *   FAMILY  → 5120 MB  (5 GB)
 *
 * PRIORITY ASSIGNMENT when a song is queued:
 *   1 = song is in any of the user's playlists  (highest)
 *   2 = song was recently played (last 7 days)
 *   3 = song is liked by the user
 *   4 = manual / default                        (lowest)
 *
 * QUOTA ENFORCEMENT during processDownloadQueue():
 *   - usedMb = SUM(sizeMb) WHERE status = 'DOWNLOADED'
 *   - For each QUEUED track (sorted priority ASC = highest first):
 *       if usedMb + track.sizeMb <= quotaMb  →  mark DOWNLOADED
 *       else  →  evict least-recently-played tracks until space freed,
 *                then download
 *
 * EVICTION ORDER:
 *   Sort DOWNLOADED tracks by lastPlayedOffline ASC
 *   NULL lastPlayedOffline = never played offline = evicted first
 *
 * ─────────────────────────────────────────────────────────
 * BUGS FIXED IN THIS VERSION:
 *
 * BUG 1 — Evicted songs could never be re-downloaded.
 *   Root cause: queueDownload() checked existsByUserIdAndSongId()
 *   and returned the existing EVICTED record without re-queuing.
 *   Fix: if the found record has status EVICTED, reset it to QUEUED
 *        and re-assign its priority.
 *
 * BUG 2 — processDownloadQueue() always used the FREE quota (500 MB)
 *   even for PREMIUM/FAMILY users.
 *   Root cause: quota was hardcoded to QUOTA_MAP.get("FREE").
 *   Fix: read user.getSubscriptionTier() and look up the correct quota.
 *        (Requires the subscriptionTier column added to User entity.)
 * ─────────────────────────────────────────────────────────
 */
@Service
public class OfflineDownloadServiceImpl implements OfflineDownloadService {

    // Subscription tier → storage limit in MB
    private static final Map<String, Double> QUOTA_MAP = new HashMap<>();
    static {
        QUOTA_MAP.put("FREE",    500.0);
        QUOTA_MAP.put("PREMIUM", 2048.0);
        QUOTA_MAP.put("FAMILY",  5120.0);
    }

    // Average simulated song size (3.5–6 MB range based on duration)
    private static final double AVG_SONG_SIZE_MB = 4.5;

    private final OfflineTrackRepository  offlineTrackRepository;
    private final SongRepository          songRepository;
    private final UserRepository          userRepository;
    private final PlaylistRepository      playlistRepository;
    private final PlaylistSongRepository  playlistSongRepository;
    private final LikedSongRepository     likedSongRepository;
    private final UserHistoryRepository   historyRepository;

    public OfflineDownloadServiceImpl(
            OfflineTrackRepository offlineTrackRepository,
            SongRepository songRepository,
            UserRepository userRepository,
            PlaylistRepository playlistRepository,
            PlaylistSongRepository playlistSongRepository,
            LikedSongRepository likedSongRepository,
            UserHistoryRepository historyRepository) {
        this.offlineTrackRepository = offlineTrackRepository;
        this.songRepository         = songRepository;
        this.userRepository         = userRepository;
        this.playlistRepository     = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.likedSongRepository    = likedSongRepository;
        this.historyRepository      = historyRepository;
    }

    // ── Queue a song for download ─────────────────────────────────────────

    @Override
    public OfflineTrack queueDownload(String userEmail, Long songId) {
        User user = getUser(userEmail);

        Optional<OfflineTrack> existing =
                offlineTrackRepository.findByUserIdAndSongId(user.getId(), songId);

        if (existing.isPresent()) {
            OfflineTrack track = existing.get();
            if ("EVICTED".equals(track.getStatus())) {
                // Check FREE queue limit before re-queuing
                if ("FREE".equals(user.getSubscriptionTier())) enforceQueueLimit(user);
                track.setStatus("QUEUED");
                track.setPriority(determinePriority(user.getId(), songId));
                track.setDownloadedAt(null);
                track.setLastPlayedOffline(null);
                return offlineTrackRepository.save(track);
            }
            return track;
        }

        // New download — check FREE limit
        if ("FREE".equals(user.getSubscriptionTier())) enforceQueueLimit(user);
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found: " + songId));

        int    priority = determinePriority(user.getId(), songId);
        double sizeMb   = song.getDuration() != null
                ? Math.max(3.0, song.getDuration() / 60.0 * AVG_SONG_SIZE_MB)
                : AVG_SONG_SIZE_MB;

        OfflineTrack track = new OfflineTrack(user.getId(), songId, sizeMb, priority);
        return offlineTrackRepository.save(track);
    }

    // ── Process the download queue ────────────────────────────────────────

    @Override
    @Transactional
    public List<OfflineTrack> processDownloadQueue(String userEmail) {
        User user = getUser(userEmail);

        // ── FIX BUG 2 ────────────────────────────────────────────────────
        // Previously: always called QUOTA_MAP.get("FREE") — every user was
        // capped at 500 MB regardless of their subscription tier.
        // Now: read the user's actual subscriptionTier from the database.
        double quotaMb = getUserQuota(user);

        double usedMb = offlineTrackRepository.sumUsedStorageMb(user.getId());

        List<OfflineTrack> queued = offlineTrackRepository
                .findByUserIdAndStatusOrderByPriorityAsc(user.getId(), "QUEUED");

        List<OfflineTrack> justDownloaded = new ArrayList<>();

        for (OfflineTrack track : queued) {
            if (usedMb + track.getSizeMb() <= quotaMb) {
                // Enough space — download directly
                markDownloaded(track);
                usedMb += track.getSizeMb();
                justDownloaded.add(track);
            } else {
                // Need to evict least-recently-played tracks first
                double needed = track.getSizeMb() - (quotaMb - usedMb);
                double freed  = evictUntilFree(user.getId(), needed);

                if (freed >= 0) {
                    usedMb = offlineTrackRepository.sumUsedStorageMb(user.getId());
                    markDownloaded(track);
                    usedMb += track.getSizeMb();
                    justDownloaded.add(track);
                }
                // If not enough evictable tracks exist, skip this track
            }
        }

        return justDownloaded;
    }

    // ── List offline tracks ───────────────────────────────────────────────

    @Override
    public List<OfflineTrack> getOfflineTracks(String userEmail, String status) {
        User user = getUser(userEmail);
        if (status != null && !status.isBlank()) {
            return offlineTrackRepository.findByUserIdAndStatus(
                    user.getId(), status.toUpperCase());
        }
        return offlineTrackRepository.findByUserId(user.getId());
    }

    // ── Manual delete (mark EVICTED) ──────────────────────────────────────

    @Override
    @Transactional
    public void removeOfflineTrack(String userEmail, Long songId) {
        User user = getUser(userEmail);
        offlineTrackRepository.findByUserIdAndSongId(user.getId(), songId)
                .ifPresent(track -> {
                    track.setStatus("EVICTED");
                    offlineTrackRepository.save(track);
                });
    }

    // ── Storage summary ───────────────────────────────────────────────────

    @Override
    public Map<String, Object> getStorageSummary(String userEmail) {
        User   user        = getUser(userEmail);
        double quotaMb     = getUserQuota(user);                   // FIX BUG 2
        double usedMb      = offlineTrackRepository.sumUsedStorageMb(user.getId());
        double availableMb = quotaMb - usedMb;

        long downloaded = offlineTrackRepository
                .findByUserIdAndStatus(user.getId(), "DOWNLOADED").size();
        long queued = offlineTrackRepository
                .findByUserIdAndStatus(user.getId(), "QUEUED").size();
        long evicted = offlineTrackRepository
                .findByUserIdAndStatus(user.getId(), "EVICTED").size();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("subscriptionTier",  user.getSubscriptionTier());  // FIX BUG 2
        summary.put("quotaMb",           quotaMb);
        summary.put("usedMb",            Math.round(usedMb      * 10.0) / 10.0);
        summary.put("availableMb",       Math.round(availableMb * 10.0) / 10.0);
        summary.put("usedPercent",       Math.round((usedMb / quotaMb) * 100));
        summary.put("downloadedTracks",  downloaded);
        summary.put("queuedTracks",      queued);
        summary.put("evictedTracks",     evicted);
        return summary;
    }

    // ── Mark played offline (updates LRU timestamp) ───────────────────────

    @Override
    public void markPlayedOffline(String userEmail, Long songId) {
        User user = getUser(userEmail);
        offlineTrackRepository.findByUserIdAndSongId(user.getId(), songId)
                .ifPresent(track -> {
                    track.setLastPlayedOffline(LocalDateTime.now());
                    offlineTrackRepository.save(track);
                });
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /** Throws if FREE user already has 3 active (QUEUED+DOWNLOADED) tracks. */
    private void enforceQueueLimit(User user) {
        long active = offlineTrackRepository.findByUserIdAndStatus(user.getId(), "DOWNLOADED").size()
                    + offlineTrackRepository.findByUserIdAndStatus(user.getId(), "QUEUED").size();
        if (active >= 3)
            throw new RuntimeException("UPGRADE_REQUIRED: Free users can have up to 3 offline songs. Upgrade to Premium for unlimited downloads.");
    }

    /**
     * Returns the storage quota in MB for a user based on their subscription tier.
     * Falls back to FREE (500 MB) for any unrecognised tier string.
     */
    private double getUserQuota(User user) {
        String tier = user.getSubscriptionTier();
        return QUOTA_MAP.getOrDefault(tier != null ? tier.toUpperCase() : "FREE",
                                     QUOTA_MAP.get("FREE"));
    }

    /**
     * Assigns download priority based on the song's relationship to the user.
     *   1 = song is in one of the user's playlists   (highest priority)
     *   2 = recently played (last 7 days)
     *   3 = liked by the user
     *   4 = manual / default                         (lowest priority)
     */
    private int determinePriority(Long userId, Long songId) {
        boolean inPlaylist = playlistRepository.findByUserId(userId).stream()
                .anyMatch(pl -> playlistSongRepository
                        .existsByPlaylistIdAndSongId(pl.getId(), songId));
        if (inPlaylist) return 1;

        long recentPlays = historyRepository.countUserPlaysByUserIdAndSongId(userId, songId);
        if (recentPlays > 0) return 2;

        if (likedSongRepository.existsByUserIdAndSongId(userId, songId)) return 3;

        return 4;
    }

    /** Mark a track as DOWNLOADED and record the timestamp. */
    private void markDownloaded(OfflineTrack track) {
        track.setStatus("DOWNLOADED");
        track.setDownloadedAt(LocalDateTime.now());
        offlineTrackRepository.save(track);
    }

    /**
     * Evict least-recently-played DOWNLOADED tracks until `neededMb` MB is freed.
     * Returns the total MB freed, or -1 if there are not enough evictable tracks.
     *
     * Eviction order: lastPlayedOffline ASC — tracks that were never played
     * offline (null) are evicted first because they haven't been used.
     */
    private double evictUntilFree(Long userId, double neededMb) {
        List<OfflineTrack> candidates = offlineTrackRepository
                .findByUserIdAndStatusOrderByLastPlayedOfflineAsc(userId, "DOWNLOADED");

        double freed = 0.0;
        for (OfflineTrack candidate : candidates) {
            candidate.setStatus("EVICTED");
            offlineTrackRepository.save(candidate);
            freed += candidate.getSizeMb();
            if (freed >= neededMb) return freed;
        }
        return freed >= neededMb ? freed : -1;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
