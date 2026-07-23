package com.mobilestore.mobile_store.controller;

import com.mobilestore.mobile_store.dto.request.CreateProductRequestDto;
import com.mobilestore.mobile_store.dto.request.UpdateProductRequestDto;
import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.dto.response.ProductResponseDto;
import com.mobilestore.mobile_store.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> createProduct(
            @Valid @RequestBody CreateProductRequestDto request) {
        ProductResponseDto response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Product created Successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> getProductById(@PathVariable Long id) {
        ProductResponseDto response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponseDto.success("Product fetched successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ProductResponseDto>>> getAllProducts() {
        List<ProductResponseDto> response = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponseDto.success("Products fetched successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequestDto request) {
        ProductResponseDto response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponseDto.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponseDto.success("Product deleted successfully", null));
    }
}
