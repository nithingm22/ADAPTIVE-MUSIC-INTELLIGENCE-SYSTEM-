package com.amis.service.impl;

import com.amis.model.PlayStats;
import com.amis.model.User;
import com.amis.repository.PlayEventRepository;
import com.amis.repository.PlayStatsRepository;
import com.amis.repository.UserRepository;
import com.amis.service.PlayAnalyticsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * PlayAnalyticsServiceImpl — Business Logic 8
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * DATA STRUCTURE: Map<TrackId, PlayStats>
 * (Here, PlayStats is persisted in the DB, keyed by songId)
 *
 * TRENDING ALGORITHM (recomputed on every play):
 *   1. Count plays in THIS 7-day window (now to 7 days ago)
 *   2. Count plays in PRIOR 7-day window (7–14 days ago)
 *   3. trendingDelta = thisWeek - priorWeek
 *   4. Sort all tracks by trendingDelta DESC → trending chart
 *
 * WEEKLY WINDOW RESET:
 *   If LocalDateTime.now() is > 7 days after weekWindowStart:
 *     → playsLastWeek = playsThisWeek (rotate)
 *     → playsThisWeek = 0
 *     → weekWindowStart = now()
 *
 * LISTENING STREAK:
 *   1. Fetch distinct calendar dates with plays in last 90 days
 *   2. Start from today, count backward while consecutive days have plays
 *   3. Example: {Mon, Tue, Wed, Fri} checked from Fri → streak = 1 (Thu missing)
 *
 * WEEKLY LISTENING HOURS:
 *   SUM(listenDurationSeconds) WHERE playedAt >= 7 days ago
 *   / 3600.0 = hours
 */
@Service
public class PlayAnalyticsServiceImpl implements PlayAnalyticsService {

    private final PlayStatsRepository playStatsRepository;
    private final PlayEventRepository playEventRepository;
    private final UserRepository userRepository;

    public PlayAnalyticsServiceImpl(PlayStatsRepository playStatsRepository,
                                     PlayEventRepository playEventRepository,
                                     UserRepository userRepository) {
        this.playStatsRepository = playStatsRepository;
        this.playEventRepository = playEventRepository;
        this.userRepository = userRepository;
    }

    @Override
    public PlayStats recordPlay(Long songId, String songTitle, String artist, String genre, Long userId) {

        // Get or create PlayStats for this track
        PlayStats stats = playStatsRepository.findBySongId(songId)
                .orElseGet(() -> {
                    PlayStats s = new PlayStats(songId, songTitle, artist, genre);
                    return playStatsRepository.save(s);
                });

        // ── Weekly Window Rotation ────────────────────────────────────────
        // If more than 7 days since window started → rotate
        if (stats.getWeekWindowStart() == null ||
                stats.getWeekWindowStart().isBefore(LocalDateTime.now().minusDays(7))) {
            stats.setPlaysLastWeek(stats.getPlaysThisWeek()); // rotate
            stats.setPlaysThisWeek(0);
            stats.setWeekWindowStart(LocalDateTime.now());
        }

        // ── Increment Counts ──────────────────────────────────────────────
        stats.setTotalPlays(stats.getTotalPlays() + 1);
        stats.setPlaysThisWeek(stats.getPlaysThisWeek() + 1);
        stats.setLastPlayedAt(LocalDateTime.now());

        // ── Recompute Trending Delta ──────────────────────────────────────
        // Count plays from PlayEvent table for precise 7-day windows
        LocalDateTime now = LocalDateTime.now();
        long thisWeekPlays = playEventRepository.countPlaysInWindow(
                songId, now.minusDays(7), now);
        long priorWeekPlays = playEventRepository.countPlaysInWindow(
                songId, now.minusDays(14), now.minusDays(7));

        // trendingDelta: positive = growing, negative = declining
        long trendingDelta = thisWeekPlays - priorWeekPlays;
        stats.setPlaysThisWeek(thisWeekPlays);
        stats.setPlaysLastWeek(priorWeekPlays);
        stats.setTrendingDelta(trendingDelta);

        return playStatsRepository.save(stats);
    }

    @Override
    public List<PlayStats> getTrendingTracks(int limit) {
        // Sorted by trendingDelta DESC — highest growth rate first
        return playStatsRepository.findTop20ByOrderByTrendingDeltaDesc()
                .stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<PlayStats> getTopTracks(int limit) {
        return playStatsRepository.findTop20ByOrderByTotalPlaysDesc()
                .stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getArtistAggregation() {
        // ── Artist-Level Aggregation ─────────────────────────────────────
        // Group all PlayStats by artist → sum totalPlays → sort DESC
        List<Object[]> raw = playStatsRepository.aggregateByArtist();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : raw) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public Map<String, Long> getGenreAggregation() {
        // Group all PlayStats by genre → sum totalPlays → sort DESC
        return playStatsRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        s -> s.getGenre() != null ? s.getGenre() : "Unknown",
                        Collectors.summingLong(PlayStats::getTotalPlays)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public int getListeningStreak(String userEmail) {
        User user = getUser(userEmail);
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);

        // Fetch distinct calendar days where user listened
        List<Object> rawDays = playEventRepository
                .findDistinctListeningDays(user.getId(), ninetyDaysAgo);

        // Convert to Set<LocalDate> for O(1) lookup
        Set<LocalDate> listeningDays = new HashSet<>();
        for (Object day : rawDays) {
            if (day instanceof java.sql.Date) {
                listeningDays.add(((java.sql.Date) day).toLocalDate());
            } else if (day instanceof LocalDate) {
                listeningDays.add((LocalDate) day);
            }
        }

        // ── Streak Calculation ────────────────────────────────────────────
        // Count consecutive days backward from today
        int streak = 0;
        LocalDate check = LocalDate.now();

        while (listeningDays.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }

        return streak;
    }

    @Override
    public double getWeeklyListeningHours(String userEmail) {
        User user = getUser(userEmail);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // Sum all listen duration seconds in the last 7 days
        Long totalSeconds = playEventRepository.sumListenDurationSince(user.getId(), sevenDaysAgo);
        if (totalSeconds == null || totalSeconds == 0) return 0.0;

        // Convert to hours with 1 decimal place
        double hours = totalSeconds / 3600.0;
        return Math.round(hours * 10.0) / 10.0;
    }

    @Override
    public Map<String, Object> getPersonalDashboard(String userEmail) {
        // ── Personal Dashboard Data ───────────────────────────────────────
        // Combines streak, weekly hours, genre breakdown, top artists
        Map<String, Object> dashboard = new LinkedHashMap<>();

        dashboard.put("listeningStreak",    getListeningStreak(userEmail));
        dashboard.put("weeklyHours",        getWeeklyListeningHours(userEmail));

        // Trending this week
        List<PlayStats> trending = getTrendingTracks(5);
        dashboard.put("trendingTracks", trending);

        // Genre breakdown (global)
        dashboard.put("genreBreakdown", getGenreAggregation());

        // Artist breakdown (global top 5)
        Map<String, Long> artists = getArtistAggregation();
        Map<String, Long> topArtists = artists.entrySet().stream()
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
        dashboard.put("topArtists", topArtists);

        return dashboard;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
