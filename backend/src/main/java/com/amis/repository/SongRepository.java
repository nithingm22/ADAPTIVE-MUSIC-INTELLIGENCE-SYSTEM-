package com.amis.repository;

import com.amis.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SongRepository - Database operations for Song entity.
 */
public interface SongRepository extends JpaRepository<Song, Long> {

    // Find songs by genre (used in recommendation engine)
    List<Song> findByGenre(String genre);

    // Advanced search: ILIKE for case-insensitive partial match across title/artist/genre
    @Query("SELECT s FROM Song s WHERE " +
           "LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.artist) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.genre) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Song> searchSongs(@Param("query") String query);

    // Get top songs by playCount (trending)
    List<Song> findTop20ByOrderByPlayCountDesc();

    // Get songs that have been updated recently (for trending growth rate)
    @Query("SELECT s FROM Song s WHERE s.updatedAt >= :since ORDER BY s.playCount DESC")
    List<Song> findRecentlyActiveSongs(@Param("since") LocalDateTime since);

    // Get all distinct genres
    @Query("SELECT DISTINCT s.genre FROM Song s ORDER BY s.genre")
    List<String> findAllGenres();
}
