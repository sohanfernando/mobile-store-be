package com.mobilestore.mobile_store.messaging;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        String orderNumber,
        String email,
        String name,
        BigDecimal total,
        int itemCount
) {
}
