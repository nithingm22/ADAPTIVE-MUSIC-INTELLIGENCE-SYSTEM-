package com.amis.repository;

import com.amis.model.PlayStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * PlayStatsRepository — Global per-track play analytics.
 */
public interface PlayStatsRepository extends JpaRepository<PlayStats, Long> {

    Optional<PlayStats> findBySongId(Long songId);

    /** Top trending by positive delta (this week vs last week) */
    List<PlayStats> findTop20ByOrderByTrendingDeltaDesc();

    /** Top by all-time total plays */
    List<PlayStats> findTop20ByOrderByTotalPlaysDesc();

    /** All stats for an artist */
    List<PlayStats> findByArtistIgnoreCase(String artist);

    /** All stats for a genre */
    List<PlayStats> findByGenreIgnoreCase(String genre);

    /** Artist aggregation: sum plays grouped by artist */
    @Query("SELECT p.artist, SUM(p.totalPlays) as total FROM PlayStats p GROUP BY p.artist ORDER BY total DESC")
    List<Object[]> aggregateByArtist();
}
