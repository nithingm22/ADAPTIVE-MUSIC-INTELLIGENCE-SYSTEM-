package com.amis.repository;

import com.amis.model.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * PlaylistSongRepository - Manages song-playlist relationships.
 */
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    List<PlaylistSong> findByPlaylistIdOrderByPosition(Long playlistId);

    void deleteByPlaylistIdAndSongId(Long playlistId, Long songId);

    boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);
}
