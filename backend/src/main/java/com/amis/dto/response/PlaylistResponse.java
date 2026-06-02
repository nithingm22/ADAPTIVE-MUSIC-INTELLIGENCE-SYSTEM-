package com.amis.dto.response;

import com.amis.model.Playlist;
import com.amis.model.Song;

import java.util.List;

/**
 * PlaylistResponse - A playlist along with its songs.
 */
public class PlaylistResponse {

    private Playlist playlist;
    private List<Song> songs;

    public PlaylistResponse() {}

    public PlaylistResponse(Playlist playlist, List<Song> songs) {
        this.playlist = playlist;
        this.songs = songs;
    }

    // Getters and Setters
    public Playlist getPlaylist() { return playlist; }
    public void setPlaylist(Playlist playlist) { this.playlist = playlist; }

    public List<Song> getSongs() { return songs; }
    public void setSongs(List<Song> songs) { this.songs = songs; }
}
