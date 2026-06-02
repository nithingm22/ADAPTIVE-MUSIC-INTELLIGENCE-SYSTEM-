package com.amis.repository;

import com.amis.model.PlayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PlayEventRepository — Rich play history for Smart Playlist + Analytics engines.
 */
public interface PlayEventRepository extends JpaRepository<PlayEvent, Long> {

    // All events for a user, newest first
    List<PlayEvent> findByUserIdOrderByPlayedAtDesc(Long userId);

    // Events in a time window (for 7-day streak and window analysis)
    @Query("SELECT e FROM PlayEvent e WHERE e.userId = :uid AND e.playedAt >= :since ORDER BY e.playedAt DESC")
    List<PlayEvent> findByUserIdSince(@Param("uid") Long userId, @Param("since") LocalDateTime since);

    // Song IDs played by user in the last N days (for recently-played exclusion)
    @Query("SELECT DISTINCT e.songId FROM PlayEvent e WHERE e.userId = :uid AND e.playedAt >= :since")
    List<Long> findDistinctSongIdsPlayedSince(@Param("uid") Long userId, @Param("since") LocalDateTime since);

    // Distinct days the user listened (for streak calculation)
    @Query("SELECT DISTINCT CAST(e.playedAt AS date) FROM PlayEvent e WHERE e.userId = :uid AND e.playedAt >= :since")
    List<Object> findDistinctListeningDays(@Param("uid") Long userId, @Param("since") LocalDateTime since);

    // Total seconds listened by user in a window (for weekly hours)
    @Query("SELECT COALESCE(SUM(e.listenDurationSeconds), 0) FROM PlayEvent e WHERE e.userId = :uid AND e.playedAt >= :since")
    Long sumListenDurationSince(@Param("uid") Long userId, @Param("since") LocalDateTime since);

    // All global events for a song in a time window (for trending)
    @Query("SELECT COUNT(e) FROM PlayEvent e WHERE e.songId = :sid AND e.playedAt BETWEEN :from AND :to")
    long countPlaysInWindow(@Param("sid") Long songId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
