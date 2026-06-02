package com.amis.controller;

import com.amis.dto.request.RecommendationRequest;
import com.amis.dto.response.ApiResponse;
import com.amis.dto.response.RecommendationResponse;
import com.amis.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RecommendationController - Context-aware, explainable recommendations.
 */
@RestController
@RequestMapping("/recommendations")
@CrossOrigin(origins = "http://localhost:3000")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /** POST /recommendations - Get personalised recommendations */
    @PostMapping
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendations(
            @RequestBody RecommendationRequest request, Authentication auth) {
        try {
            List<RecommendationResponse> recs = recommendationService.getRecommendations(auth.getName(), request);
            return ResponseEntity.ok(ApiResponse.success("Recommendations ready", recs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** GET /recommendations/surprise - "Surprise Me" discovery mode */
    @GetMapping("/surprise")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getSurprise(Authentication auth) {
        try {
            List<RecommendationResponse> recs = recommendationService.getSurpriseRecommendations(auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Surprise picks ready", recs));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
