package com.amis.service;

import com.amis.model.Song;
import com.amis.model.UserHistory;

import java.util.List;

/**
 * UserService - Interface for user personalization features.
 * Handles listening history, liked songs, and user profile.
 */
public interface UserService {

    // Record that a user played a song
    UserHistory recordPlay(String userEmail, Long songId);

    // Get recently played songs for a user
    List<Song> getRecentlyPlayed(String userEmail);

    // Like a song
    void likeSong(String userEmail, Long songId);

    // Unlike a song
    void unlikeSong(String userEmail, Long songId);

    // Get all liked songs
    List<Song> getLikedSongs(String userEmail);

    // Check if a song is liked by user
    boolean isSongLiked(String userEmail, Long songId);

    // Get all users (admin only)
    List<com.amis.model.User> getAllUsers();
}
