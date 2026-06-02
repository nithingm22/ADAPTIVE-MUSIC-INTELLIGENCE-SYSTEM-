package com.amis.repository;

import com.amis.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * PlaylistRepository - Database operations for Playlist entity.
 */
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // Get all playlists owned by a user
    List<Playlist> findByUserId(Long userId);

    // Get playlists by tag (e.g., "pop", "chill")
    List<Playlist> findByUserIdAndTag(Long userId, String tag);
}
