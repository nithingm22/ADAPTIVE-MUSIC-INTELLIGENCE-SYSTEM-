package com.amis.repository;

import com.amis.model.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserHistoryRepository - Tracks what users have listened to.
 */
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {

    // Get recent history for a user (last N entries)
    List<UserHistory> findTop50ByUserIdOrderByPlayedAtDesc(Long userId);

    // Get song IDs played by user within a time window (for recency scoring)
    @Query("SELECT h.songId FROM UserHistory h WHERE h.userId = :userId AND h.playedAt >= :since")
    List<Long> findSongIdsPlayedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Count how many times a user played a specific song
    @Query("SELECT COUNT(h) FROM UserHistory h WHERE h.userId = :userId AND h.songId = :songId")
    long countUserPlaysByUserIdAndSongId(@Param("userId") Long userId, @Param("songId") Long songId);

    // Get user's most played genres
    @Query("SELECT s.genre, COUNT(h) as cnt FROM UserHistory h " +
           "JOIN Song s ON h.songId = s.id " +
           "WHERE h.userId = :userId " +
           "GROUP BY s.genre ORDER BY cnt DESC")
    List<Object[]> findTopGenresByUser(@Param("userId") Long userId);
}
