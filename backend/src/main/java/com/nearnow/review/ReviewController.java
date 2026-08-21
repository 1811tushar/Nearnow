package com.nearnow.review;

import com.nearnow.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // Protected (needs Authentication to know who's submitting + for
    // the purchase-check) — see SecurityConfig for the explicit path
    // that keeps this OUT of the public list, unlike GET below.
    @PostMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> submitReview(
            Authentication authentication, @PathVariable Long productId,
            @Valid @RequestBody ReviewRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.submitReview(authentication.getName(), productId, request), "Review submitted"));
    }

    // Public — anyone browsing a product should see its reviews, same
    // reasoning as Product/Category being fully public.
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponseDTO>>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewsByProduct(productId)));
    }
}
