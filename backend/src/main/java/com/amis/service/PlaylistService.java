package com.amis.service;

import com.amis.dto.request.PlaylistGenerateRequest;
import com.amis.dto.request.PlaylistRequest;
import com.amis.dto.response.PlaylistResponse;

import java.util.List;

/**
 * PlaylistService - Interface for playlist management and dynamic generation.
 */
public interface PlaylistService {

    PlaylistResponse createPlaylist(String userEmail, PlaylistRequest request);

    List<PlaylistResponse> getUserPlaylists(String userEmail);

    PlaylistResponse getPlaylistById(Long playlistId, String userEmail);

    void addSongToPlaylist(Long playlistId, Long songId, String userEmail);

    void removeSongFromPlaylist(Long playlistId, Long songId, String userEmail);

    /**
     * Dynamic Playlist Generation:
     * 70% familiar songs + 30% new songs based on mood/genre.
     */
    PlaylistResponse generatePlaylist(String userEmail, PlaylistGenerateRequest request);

    void deletePlaylist(Long playlistId, String userEmail);
}
