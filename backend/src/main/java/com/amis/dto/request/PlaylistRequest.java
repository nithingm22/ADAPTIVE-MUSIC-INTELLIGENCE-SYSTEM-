package com.amis.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * PlaylistRequest - Data for creating a new playlist manually.
 */
public class PlaylistRequest {

    @NotBlank(message = "Playlist name is required")
    private String name;

    private String tag;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
