package com.amis.service.impl;

import com.amis.dto.request.PlaylistGenerateRequest;
import com.amis.dto.request.PlaylistRequest;
import com.amis.dto.response.PlaylistResponse;
import com.amis.model.Playlist;
import com.amis.model.PlaylistSong;
import com.amis.model.Song;
import com.amis.model.User;
import com.amis.repository.*;
import com.amis.service.PlaylistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PlaylistServiceImpl - Creates, manages, and dynamically generates playlists.
 *
 * DYNAMIC GENERATION RULE:
 * 70% familiar songs (user has played) + 30% new songs
 * Filtered by mood (genre tag) if provided.
 */
@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final UserHistoryRepository historyRepository;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository,
                                PlaylistSongRepository playlistSongRepository,
                                SongRepository songRepository,
                                UserRepository userRepository,
                                UserHistoryRepository historyRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public PlaylistResponse createPlaylist(String userEmail, PlaylistRequest request) {
        User user = getUser(userEmail);
        // FREE users: max 3 playlists
        if ("FREE".equals(user.getSubscriptionTier())) {
            long count = playlistRepository.findByUserId(user.getId()).size();
            if (count >= 3)
                throw new RuntimeException("UPGRADE_REQUIRED: Free users can create up to 3 playlists. Upgrade to Premium for unlimited playlists.");
        }
        Playlist playlist = new Playlist(request.getName(), user.getId(), request.getTag());
        playlistRepository.save(playlist);
        return new PlaylistResponse(playlist, Collections.emptyList());
    }

    @Override
    public List<PlaylistResponse> getUserPlaylists(String userEmail) {
        User user = getUser(userEmail);
        List<Playlist> playlists = playlistRepository.findByUserId(user.getId());

        // Use stream + lambda to build response list
        return playlists.stream()
                .map(playlist -> {
                    List<Song> songs = getSongsForPlaylist(playlist.getId());
                    return new PlaylistResponse(playlist, songs);
                })
                .collect(Collectors.toList());
    }

    @Override
    public PlaylistResponse getPlaylistById(Long playlistId, String userEmail) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found: " + playlistId));
        List<Song> songs = getSongsForPlaylist(playlistId);
        return new PlaylistResponse(playlist, songs);
    }

    @Override
    public void addSongToPlaylist(Long playlistId, Long songId, String userEmail) {
        // Verify playlist exists
        playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found: " + playlistId));

        // Avoid duplicates
        if (!playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, songId)) {
            int position = playlistSongRepository.findByPlaylistIdOrderByPosition(playlistId).size() + 1;
            playlistSongRepository.save(new PlaylistSong(playlistId, songId, position));
        }
    }

    @Override
    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long songId, String userEmail) {
        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
    }

    /**
     * DYNAMIC PLAYLIST GENERATOR
     * Rule: 70% familiar + 30% new songs
     * Filter by mood/genre if specified.
     */
    @Override
    public PlaylistResponse generatePlaylist(String userEmail, PlaylistGenerateRequest request) {
        User user = getUser(userEmail);

        // Get songs user has played in the last 60 days
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        Set<Long> playedSongIds = new HashSet<>(
                historyRepository.findSongIdsPlayedSince(user.getId(), sixtyDaysAgo)
        );

        // Get candidate songs (filtered by genre/mood if specified)
        List<Song> candidates;
        String tag = request.getMood() != null ? request.getMood() :
                     request.getGenre() != null ? request.getGenre() : null;

        if (tag != null) {
            candidates = songRepository.findByGenre(tag);
            // If not enough, broaden to all songs
            if (candidates.size() < request.getSongCount()) {
                candidates = songRepository.findAll();
            }
        } else {
            candidates = songRepository.findAll();
        }

        // Split into familiar and new
        List<Song> familiarSongs = candidates.stream()
                .filter(s -> playedSongIds.contains(s.getId()))
                .collect(Collectors.toList());

        List<Song> newSongs = candidates.stream()
                .filter(s -> !playedSongIds.contains(s.getId()))
                .collect(Collectors.toList());

        // Shuffle both lists
        Collections.shuffle(familiarSongs);
        Collections.shuffle(newSongs);

        // Apply 70/30 rule
        int total = request.getSongCount();
        int familiarCount = (int) (total * 0.7);
        int newCount = total - familiarCount;

        List<Song> selectedSongs = new ArrayList<>();
        selectedSongs.addAll(familiarSongs.stream().limit(familiarCount).collect(Collectors.toList()));
        selectedSongs.addAll(newSongs.stream().limit(newCount).collect(Collectors.toList()));

        // Shuffle the final list
        Collections.shuffle(selectedSongs);

        // Save playlist
        Playlist playlist = new Playlist(request.getName(), user.getId(), tag);
        playlistRepository.save(playlist);

        // Save songs to playlist
        for (int i = 0; i < selectedSongs.size(); i++) {
            playlistSongRepository.save(new PlaylistSong(playlist.getId(), selectedSongs.get(i).getId(), i + 1));
        }

        return new PlaylistResponse(playlist, selectedSongs);
    }

    @Override
    @Transactional
    public void deletePlaylist(Long playlistId, String userEmail) {
        playlistRepository.deleteById(playlistId);
    }

    // Helper: get all songs in a playlist ordered by position
    private List<Song> getSongsForPlaylist(Long playlistId) {
        List<PlaylistSong> playlistSongs = playlistSongRepository
                .findByPlaylistIdOrderByPosition(playlistId);

        return playlistSongs.stream()
                .map(ps -> songRepository.findById(ps.getSongId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    // Helper: get user by email
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
