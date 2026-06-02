package com.amis.dto.response;

import com.amis.model.Song;

/**
 * RecommendationResponse - A single song recommendation with an explanation.
 * This is the core of the Explainable Recommendation System.
 */
public class RecommendationResponse {

    private Song song;

    // Human-readable explanation of why this song was recommended
    private String reason;

    // Computed relevance score (for transparency)
    private double score;

    // Tag: "familiar" or "discovery"
    private String type;

    public RecommendationResponse() {}

    public RecommendationResponse(Song song, String reason, double score, String type) {
        this.song = song;
        this.reason = reason;
        this.score = score;
        this.type = type;
    }

    // Getters and Setters
    public Song getSong() { return song; }
    public void setSong(Song song) { this.song = song; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
