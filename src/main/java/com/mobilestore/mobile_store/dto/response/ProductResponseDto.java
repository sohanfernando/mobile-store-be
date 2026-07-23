package com.mobilestore.mobile_store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {
    
    private Long id;
    private String name;
    private String brand;
    private String modelNumber;
    private String description;
    private String descriptionImageUrl;
    private BigDecimal price;
    private String category;
    private Integer ramGb;
    private Integer storageGb;
    private Integer warrantyPeriod;
    private List<ColorVariantResponseDto> colorVariants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
