package com.amis.service;

import com.amis.dto.request.SongRequest;
import com.amis.model.Song;

import java.util.List;

/**
 * SongService - Interface for song CRUD and search operations.
 */
public interface SongService {

    Song createSong(SongRequest request);

    Song updateSong(Long id, SongRequest request);

    void deleteSong(Long id);

    Song getSongById(Long id);

    List<Song> getAllSongs();

    List<Song> searchSongs(String query);

    List<Song> getTrendingSongs();

    List<String> getAllGenres();

    // Increment play count when a song is played
    Song incrementPlayCount(Long songId);
}
