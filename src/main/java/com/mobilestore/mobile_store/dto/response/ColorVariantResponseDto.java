package com.mobilestore.mobile_store.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColorVariantResponseDto {
    private Long id;
    private String color;
    private Integer stockQuantity;
    private List<String> images;
}
