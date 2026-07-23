package com.mobilestore.mobile_store.service;

import java.util.List;

import com.mobilestore.mobile_store.dto.request.CreateReviewRequestDto;
import com.mobilestore.mobile_store.dto.response.ReviewResponseDto;

public interface ReviewService {
    List<ReviewResponseDto> getReviewsForProduct(Long productId);
    ReviewResponseDto addReview(Long productId, CreateReviewRequestDto request);
}
