package com.amis.service;

import com.amis.dto.request.RecommendationRequest;
import com.amis.dto.response.RecommendationResponse;

import java.util.List;

/**
 * RecommendationService - Core AI recommendation engine interface.
 * Implements context-aware, explainable, and algorithm-controlled recommendations.
 */
public interface RecommendationService {

    /**
     * Context-aware recommendations for a user.
     * Considers history, genre preference, and time of day.
     * explorationLevel controls how many new songs to include.
     */
    List<RecommendationResponse> getRecommendations(String userEmail, RecommendationRequest request);

    /**
     * "Surprise Me" - random discovery picks based on user's taste profile.
     */
    List<RecommendationResponse> getSurpriseRecommendations(String userEmail);
}
