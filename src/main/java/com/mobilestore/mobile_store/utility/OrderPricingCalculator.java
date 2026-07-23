package com.mobilestore.mobile_store.utility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.dto.request.OrderItemRequestDto;
import com.mobilestore.mobile_store.entity.Coupon;
import com.mobilestore.mobile_store.entity.DiscountType;
import com.mobilestore.mobile_store.entity.ProductColorVariant;
import com.mobilestore.mobile_store.exception.ProductVariantNotFoundException;
import com.mobilestore.mobile_store.repository.CouponRedemptionRepository;
import com.mobilestore.mobile_store.repository.CouponRepository;
import com.mobilestore.mobile_store.repository.ProductColorVariantRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderPricingCalculator {

    private static final BigDecimal PICKUP_SHIPPING_COST = BigDecimal.ZERO;
    private static final BigDecimal HEAVY_SHIPPING_COST = BigDecimal.valueOf(200);

    private final ProductColorVariantRepository productColorVariantRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    public record ResolvedOrderItem(ProductColorVariant variant, Integer quantity, BigDecimal lineTotal) {
    }

    public record PricingResult(BigDecimal subtotal, BigDecimal shippingCost, BigDecimal discountAmount,
            Coupon appliedCoupon, BigDecimal total, List<ResolvedOrderItem> items) {
    }

    public PricingResult calculate(String shippingMethod, List<OrderItemRequestDto> items) {
        return calculate(shippingMethod, items, null, null, false);
    }

    public PricingResult calculate(String shippingMethod, List<OrderItemRequestDto> items, String couponCode) {
        return calculate(shippingMethod, items, couponCode, null, false);
    }

    /**
     * @param customerEmail used only to enforce "one redemption per customer per coupon". Pass the
     *                      same email at payment-intent-preview time and at order-creation time so
     *                      both stages agree on eligibility - otherwise a customer could be charged
     *                      a discounted amount that order creation then refuses to honor.
     * @param lockForUpdate pass true only when this call is part of actually committing an order
     *                      (i.e. stock/coupon usage are about to be decremented/incremented), so
     *                      concurrent purchases racing on the same variant or coupon serialize
     *                      instead of both reading stale state. Pricing previews (e.g. payment
     *                      intent creation) should leave this false.
     */
    public PricingResult calculate(String shippingMethod, List<OrderItemRequestDto> items, String couponCode,
            String customerEmail, boolean lockForUpdate) {
        List<ResolvedOrderItem> resolvedItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequestDto itemRequest : items) {
            ProductColorVariant variant = (lockForUpdate
                    ? productColorVariantRepository.findByIdForUpdate(itemRequest.getVariantId())
                    : productColorVariantRepository.findById(itemRequest.getVariantId()))
                    .orElseThrow(() -> new ProductVariantNotFoundException(itemRequest.getVariantId()));

            BigDecimal lineTotal = variant.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            resolvedItems.add(new ResolvedOrderItem(variant, itemRequest.getQuantity(), lineTotal));
        }

        BigDecimal discount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Coupon coupon = (lockForUpdate
                    ? couponRepository.findByCodeIgnoreCaseForUpdate(couponCode.trim())
                    : couponRepository.findByCodeIgnoreCase(couponCode.trim()))
                    .orElse(null);

            if (coupon != null && isEligible(coupon, subtotal, customerEmail)) {
                discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                        ? subtotal.multiply(coupon.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                        : coupon.getDiscountValue();
                appliedCoupon = coupon;
            }
        }

        BigDecimal shippingCost = resolveShippingCost(shippingMethod);
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO).add(shippingCost);

        return new PricingResult(subtotal, shippingCost, discount, appliedCoupon, total, resolvedItems);
    }

    // Deliberately silent (no-op the discount) rather than throwing: by the time a couponCode
    // reaches here, the frontend has already surfaced a clear error via /api/coupons/validate,
    // so pricing/payment-intent creation should never hard-fail over a coupon becoming stale
    // between "applied in cart" and "order submitted" - it should just stop discounting.
    private boolean isEligible(Coupon coupon, BigDecimal subtotal, String customerEmail) {
        if (!coupon.isActive()) return false;
        if (coupon.getExpiryDate().isBefore(LocalDate.now())) return false;
        if (coupon.getMaxUses() != null && coupon.getUsesCount() >= coupon.getMaxUses()) return false;
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) return false;
        if (customerEmail != null && !customerEmail.isBlank()
                && couponRedemptionRepository.existsByCoupon_IdAndCustomerEmailIgnoreCase(coupon.getId(), customerEmail)) {
            return false;
        }
        return true;
    }

    private BigDecimal resolveShippingCost(String shippingMethod) {
        return "pickup".equalsIgnoreCase(shippingMethod) ? PICKUP_SHIPPING_COST : HEAVY_SHIPPING_COST;
    }
}
