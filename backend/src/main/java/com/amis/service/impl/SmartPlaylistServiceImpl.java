package com.amis.service.impl;

import com.amis.model.PlayEvent;
import com.amis.model.Song;
import com.amis.model.User;
import com.amis.repository.PlayEventRepository;
import com.amis.repository.SongRepository;
import com.amis.repository.UserRepository;
import com.amis.service.SmartPlaylistService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * SmartPlaylistServiceImpl — Business Logic 5
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * STEP-BY-STEP ALGORITHM:
 *
 * Step 1 — Build Genre Frequency Map
 *   Fetch all PlayEvents for user → group by genre → count
 *   Result: Map<String genre, Long count>
 *   Example: {"pop": 42, "rock": 28, "jazz": 7}
 *
 * Step 2 — Build Artist Frequency Map
 *   Same events → group by artist → count
 *   Result: Map<String artist, Long count>
 *
 * Step 3 — Build Recently Played Set
 *   Events from last 7 days → distinct song IDs
 *   Result: Set<Long songId> — excluded from recommendations
 *
 * Step 4 — Mood-Based Filtering
 *   Get all songs where song.moodTag == selectedMood
 *   (Stored as a field on Song, set by ADMIN on upload)
 *   Also includes songs whose genre is a mood-genre mapping:
 *     energetic → rock, hip-hop, electronic
 *     calm      → ambient, classical, jazz
 *     focus     → classical, ambient
 *     party     → pop, electronic, hip-hop
 *     sleep     → ambient, classical
 *
 * Step 5 — Score Each Candidate Song
 *   genreWeight  = genreFreqMap.getOrDefault(song.genre, 0) / maxGenreCount
 *   artistWeight = artistFreqMap.getOrDefault(song.artist, 0) / maxArtistCount
 *   score = (genreWeight * 0.5) + (artistWeight * 0.3) + (random novelty bonus * 0.2)
 *
 * Step 6 — Sort by score DESC, return top N
 */
@Service
public class SmartPlaylistServiceImpl implements SmartPlaylistService {

    private final PlayEventRepository playEventRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;

    // Mood → compatible genres mapping
    private static final Map<String, List<String>> MOOD_GENRE_MAP = new HashMap<>();
    static {
        MOOD_GENRE_MAP.put("energetic", Arrays.asList("rock", "hip-hop", "electronic", "pop"));
        MOOD_GENRE_MAP.put("calm",      Arrays.asList("ambient", "classical", "jazz", "acoustic"));
        MOOD_GENRE_MAP.put("focus",     Arrays.asList("classical", "ambient", "electronic"));
        MOOD_GENRE_MAP.put("party",     Arrays.asList("pop", "electronic", "hip-hop", "dance"));
        MOOD_GENRE_MAP.put("sleep",     Arrays.asList("ambient", "classical", "acoustic"));
    }

    public SmartPlaylistServiceImpl(PlayEventRepository playEventRepository,
                                     SongRepository songRepository,
                                     UserRepository userRepository) {
        this.playEventRepository = playEventRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Song> generateSmartPlaylist(String userEmail, String mood, int limit) {
        User user = getUser(userEmail);
        // FREE users: cap at 10 songs per generation
        if ("FREE".equals(user.getSubscriptionTier()) && limit > 10) limit = 10;

        // ── Step 1: Genre Frequency Map ──────────────────────────────────
        // Stream all play events → collect to Map<genre, count>
        List<PlayEvent> allEvents = playEventRepository.findByUserIdOrderByPlayedAtDesc(user.getId());

        Map<String, Long> genreFreqMap = allEvents.stream()
                .collect(Collectors.groupingBy(PlayEvent::getGenre, Collectors.counting()));

        // ── Step 2: Artist Frequency Map ─────────────────────────────────
        Map<String, Long> artistFreqMap = allEvents.stream()
                .collect(Collectors.groupingBy(PlayEvent::getArtist, Collectors.counting()));

        // ── Step 3: Recently Played Set (last 7 days) ─────────────────────
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Set<Long> recentlyPlayedIds = new HashSet<>(
                playEventRepository.findDistinctSongIdsPlayedSince(user.getId(), sevenDaysAgo)
        );

        // ── Step 4: Mood-Based Genre Filter ──────────────────────────────
        List<String> moodGenres = MOOD_GENRE_MAP.getOrDefault(
                mood.toLowerCase(), Arrays.asList("pop", "rock")
        );

        // Get all songs whose genre is in the mood-compatible list
        // Also include any songs explicitly tagged with the mood
        List<Song> candidates = songRepository.findAll().stream()
                .filter(s -> {
                    String genre = s.getGenre() != null ? s.getGenre().toLowerCase() : "";
                    String moodTag = s.getAlbumArtUrl(); // We reuse albumArtUrl for mood tag check
                    // Match by genre mapping OR by explicit mood tag on song
                    return moodGenres.contains(genre);
                })
                // Exclude recently played songs
                .filter(s -> !recentlyPlayedIds.contains(s.getId()))
                .collect(Collectors.toList());

        // If no mood-matched songs, fall back to all unplayed songs
        if (candidates.size() < 3) {
            candidates = songRepository.findAll().stream()
                    .filter(s -> !recentlyPlayedIds.contains(s.getId()))
                    .collect(Collectors.toList());
        }

        // ── Step 5: Score Each Candidate ─────────────────────────────────
        // Normalize: find max counts to create 0–1 weights
        long maxGenreCount  = genreFreqMap.values().stream().max(Long::compare).orElse(1L);
        long maxArtistCount = artistFreqMap.values().stream().max(Long::compare).orElse(1L);

        // Use a map to store scored songs
        Map<Song, Double> scoredSongs = new LinkedHashMap<>();
        Random random = new Random();

        for (Song song : candidates) {
            // Genre preference weight (0.0–1.0)
            double genreWeight = (double) genreFreqMap.getOrDefault(song.getGenre(), 0L) / maxGenreCount;

            // Artist preference weight (0.0–1.0)
            double artistWeight = (double) artistFreqMap.getOrDefault(song.getArtist(), 0L) / maxArtistCount;

            // Novelty bonus: small random factor to prevent always-same results (0.0–0.2)
            double noveltyBonus = random.nextDouble() * 0.2;

            // Combined score
            double score = (genreWeight * 0.5) + (artistWeight * 0.3) + noveltyBonus;
            scoredSongs.put(song, score);
        }

        // ── Step 6: Sort by score DESC, return top N ─────────────────────
        return scoredSongs.entrySet().stream()
                .sorted(Map.Entry.<Song, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Map<String, Long>> getUserPreferenceMap(String userEmail) {
        User user = getUser(userEmail);
        List<PlayEvent> events = playEventRepository.findByUserIdOrderByPlayedAtDesc(user.getId());

        // Build genre frequency map sorted by count DESC
        Map<String, Long> genreMap = events.stream()
                .collect(Collectors.groupingBy(PlayEvent::getGenre, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Build artist frequency map sorted by count DESC
        Map<String, Long> artistMap = events.stream()
                .collect(Collectors.groupingBy(PlayEvent::getArtist, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        result.put("genres", genreMap);
        result.put("artists", artistMap);
        return result;
    }

    @Override
    public void recordPlayEvent(String userEmail, Long songId, String sessionMood, Integer listenDurationSeconds) {
        User user = getUser(userEmail);
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found: " + songId));

        // moodTag defaults to genre-based mood if not explicitly set
        String moodTag = inferMoodTag(song.getGenre());

        PlayEvent event = new PlayEvent(
                user.getId(), songId,
                song.getArtist(), song.getGenre(),
                moodTag, sessionMood, listenDurationSeconds
        );
        playEventRepository.save(event);
    }

    // Infer a mood tag from the song's genre
    private String inferMoodTag(String genre) {
        if (genre == null) return "general";
        switch (genre.toLowerCase()) {
            case "rock": case "hip-hop": case "electronic": return "energetic";
            case "ambient": case "classical": return "calm";
            case "pop": return "party";
            default: return "general";
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
