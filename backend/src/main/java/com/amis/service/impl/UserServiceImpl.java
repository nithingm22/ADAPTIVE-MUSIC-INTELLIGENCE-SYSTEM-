package com.amis.service.impl;

import com.amis.model.LikedSong;
import com.amis.model.Song;
import com.amis.model.User;
import com.amis.model.UserHistory;
import com.amis.repository.LikedSongRepository;
import com.amis.repository.SongRepository;
import com.amis.repository.UserHistoryRepository;
import com.amis.repository.UserRepository;
import com.amis.service.SongService;
import com.amis.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserHistoryRepository historyRepository;
    private final LikedSongRepository likedSongRepository;
    private final SongRepository songRepository;
    private final SongService songService;

    public UserServiceImpl(UserRepository userRepository,
                           UserHistoryRepository historyRepository,
                           LikedSongRepository likedSongRepository,
                           SongRepository songRepository,
                           SongService songService) {
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.likedSongRepository = likedSongRepository;
        this.songRepository = songRepository;
        this.songService = songService;
    }

    @Override
    public UserHistory recordPlay(String userEmail, Long songId) {
        User user = getUser(userEmail);
        // Increment global play count on the song
        songService.incrementPlayCount(songId);
        // Save to history
        UserHistory history = new UserHistory(user.getId(), songId);
        return historyRepository.save(history);
    }

    @Override
    public List<Song> getRecentlyPlayed(String userEmail) {
        User user = getUser(userEmail);
        List<UserHistory> recent = historyRepository.findTop50ByUserIdOrderByPlayedAtDesc(user.getId());
        // Map history entries to Song objects, preserving order and removing duplicates
        return recent.stream()
                .map(h -> songRepository.findById(h.getSongId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .limit(20)
                .collect(Collectors.toList());
    }

    @Override
    public void likeSong(String userEmail, Long songId) {
        User user = getUser(userEmail);
        if (!likedSongRepository.existsByUserIdAndSongId(user.getId(), songId)) {
            likedSongRepository.save(new LikedSong(user.getId(), songId));
        }
    }

    @Override
    @Transactional
    public void unlikeSong(String userEmail, Long songId) {
        User user = getUser(userEmail);
        likedSongRepository.deleteByUserIdAndSongId(user.getId(), songId);
    }

    @Override
    public List<Song> getLikedSongs(String userEmail) {
        User user = getUser(userEmail);
        List<LikedSong> liked = likedSongRepository.findByUserId(user.getId());
        return liked.stream()
                .map(ls -> songRepository.findById(ls.getSongId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSongLiked(String userEmail, Long songId) {
        User user = getUser(userEmail);
        return likedSongRepository.existsByUserIdAndSongId(user.getId(), songId);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
