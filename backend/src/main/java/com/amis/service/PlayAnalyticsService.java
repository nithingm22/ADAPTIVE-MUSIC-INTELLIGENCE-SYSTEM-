package com.amis.service;

import com.amis.model.PlayStats;

import java.util.List;
import java.util.Map;

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * BUSINESS LOGIC 8: Play Count Analytics & Trending Tracker
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * HOW IT WORKS:
 *
 * MAP STRUCTURE: Map<TrackId, PlayStats>
 *   Each PlayStats holds totalPlays, playsThisWeek, playsLastWeek.
 *
 * TRENDING ALGORITHM:
 *   trendingDelta = playsThisWeek - playsLastWeek
 *   Tracks with highest positive delta → most trending
 *   Tracks with negative delta → declining
 *   Window resets every 7 days (weekWindowStart tracked in entity)
 *
 * ARTIST AGGREGATION:
 *   Group all PlayStats by artist → sum totalPlays per artist
 *   Returns top N artists by play count
 *
 * LISTENING STREAK:
 *   Query distinct calendar days with at least 1 play event
 *   Count consecutive days backward from today
 *   Example: played Mon, Tue, Wed → streak = 3
 *
 * WEEKLY LISTENING HOURS:
 *   Sum(listenDurationSeconds) for all PlayEvents in last 7 days
 *   Convert to hours: totalSeconds / 3600.0
 */
public interface PlayAnalyticsService {

    /** Record a play event and update PlayStats for that track */
    PlayStats recordPlay(Long songId, String songTitle, String artist, String genre, Long userId);

    /** Get trending tracks sorted by trendingDelta DESC */
    List<PlayStats> getTrendingTracks(int limit);

    /** Get all-time top tracks by totalPlays */
    List<PlayStats> getTopTracks(int limit);

    /** Artist-level aggregation: Map<ArtistName, TotalPlays> */
    Map<String, Long> getArtistAggregation();

    /** Genre-level aggregation: Map<Genre, TotalPlays> */
    Map<String, Long> getGenreAggregation();

    /**
     * User listening streak: consecutive days with at least one play.
     * Returns number of consecutive days ending today.
     */
    int getListeningStreak(String userEmail);

    /**
     * Weekly listening hours for a user.
     * Sum of listenDurationSeconds / 3600.0 for the last 7 days.
     */
    double getWeeklyListeningHours(String userEmail);

    /** Full personal analytics dashboard data */
    Map<String, Object> getPersonalDashboard(String userEmail);
}
