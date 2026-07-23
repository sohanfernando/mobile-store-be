package com.mobilestore.mobile_store.service;

import java.util.List;

import com.mobilestore.mobile_store.dto.response.AdminAnalyticsResponseDto;
import com.mobilestore.mobile_store.dto.response.CustomerResponseDto;
import com.mobilestore.mobile_store.dto.response.GlobalReviewResponseDto;

public interface AdminService {
    AdminAnalyticsResponseDto getAnalytics();
    List<GlobalReviewResponseDto> getAllReviews();
    void deleteReview(Long id);
    List<CustomerResponseDto> getAllCustomers();
    CustomerResponseDto toggleCustomerStatus(Long id);
}
