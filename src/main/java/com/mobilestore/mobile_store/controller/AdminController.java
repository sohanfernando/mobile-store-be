package com.mobilestore.mobile_store.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mobilestore.mobile_store.dto.response.AdminAnalyticsResponseDto;
import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.dto.response.CustomerResponseDto;
import com.mobilestore.mobile_store.dto.response.GlobalReviewResponseDto;
import com.mobilestore.mobile_store.service.AdminService;
import com.mobilestore.mobile_store.service.PdfService;
import com.mobilestore.mobile_store.repository.OrderRepository;
import com.mobilestore.mobile_store.entity.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PdfService pdfService;
    private final OrderRepository orderRepository;

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponseDto<AdminAnalyticsResponseDto>> getAnalytics() {
        AdminAnalyticsResponseDto response = adminService.getAnalytics();
        return ResponseEntity.ok(ApiResponseDto.success("Analytics data fetched successfully", response));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponseDto<List<GlobalReviewResponseDto>>> getAllReviews() {
        List<GlobalReviewResponseDto> response = adminService.getAllReviews();
        return ResponseEntity.ok(ApiResponseDto.success("All product reviews fetched successfully", response));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
        return ResponseEntity.ok(ApiResponseDto.success("Review deleted successfully", null));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponseDto<List<CustomerResponseDto>>> getAllCustomers() {
        List<CustomerResponseDto> response = adminService.getAllCustomers();
        return ResponseEntity.ok(ApiResponseDto.success("Customer registry fetched successfully", response));
    }

    @PatchMapping("/customers/{id}/toggle-status")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> toggleCustomerStatus(@PathVariable Long id) {
        CustomerResponseDto response = adminService.toggleCustomerStatus(id);
        return ResponseEntity.ok(ApiResponseDto.success("Customer status toggled successfully", response));
    }

    @GetMapping("/orders/{id}/pdf")
    public ResponseEntity<byte[]> downloadOrderPdf(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new com.mobilestore.mobile_store.exception.OrderNotFoundException(id));
        byte[] pdfBytes = pdfService.generateInvoicePdf(order);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + order.getOrderNumber() + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
