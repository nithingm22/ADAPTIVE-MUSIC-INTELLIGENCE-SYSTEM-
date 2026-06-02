package com.amis.repository;

import com.amis.model.CollaborativePlaylist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * CollaborativePlaylistRepository — Manages shared playlist metadata.
 */
public interface CollaborativePlaylistRepository extends JpaRepository<CollaborativePlaylist, Long> {

    Optional<CollaborativePlaylist> findByPlaylistId(Long playlistId);

    boolean existsByPlaylistId(Long playlistId);
}
