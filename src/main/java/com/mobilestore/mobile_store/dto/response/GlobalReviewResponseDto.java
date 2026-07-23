package com.mobilestore.mobile_store.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalReviewResponseDto {
    private Long id;
    private String authorName;
    private Integer rating;
    private String title;
    private String text;
    private boolean verifiedPurchase;
    private LocalDateTime createdAt;
    private Long productId;
    private String productName;
    private String productBrand;
}
