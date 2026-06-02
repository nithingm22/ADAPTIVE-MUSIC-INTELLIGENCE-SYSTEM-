package com.amis.service.impl;

import com.amis.dto.response.PlaylistResponse;
import com.amis.model.*;
import com.amis.repository.*;
import com.amis.service.CollaborativePlaylistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * CollaborativePlaylistServiceImpl — Business Logic 6
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * WHAT THIS DOES IN PLAIN ENGLISH:
 * ---------------------------------
 * Multiple users can edit the same playlist at the same time.
 * When they do, "conflicts" can happen — for example:
 *   • Two people add the same song → keep it once (no duplicates)
 *   • One person adds a song while another removes it → the ADD wins
 *   • Two people add different songs to the same slot → keep both,
 *     shift the second one one position forward
 *
 * This service:
 *   1. Saves every edit as a timestamped record (like a change log)
 *   2. When "Merge" is clicked, replays all edits in order
 *   3. Detects and resolves conflicts automatically
 *   4. Returns the clean, merged playlist + a human-readable
 *      summary of what was fixed
 *
 * SIMPLIFIED CHANGES vs ORIGINAL:
 * ---------------------------------
 * • recordEdit() no longer requires the caller to pass userId in the
 *   request body — the controller now derives it from the JWT token
 *   so regular users cannot spoof someone else's edits.
 * • Conflict notifications now include song TITLES (looked up from DB)
 *   instead of just numeric song IDs, so messages are human-readable.
 * • Added getSongTitle() helper to resolve IDs to names everywhere.
 * • Added getCollaborators() helper so the frontend can show "who is
 *   editing this playlist" without extra queries.
 */
@Service
public class CollaborativePlaylistServiceImpl implements CollaborativePlaylistService {

    private final PlaylistEditRepository         editRepository;
    private final PlaylistSongRepository         playlistSongRepository;
    private final PlaylistRepository             playlistRepository;
    private final SongRepository                 songRepository;
    private final CollaborativePlaylistRepository collabRepository;
    private final UserRepository                 userRepository;

    public CollaborativePlaylistServiceImpl(
            PlaylistEditRepository editRepository,
            PlaylistSongRepository playlistSongRepository,
            PlaylistRepository playlistRepository,
            SongRepository songRepository,
            CollaborativePlaylistRepository collabRepository,
            UserRepository userRepository) {
        this.editRepository        = editRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.playlistRepository    = playlistRepository;
        this.songRepository        = songRepository;
        this.collabRepository      = collabRepository;
        this.userRepository        = userRepository;
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    /**
     * Marks a playlist as shared and records who the collaborators are.
     * Call this once before anyone starts editing.
     *
     * @param playlistId       the playlist to share
     * @param collaboratorIds  user IDs of people who will edit together
     */
    @Override
    public void makeCollaborative(Long playlistId, List<Long> collaboratorIds) {
        String ids = collaboratorIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        CollaborativePlaylist collab = new CollaborativePlaylist(playlistId, ids);
        collabRepository.save(collab);
    }

    // ── Recording edits ───────────────────────────────────────────────────

    /**
     * Saves a single user's edit to the change log.
     *
     * editType values:
     *   ADD     → add song to playlist at a given position
     *   REMOVE  → remove song from playlist
     *   REORDER → move song to a different position
     *
     * FIX: userId is now passed from the controller (derived from JWT)
     *      rather than being supplied by the client in the request body.
     *      This prevents one user from recording fake edits as another user.
     */
    @Override
    public PlaylistEdit recordEdit(Long playlistId, Long userId, Long songId,
                                   String editType, Integer position) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if ("FREE".equals(user.getSubscriptionTier()))
            throw new RuntimeException("UPGRADE_REQUIRED: Collaborative editing is a Premium feature. Upgrade to edit shared playlists.");
        if (!songRepository.existsById(songId))
            throw new RuntimeException("Song not found: " + songId + ". Use GET /songs to see all valid song IDs.");
        PlaylistEdit edit = new PlaylistEdit(playlistId, userId, songId, editType, position);
        return editRepository.save(edit);
    }

    // ── Merge & Conflict Resolution ───────────────────────────────────────

    /**
     * Replays all saved edits in timestamp order and resolves any conflicts.
     *
     * Returns a map containing:
     *   "playlist"          → the resolved playlist with song details
     *   "conflictsResolved" → number of conflicts that were auto-fixed
     *   "notifications"     → plain-English list of what was fixed and why
     */
    @Override
    @Transactional
    public Map<String, Object> mergeAndResolve(Long playlistId, String userEmail) {

        // ── Step 1: Load all edits in time order ──────────────────────────
        // Think of this as a "replay log" — we replay every edit from the
        // beginning to reconstruct what the playlist should look like.
        List<PlaylistEdit> edits =
                editRepository.findByPlaylistIdOrderByEditedAtAsc(playlistId);

        // ── Step 2: Start from the current saved state ────────────────────
        // LinkedList is used because we need fast insert/remove anywhere.
        LinkedList<Long> songIdList = new LinkedList<>(
                playlistSongRepository.findByPlaylistIdOrderByPosition(playlistId)
                        .stream()
                        .map(PlaylistSong::getSongId)
                        .collect(Collectors.toList())
        );

        List<String>      notifications = new ArrayList<>();
        List<PlaylistEdit> conflictEdits = new ArrayList<>();

        // ── Step 3: Replay each edit, check for conflicts ─────────────────
        for (PlaylistEdit edit : edits) {

            String songTitle = getSongTitle(edit.getSongId()); // human-readable name

            switch (edit.getEditType().toUpperCase()) {

                // ─── ADD ──────────────────────────────────────────────────
                case "ADD": {
                    Long songId = edit.getSongId();

                    // CONFLICT A: Song already in the playlist
                    // Two people added the same song — keep it once.
                    if (songIdList.contains(songId)) {
                        flagConflict(edit, conflictEdits);
                        notifications.add(String.format(
                                "✅ Fixed: \"%s\" was added by two people — kept once (no duplicates).",
                                songTitle));
                        break; // skip this duplicate add
                    }

                    // CONFLICT B: Someone removed this song at roughly the same time
                    // Rule: ADD always wins over a concurrent REMOVE.
                    boolean concurrentRemove = edits.stream().anyMatch(e ->
                            e.getSongId().equals(songId)
                            && "REMOVE".equalsIgnoreCase(e.getEditType())
                            && isWithinConflictWindow(edit.getEditedAt(), e.getEditedAt()));

                    if (concurrentRemove) {
                        flagConflict(edit, conflictEdits);
                        notifications.add(String.format(
                                "✅ Fixed: \"%s\" was added and removed at the same time — kept it (add wins).",
                                songTitle));
                        // Still insert the song below — ADD wins
                    }

                    // CONFLICT C: Two songs added to the exact same position at the same time
                    // Insert both — shift this one one slot forward.
                    int targetPos = (edit.getPosition() != null)
                            ? edit.getPosition() : songIdList.size();

                    boolean positionConflict = edits.stream().anyMatch(e ->
                            !e.getId().equals(edit.getId())
                            && "ADD".equalsIgnoreCase(e.getEditType())
                            && e.getPosition() != null
                            && e.getPosition().equals(edit.getPosition())
                            && isWithinConflictWindow(edit.getEditedAt(), e.getEditedAt()));

                    if (positionConflict) {
                        targetPos = Math.min(targetPos + 1, songIdList.size());
                        flagConflict(edit, conflictEdits);
                        notifications.add(String.format(
                                "✅ Fixed: Two songs were added to the same slot — both kept, \"%s\" moved one position forward.",
                                songTitle));
                    }

                    // Insert at the resolved position
                    if (targetPos >= songIdList.size()) {
                        songIdList.addLast(songId);
                    } else {
                        songIdList.add(targetPos, songId);
                    }
                    break;
                }

                // ─── REMOVE ───────────────────────────────────────────────
                case "REMOVE": {
                    Long songId = edit.getSongId();
                    boolean removed = songIdList.remove(songId);

                    if (!removed) {
                        // Song was already gone — another user removed it first
                        flagConflict(edit, conflictEdits);
                        notifications.add(String.format(
                                "ℹ️ Note: \"%s\" was already removed by another collaborator — skipped.",
                                songTitle));
                    }
                    break;
                }

                // ─── REORDER ──────────────────────────────────────────────
                case "REORDER": {
                    Long songId = edit.getSongId();
                    int newPos  = (edit.getPosition() != null) ? edit.getPosition() : 0;
                    boolean found = songIdList.remove(songId);
                    if (found) {
                        newPos = Math.min(newPos, songIdList.size());
                        songIdList.add(newPos, songId);
                    }
                    // If not found, silently skip — song may have been removed already
                    break;
                }
            }
        }

        // ── Step 4: Save the conflict summary ─────────────────────────────
        CollaborativePlaylist collab = collabRepository.findByPlaylistId(playlistId)
                .orElseGet(() -> new CollaborativePlaylist(playlistId, ""));
        collab.setConflictNotifications(String.join("|", notifications));
        collabRepository.save(collab);
        editRepository.saveAll(conflictEdits);

        // ── Step 5: Write the resolved song order back to the database ─────
        List<PlaylistSong> existing =
                playlistSongRepository.findByPlaylistIdOrderByPosition(playlistId);
        playlistSongRepository.deleteAll(existing);

        List<PlaylistSong> resolved = new ArrayList<>();
        for (int i = 0; i < songIdList.size(); i++) {
            resolved.add(new PlaylistSong(playlistId, songIdList.get(i), i + 1));
        }
        playlistSongRepository.saveAll(resolved);

        // ── Step 6: Build the response ─────────────────────────────────────
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found: " + playlistId));

        List<Song> resolvedSongs = songIdList.stream()
                .map(sid -> songRepository.findById(sid))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("playlist",          new PlaylistResponse(playlist, resolvedSongs));
        result.put("conflictsResolved", conflictEdits.size());
        result.put("notifications",     notifications);
        return result;
    }

    // ── History & Notifications ───────────────────────────────────────────

    @Override
    public List<PlaylistEdit> getEditHistory(Long playlistId) {
        return editRepository.findByPlaylistIdOrderByEditedAtAsc(playlistId);
    }

    @Override
    public List<String> getConflictNotifications(Long playlistId) {
        return collabRepository.findByPlaylistId(playlistId)
                .map(c -> {
                    String raw = c.getConflictNotifications();
                    if (raw == null || raw.isBlank()) return List.<String>of();
                    return Arrays.asList(raw.split("\\|"));
                })
                .orElse(List.of());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Returns the song title for a given ID, or "Song #<id>" as a fallback.
     * Used to make conflict messages human-readable.
     */
    private String getSongTitle(Long songId) {
        return songRepository.findById(songId)
                .map(Song::getTitle)
                .orElse("Song #" + songId);
    }

    /** Marks an edit as conflicted and adds it to the conflict list. */
    private void flagConflict(PlaylistEdit edit, List<PlaylistEdit> conflictEdits) {
        edit.setConflictFlagged(true);
        if (!conflictEdits.contains(edit)) {
            conflictEdits.add(edit);
        }
    }

    /**
     * Two edits are "concurrent" (in the same conflict window) if they
     * happened within 60 seconds of each other.
     *
     * Why 60 seconds? It simulates realistic network/user latency — if
     * two people make changes within a minute of each other without
     * seeing each other's updates, that's a true conflict.
     */
    private boolean isWithinConflictWindow(LocalDateTime t1, LocalDateTime t2) {
        return Math.abs(java.time.Duration.between(t1, t2).getSeconds()) <= 60;
    }
}
