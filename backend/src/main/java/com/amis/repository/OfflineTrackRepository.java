package com.amis.repository;

import com.amis.model.OfflineTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * OfflineTrackRepository — Manages per-user offline download records.
 */
public interface OfflineTrackRepository extends JpaRepository<OfflineTrack, Long> {

    List<OfflineTrack> findByUserId(Long userId);

    List<OfflineTrack> findByUserIdAndStatus(Long userId, String status);

    Optional<OfflineTrack> findByUserIdAndSongId(Long userId, Long songId);

    boolean existsByUserIdAndSongId(Long userId, Long songId);

    /** Total MB currently stored offline by this user */
    @Query("SELECT COALESCE(SUM(o.sizeMb), 0) FROM OfflineTrack o WHERE o.userId = :uid AND o.status = 'DOWNLOADED'")
    double sumUsedStorageMb(@Param("uid") Long userId);

    /** Least-recently-played DOWNLOADED tracks (candidates for eviction) */
    List<OfflineTrack> findByUserIdAndStatusOrderByLastPlayedOfflineAsc(Long userId, String status);

    /** Downloads in priority order (1 first) */
    List<OfflineTrack> findByUserIdAndStatusOrderByPriorityAsc(Long userId, String status);
}
