package com.amis.service.impl;

import com.amis.dto.request.SongRequest;
import com.amis.model.Song;
import com.amis.repository.SongRepository;
import com.amis.service.SongService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SongServiceImpl - Implements song management and the Trending Songs Engine.
 *
 * TRENDING ALGORITHM:
 * Scores songs by recent play activity (last 7 days) + overall playCount.
 * growth_score = recent_plays * 0.7 + total_plays * 0.3
 */
@Service
public class SongServiceImpl implements SongService {

    private final SongRepository songRepository;

    public SongServiceImpl(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @Override
    public Song createSong(SongRequest request) {
        Song song = new Song(
            request.getTitle(),
            request.getArtist(),
            request.getGenre(),
            request.getDuration()
        );
        song.setAlbumArtUrl(request.getAlbumArtUrl());
        song.setAudioUrl(request.getAudioUrl());
        return songRepository.save(song);
    }

    @Override
    public Song updateSong(Long id, SongRequest request) {
        Song song = getSongById(id);
        song.setTitle(request.getTitle());
        song.setArtist(request.getArtist());
        song.setGenre(request.getGenre());
        song.setDuration(request.getDuration());
        if (request.getAlbumArtUrl() != null) song.setAlbumArtUrl(request.getAlbumArtUrl());
        if (request.getAudioUrl() != null) song.setAudioUrl(request.getAudioUrl());
        return songRepository.save(song);
    }

    @Override
    public void deleteSong(Long id) {
        Song song = getSongById(id);
        songRepository.delete(song);
    }

    @Override
    public Song getSongById(Long id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found with id: " + id));
    }

    @Override
    public List<Song> getAllSongs() {
        return songRepository.findAll();
    }

    @Override
    public List<Song> searchSongs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return songRepository.findAll();
        }
        return songRepository.searchSongs(query.trim());
    }

    /**
     * TRENDING SONGS ENGINE
     * Songs active in the last 7 days are scored by growth rate.
     * Falls back to all-time top songs if no recent activity.
     */
    @Override
    public List<Song> getTrendingSongs() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // Get songs that were recently played
        List<Song> recentlyActive = songRepository.findRecentlyActiveSongs(sevenDaysAgo);

        if (!recentlyActive.isEmpty()) {
            // Score: recency weight (position in list) * 0.7 + playCount * 0.3
            int size = recentlyActive.size();
            return recentlyActive.stream()
                    .sorted(Comparator.comparingDouble((Song s) ->
                            -(s.getPlayCount() * 0.3 + (size - recentlyActive.indexOf(s)) * 0.7))
                    )
                    .limit(20)
                    .collect(Collectors.toList());
        }

        // Fallback: top songs by all-time play count
        return songRepository.findTop20ByOrderByPlayCountDesc();
    }

    @Override
    public List<String> getAllGenres() {
        return songRepository.findAllGenres();
    }

    @Override
    public Song incrementPlayCount(Long songId) {
        Song song = getSongById(songId);
        song.setPlayCount(song.getPlayCount() + 1);
        return songRepository.save(song);
    }
}
