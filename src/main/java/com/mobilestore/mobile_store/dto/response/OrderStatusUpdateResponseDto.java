package com.mobilestore.mobile_store.dto.response;

/**
 * emailSent is null when the status change didn't trigger a notification email at all
 * (e.g. setting the same status it already had); true/false otherwise.
 */
public record OrderStatusUpdateResponseDto(OrderResponseDto order, Boolean emailSent) {
}
