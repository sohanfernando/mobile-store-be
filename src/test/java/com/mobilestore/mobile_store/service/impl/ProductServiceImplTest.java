package com.mobilestore.mobile_store.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.mobilestore.mobile_store.dto.request.ColorVariantRequestDto;
import com.mobilestore.mobile_store.dto.request.UpdateProductRequestDto;
import com.mobilestore.mobile_store.entity.Product;
import com.mobilestore.mobile_store.entity.ProductColorVariant;
import com.mobilestore.mobile_store.exception.ProductInUseException;
import com.mobilestore.mobile_store.exception.ProductNotFoundException;
import com.mobilestore.mobile_store.mapper.ProductMapper;
import com.mobilestore.mobile_store.repository.ProductRepository;
import com.mobilestore.mobile_store.storage.FileStorageService;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private FileStorageService fileStorageService;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, new ProductMapper(), fileStorageService);
    }

    private Product productWithVariantImages(List<String> images) {
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .brand("Acme")
                .price(BigDecimal.valueOf(1000))
                .build();
        ProductColorVariant variant = ProductColorVariant.builder()
                .id(10L)
                .color("Black")
                .stockQuantity(5)
                .images(new java.util.ArrayList<>(images))
                .product(product)
                .build();
        product.getColorVariants().add(variant);
        return product;
    }

    @Test
    void deleteProductRemovesAllOfItsImagesAfterSuccessfulDelete() {
        Product product = productWithVariantImages(List.of("/uploads/products/a.png", "/uploads/products/b.png"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        service.deleteProduct(1L);

        verify(fileStorageService, times(1)).delete("/uploads/products/a.png");
        verify(fileStorageService, times(1)).delete("/uploads/products/b.png");
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProductDoesNotDeleteImagesWhenProductIsInUse() {
        Product product = productWithVariantImages(List.of("/uploads/products/a.png"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("in use"))
                .when(productRepository).delete(product);

        assertThrows(ProductInUseException.class, () -> service.deleteProduct(1L));

        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void deleteProductThrowsWhenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.deleteProduct(99L));
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void updateProductDeletesOnlyImagesRemovedFromTheVariant() {
        Product product = productWithVariantImages(List.of("/uploads/products/a.png", "/uploads/products/b.png"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductRequestDto request = new UpdateProductRequestDto();
        request.setName("Phone");
        request.setBrand("Acme");
        request.setPrice(BigDecimal.valueOf(1000));
        request.setColorVariants(List.of(ColorVariantRequestDto.builder()
                .id(10L)
                .color("Black")
                .stockQuantity(5)
                .images(List.of("/uploads/products/a.png"))
                .build()));

        service.updateProduct(1L, request);

        verify(fileStorageService, times(1)).delete("/uploads/products/b.png");
        verify(fileStorageService, never()).delete("/uploads/products/a.png");
    }
}
