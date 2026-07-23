package com.mobilestore.mobile_store.mapper;

import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.dto.request.CreateProductRequestDto;
import com.mobilestore.mobile_store.dto.request.UpdateProductRequestDto;
import com.mobilestore.mobile_store.dto.response.ProductResponseDto;
import com.mobilestore.mobile_store.dto.response.ColorVariantResponseDto;
import com.mobilestore.mobile_store.entity.Product;
import com.mobilestore.mobile_store.entity.ProductColorVariant;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequestDto request){
        Product product = Product.builder()
                .name(request.getName())
                .brand(request.getBrand())
                .modelNumber(request.getModelNumber())
                .description(request.getDescription())
                .descriptionImageUrl(request.getDescriptionImageUrl())
                .price(request.getPrice())
                .category(request.getCategory())
                .ramGb(request.getRamGb())
                .storageGb(request.getStorageGb())
                .warrantyPeriod(request.getWarrantyPeriod())
                .build();

        if (request.getColorVariants() != null) {
            for (var dto : request.getColorVariants()) {
                product.getColorVariants().add(ProductColorVariant.builder()
                        .color(dto.getColor())
                        .stockQuantity(dto.getStockQuantity())
                        .images(dto.getImages())
                        .product(product)
                        .build());
            }
        }
        return product;
    }

    public void updateEntityFromRequest(UpdateProductRequestDto request, Product product){
        product.setName(request.getName());
        product.setBrand(request.getBrand());
        product.setModelNumber(request.getModelNumber());
        product.setDescription(request.getDescription());
        product.setDescriptionImageUrl(request.getDescriptionImageUrl());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setRamGb(request.getRamGb());
        product.setStorageGb(request.getStorageGb());
        product.setWarrantyPeriod(request.getWarrantyPeriod());

        // Reconcile color variants by id so unchanged/edited variants are updated in place
        // (rather than deleted and reinserted), which would fail if they're referenced by past orders.
        Map<Long, ProductColorVariant> existingById = product.getColorVariants().stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ProductColorVariant::getId, v -> v));

        List<ProductColorVariant> reconciled = new ArrayList<>();
        if (request.getColorVariants() != null) {
            for (var dto : request.getColorVariants()) {
                ProductColorVariant variant = dto.getId() != null ? existingById.get(dto.getId()) : null;
                if (variant != null) {
                    variant.setColor(dto.getColor());
                    variant.setStockQuantity(dto.getStockQuantity());
                    variant.getImages().clear();
                    if (dto.getImages() != null) {
                        variant.getImages().addAll(dto.getImages());
                    }
                } else {
                    variant = ProductColorVariant.builder()
                            .color(dto.getColor())
                            .stockQuantity(dto.getStockQuantity())
                            .images(dto.getImages())
                            .product(product)
                            .build();
                }
                reconciled.add(variant);
            }
        }

        product.getColorVariants().clear();
        product.getColorVariants().addAll(reconciled);
    }

    public ProductResponseDto toResponseDto(Product product){
        List<ColorVariantResponseDto> variants = new ArrayList<>();
        if (product.getColorVariants() != null) {
            for (var v : product.getColorVariants()) {
                variants.add(ColorVariantResponseDto.builder()
                        .id(v.getId())
                        .color(v.getColor())
                        .stockQuantity(v.getStockQuantity())
                        .images(v.getImages())
                        .build());
            }
        }

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .modelNumber(product.getModelNumber())
                .description(product.getDescription())
                .descriptionImageUrl(product.getDescriptionImageUrl())
                .price(product.getPrice())
                .category(product.getCategory())
                .ramGb(product.getRamGb())
                .storageGb(product.getStorageGb())
                .warrantyPeriod(product.getWarrantyPeriod())
                .colorVariants(variants)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
