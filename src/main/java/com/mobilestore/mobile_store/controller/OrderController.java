package com.mobilestore.mobile_store.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mobilestore.mobile_store.dto.request.CreateOrderRequestDto;
import com.mobilestore.mobile_store.dto.request.UpdateOrderStatusRequestDto;
import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.dto.response.OrderResponseDto;
import com.mobilestore.mobile_store.entity.Order;
import com.mobilestore.mobile_store.exception.OrderNotFoundException;
import com.mobilestore.mobile_store.repository.OrderRepository;
import com.mobilestore.mobile_store.service.OrderService;
import com.mobilestore.mobile_store.service.PdfService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PdfService pdfService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> createOrder(
            @Valid @RequestBody CreateOrderRequestDto request) {
        OrderResponseDto response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Order placed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<OrderResponseDto>>> getOrders(
            @RequestParam(required = false) String email) {
        List<OrderResponseDto> response = (email != null && !email.isBlank())
                ? orderService.getOrdersByEmail(email)
                : orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponseDto.success("Orders fetched successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponseDto<OrderResponseDto>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequestDto request) {
        OrderResponseDto response = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponseDto.success("Order status updated successfully", response));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadOrderPdf(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
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
