package com.mobilestore.mobile_store.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mobilestore.mobile_store.dto.request.CreateReviewRequestDto;
import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.dto.response.ReviewResponseDto;
import com.mobilestore.mobile_store.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ReviewResponseDto>>> getReviews(@PathVariable Long productId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsForProduct(productId);
        return ResponseEntity.ok(ApiResponseDto.success("Reviews fetched successfully", reviews));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto<ReviewResponseDto>> addReview(
            @PathVariable Long productId,
            @Valid @RequestBody CreateReviewRequestDto request) {
        ReviewResponseDto response = reviewService.addReview(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Review submitted successfully", response));
    }
}
