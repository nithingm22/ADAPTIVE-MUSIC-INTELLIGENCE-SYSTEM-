package com.amis.repository;

import com.amis.model.LikedSong;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * LikedSongRepository - Manages user's liked/favorited songs.
 */
public interface LikedSongRepository extends JpaRepository<LikedSong, Long> {

    List<LikedSong> findByUserId(Long userId);

    Optional<LikedSong> findByUserIdAndSongId(Long userId, Long songId);

    boolean existsByUserIdAndSongId(Long userId, Long songId);

    void deleteByUserIdAndSongId(Long userId, Long songId);
}
