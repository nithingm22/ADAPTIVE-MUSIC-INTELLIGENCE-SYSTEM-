package com.amis.service;

import com.amis.model.Song;

import java.util.List;
import java.util.Map;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * BUSINESS LOGIC 5: Smart Playlist Generator
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * HOW IT WORKS:
 * 1. Analyze the user's full play history → build frequency maps:
 *      Map<Genre, PlayCount>   — which genres they listen to most
 *      Map<Artist, PlayCount>  — which artists they prefer
 * 2. Accept a mood parameter: "energetic" | "calm" | "focus"
 * 3. Filter the song catalog:
 *      - song.moodTag must match selected mood (Set<MoodTag> filter)
 *      - song.genre must appear in user's top genres
 *      - song.artist gets bonus scoring if in user's top artists
 * 4. Exclude recently played songs (within last 7 days) using
 *      Set<RecentlyPlayedId>
 * 5. Rank remaining songs by combined score:
 *      score = (genre_preference_weight * 0.5) + (artist_preference_weight * 0.3) + (recency_penalty * 0.2)
 * 6. Return top N songs as a smart playlist
 */
public interface SmartPlaylistService {

    /**
     * Generate a mood + history-aware song list.
     *
     * @param userEmail  authenticated user
     * @param mood       "energetic" | "calm" | "focus" | "party" | "sleep"
     * @param limit      number of songs to return
     * @return ranked list of songs matching mood and user preferences
     */
    List<Song> generateSmartPlaylist(String userEmail, String mood, int limit);

    /**
     * Get the user's preference frequency maps.
     * Exposed for the Analytics dashboard.
     *
     * @return Map with "genres" and "artists" keys → frequency maps
     */
    Map<String, Map<String, Long>> getUserPreferenceMap(String userEmail);

    /**
     * Record a rich play event (used instead of simple history recording).
     * Captures mood context, artist, genre, and listen duration.
     */
    void recordPlayEvent(String userEmail, Long songId, String sessionMood, Integer listenDurationSeconds);
}
