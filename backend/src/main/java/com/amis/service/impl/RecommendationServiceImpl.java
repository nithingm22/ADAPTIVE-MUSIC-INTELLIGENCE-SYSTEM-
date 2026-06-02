package com.amis.service.impl;

import com.amis.dto.request.RecommendationRequest;
import com.amis.dto.response.RecommendationResponse;
import com.amis.model.Song;
import com.amis.model.User;
import com.amis.repository.SongRepository;
import com.amis.repository.UserHistoryRepository;
import com.amis.repository.UserRepository;
import com.amis.service.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RecommendationServiceImpl - The core AI recommendation engine.
 *
 * SCORING FORMULA:
 *   score = (playCount * 0.4) + (recency * 0.4) + (genreMatch * 0.2)
 *
 * ALGORITHM CONTROL:
 *   explorationLevel 0-100:
 *     - Low  → mostly familiar songs (user has played before)
 *     - High → mostly new/undiscovered songs
 *
 * RULE: 70% familiar + 30% new (adjusted by explorationLevel)
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final SongRepository songRepository;
    private final UserHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public RecommendationServiceImpl(SongRepository songRepository,
                                     UserHistoryRepository historyRepository,
                                     UserRepository userRepository) {
        this.songRepository = songRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<RecommendationResponse> getRecommendations(String userEmail, RecommendationRequest request) {
        // 1. Find the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        // 2. Get the user's top genres from history
        List<Object[]> topGenresRaw = historyRepository.findTopGenresByUser(user.getId());
        List<String> topGenres = topGenresRaw.stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());

        // 3. Get song IDs the user played in the last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Set<Long> recentlyPlayedIds = new HashSet<>(
                historyRepository.findSongIdsPlayedSince(user.getId(), thirtyDaysAgo)
        );

        // 4. Get all songs
        List<Song> allSongs = songRepository.findAll();

        // 5. Apply genre filter if specified
        if (request.getGenre() != null && !request.getGenre().isEmpty()) {
            allSongs = allSongs.stream()
                    .filter(s -> s.getGenre().equalsIgnoreCase(request.getGenre()))
                    .collect(Collectors.toList());
        }

        // 6. Determine time-of-day genre preference
        String timeOfDayGenre = getTimeOfDayGenreHint();

        // 7. Score each song
        Map<Song, Double> scoredSongs = new HashMap<>();
        for (Song song : allSongs) {
            double score = computeScore(song, recentlyPlayedIds, topGenres, timeOfDayGenre);
            scoredSongs.put(song, score);
        }

        // 8. Split songs into familiar (played before) and new (never played)
        List<Song> familiarSongs = allSongs.stream()
                .filter(s -> recentlyPlayedIds.contains(s.getId()))
                .sorted(Comparator.comparingDouble(s -> -scoredSongs.getOrDefault(s, 0.0)))
                .collect(Collectors.toList());

        List<Song> newSongs = allSongs.stream()
                .filter(s -> !recentlyPlayedIds.contains(s.getId()))
                .sorted(Comparator.comparingDouble(s -> -scoredSongs.getOrDefault(s, 0.0)))
                .collect(Collectors.toList());

        // 9. Apply exploration level to determine mix ratio
        // explorationLevel 0 = all familiar, 100 = all new
        int total = request.getLimit();
        int newCount = (int) Math.round(total * (request.getExplorationLevel() / 100.0));
        int familiarCount = total - newCount;

        // Ensure minimum of 30% new songs (baseline discovery)
        if (newCount < (int)(total * 0.3)) newCount = (int)(total * 0.3);

        // 10. Build final list
        List<RecommendationResponse> result = new ArrayList<>();

        // Add familiar songs
        familiarSongs.stream()
                .limit(familiarCount)
                .forEach(song -> result.add(new RecommendationResponse(
                        song,
                        buildFamiliarReason(song, topGenres),
                        scoredSongs.getOrDefault(song, 0.0),
                        "familiar"
                )));

        // Add new/discovery songs
        newSongs.stream()
                .limit(newCount)
                .forEach(song -> result.add(new RecommendationResponse(
                        song,
                        buildDiscoveryReason(song, topGenres),
                        scoredSongs.getOrDefault(song, 0.0),
                        "discovery"
                )));

        // Shuffle to avoid predictable ordering
        Collections.shuffle(result);

        return result.stream().limit(total).collect(Collectors.toList());
    }

    @Override
    public List<RecommendationResponse> getSurpriseRecommendations(String userEmail) {
        // "Surprise Me" picks random songs outside the user's usual genres
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Object[]> topGenresRaw = historyRepository.findTopGenresByUser(user.getId());
        Set<String> usualGenres = topGenresRaw.stream()
                .map(row -> ((String) row[0]).toLowerCase())
                .collect(Collectors.toSet());

        List<Song> allSongs = songRepository.findAll();

        // Prefer songs outside usual genres for maximum surprise
        List<Song> surpriseSongs = allSongs.stream()
                .filter(s -> !usualGenres.contains(s.getGenre().toLowerCase()))
                .collect(Collectors.toList());

        // If not enough outside-genre songs, use all songs
        if (surpriseSongs.size() < 5) {
            surpriseSongs = allSongs;
        }

        Collections.shuffle(surpriseSongs);

        return surpriseSongs.stream()
                .limit(10)
                .map(song -> new RecommendationResponse(
                        song,
                        "Stepping outside your comfort zone — try something new!",
                        Math.random() * 100,
                        "surprise"
                ))
                .collect(Collectors.toList());
    }

    /**
     * CORE SCORING FORMULA:
     * score = (normalizedPlayCount * 0.4) + (recencyBonus * 0.4) + (genreMatch * 0.2)
     */
    private double computeScore(Song song, Set<Long> recentlyPlayed,
                                 List<String> topGenres, String timeOfDayGenre) {
        // Normalize playCount to 0-100 range (cap at 10000)
        double normalizedPlayCount = Math.min(song.getPlayCount() / 100.0, 100.0);

        // Recency bonus: 100 if played recently, 0 if not
        double recencyBonus = recentlyPlayed.contains(song.getId()) ? 100.0 : 0.0;

        // Genre match score
        double genreScore = 0.0;
        if (!topGenres.isEmpty()) {
            int genreIndex = topGenres.indexOf(song.getGenre());
            if (genreIndex == 0) genreScore = 100.0;       // Favorite genre
            else if (genreIndex == 1) genreScore = 70.0;   // Second favorite
            else if (genreIndex > 1) genreScore = 40.0;    // Known genre
        }

        // Bonus for time-of-day match
        if (song.getGenre().equalsIgnoreCase(timeOfDayGenre)) {
            genreScore = Math.min(genreScore + 20, 100.0);
        }

        return (normalizedPlayCount * 0.4) + (recencyBonus * 0.4) + (genreScore * 0.2);
    }

    /**
     * Time-of-day context: different moods for different parts of the day.
     */
    private String getTimeOfDayGenreHint() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 6 && hour < 10) return "pop";       // Morning: energetic
        if (hour >= 10 && hour < 14) return "classical"; // Midday: focus
        if (hour >= 14 && hour < 18) return "jazz";      // Afternoon: relaxed
        if (hour >= 18 && hour < 22) return "rock";      // Evening: upbeat
        return "ambient";                                 // Night: chill
    }

    /**
     * Build a human-readable reason for a familiar song recommendation.
     */
    private String buildFamiliarReason(Song song, List<String> topGenres) {
        if (!topGenres.isEmpty() && song.getGenre().equalsIgnoreCase(topGenres.get(0))) {
            return "Because you love " + topGenres.get(0) + " music";
        }
        if (song.getPlayCount() > 100) {
            return "A popular track by " + song.getArtist() + " that matches your taste";
        }
        return "Because you've enjoyed similar songs before";
    }

    /**
     * Build a human-readable reason for a discovery song recommendation.
     */
    private String buildDiscoveryReason(Song song, List<String> topGenres) {
        if (!topGenres.isEmpty() && song.getGenre().equalsIgnoreCase(topGenres.get(0))) {
            return "A new " + song.getGenre() + " track you haven't heard yet";
        }
        return "Expanding your taste — " + song.getArtist() + " is trending in " + song.getGenre();
    }
}
