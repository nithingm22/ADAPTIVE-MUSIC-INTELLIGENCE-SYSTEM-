package com.amis.dto.request;

/**
 * RecommendationRequest - Parameters for the recommendation engine.
 * explorationLevel: 0 = familiar songs only, 100 = discover new songs
 */
public class RecommendationRequest {

    // 0 to 100: controls how adventurous recommendations are
    private int explorationLevel = 50;

    // Optional genre filter
    private String genre;

    // Number of recommendations to return
    private int limit = 10;

    // Getters and Setters
    public int getExplorationLevel() { return explorationLevel; }
    public void setExplorationLevel(int explorationLevel) { this.explorationLevel = explorationLevel; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
