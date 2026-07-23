package com.mobilestore.mobile_store.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mobilestore.mobile_store.dto.request.CreateReviewRequestDto;
import com.mobilestore.mobile_store.dto.response.ReviewResponseDto;
import com.mobilestore.mobile_store.entity.Product;
import com.mobilestore.mobile_store.entity.Review;
import com.mobilestore.mobile_store.exception.ProductNotFoundException;
import com.mobilestore.mobile_store.repository.OrderRepository;
import com.mobilestore.mobile_store.repository.ProductRepository;
import com.mobilestore.mobile_store.repository.ReviewRepository;
import com.mobilestore.mobile_store.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsForProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public ReviewResponseDto addReview(Long productId, CreateReviewRequestDto request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        boolean verifiedPurchase = orderRepository.findByCustomerEmailOrderByCreatedAtDesc(request.getAuthorEmail())
                .stream()
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> item.getProduct().getId().equals(productId));

        Review review = Review.builder()
                .product(product)
                .authorEmail(request.getAuthorEmail())
                .authorName(request.getAuthorName())
                .rating(request.getRating())
                .title(request.getTitle())
                .text(request.getText())
                .verifiedPurchase(verifiedPurchase)
                .build();

        Review saved = reviewRepository.save(review);
        return toResponseDto(saved);
    }

    private ReviewResponseDto toResponseDto(Review review) {
        return ReviewResponseDto.builder()
                .id(review.getId())
                .authorName(review.getAuthorName())
                .rating(review.getRating())
                .title(review.getTitle())
                .text(review.getText())
                .verifiedPurchase(review.isVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
