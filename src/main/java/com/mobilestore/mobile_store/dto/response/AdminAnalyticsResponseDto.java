package com.mobilestore.mobile_store.dto.response;

import java.math.BigDecimal;
import java.util.List;
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
public class AdminAnalyticsResponseDto {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long lowStockCount;
    private long totalCustomers;
    private List<CategoryShareResponseDto> categoryShare;
    private List<SalesTrendResponseDto> salesTrend;
}
