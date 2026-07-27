package com.mobilestore.mobile_store.service;

import com.mobilestore.mobile_store.dto.request.CreateProductRequestDto;
import com.mobilestore.mobile_store.dto.request.UpdateProductRequestDto;
import com.mobilestore.mobile_store.dto.response.ProductResponseDto;

import java.util.List;

public interface ProductService {

    ProductResponseDto createProduct(CreateProductRequestDto request);

    ProductResponseDto getProductById(Long id);

    List<ProductResponseDto> getAllProducts(boolean includeInactive);

    ProductResponseDto updateProduct(Long id, UpdateProductRequestDto request);

    ProductResponseDto setProductActive(Long id, boolean active);

    void deleteProduct(Long id);
}
