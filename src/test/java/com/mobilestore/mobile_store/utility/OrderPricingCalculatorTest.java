package com.mobilestore.mobile_store.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mobilestore.mobile_store.dto.request.OrderItemRequestDto;
import com.mobilestore.mobile_store.entity.Coupon;
import com.mobilestore.mobile_store.entity.DiscountType;
import com.mobilestore.mobile_store.entity.Product;
import com.mobilestore.mobile_store.entity.ProductColorVariant;
import com.mobilestore.mobile_store.exception.ProductVariantNotFoundException;
import com.mobilestore.mobile_store.repository.CouponRedemptionRepository;
import com.mobilestore.mobile_store.repository.CouponRepository;
import com.mobilestore.mobile_store.repository.ProductColorVariantRepository;

@ExtendWith(MockitoExtension.class)
class OrderPricingCalculatorTest {

    @Mock
    private ProductColorVariantRepository productColorVariantRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;

    private OrderPricingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new OrderPricingCalculator(productColorVariantRepository, couponRepository, couponRedemptionRepository);
    }

    private ProductColorVariant variant(long id, BigDecimal price, int stock) {
        Product product = Product.builder().id(1L).name("Phone").price(price).build();
        return ProductColorVariant.builder().id(id).color("Black").stockQuantity(stock).product(product).build();
    }

    private Coupon activeCoupon(DiscountType type, BigDecimal value) {
        return Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .discountType(type)
                .discountValue(value)
                .expiryDate(LocalDate.now().plusDays(30))
                .active(true)
                .usesCount(0)
                .build();
    }

    @Test
    void calculatesSubtotalAndShippingForNonPickup() {
        when(productColorVariantRepository.findById(1L)).thenReturn(Optional.of(variant(1L, BigDecimal.valueOf(1000), 5)));
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1L, 2);

        OrderPricingCalculator.PricingResult result = calculator.calculate("courier", List.of(item));

        assertThat(result.subtotal()).isEqualByComparingTo("2000");
        assertThat(result.shippingCost()).isEqualByComparingTo("200");
        assertThat(result.total()).isEqualByComparingTo("2200");
        assertThat(result.discountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void pickupShippingIsFree() {
        when(productColorVariantRepository.findById(1L)).thenReturn(Optional.of(variant(1L, BigDecimal.valueOf(500), 5)));
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1L, 1);

        OrderPricingCalculator.PricingResult result = calculator.calculate("pickup", List.of(item));

        assertThat(result.shippingCost()).isEqualByComparingTo("0");
        assertThat(result.total()).isEqualByComparingTo("500");
    }

    @Test
    void throwsWhenVariantNotFound() {
        when(productColorVariantRepository.findById(99L)).thenReturn(Optional.empty());
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 99L, 1);

        assertThrows(ProductVariantNotFoundException.class,
                () -> calculator.calculate("courier", List.of(item)));
    }

    @Test
    void appliesPercentageCouponToSubtotal() {
        when(productColorVariantRepository.findById(1L)).thenReturn(Optional.of(variant(1L, BigDecimal.valueOf(1000), 5)));
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(activeCoupon(DiscountType.PERCENTAGE, BigDecimal.TEN)));
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1L, 1);

        OrderPricingCalculator.PricingResult result = calculator.calculate("courier", List.of(item), "SAVE10");

        assertThat(result.discountAmount()).isEqualByComparingTo("100");
        assertThat(result.total()).isEqualByComparingTo("1100");
        assertThat(result.appliedCoupon()).isNotNull();
    }

    @Test
    void expiredCouponIsIgnoredNotRejected() {
        when(productColorVariantRepository.findById(1L)).thenReturn(Optional.of(variant(1L, BigDecimal.valueOf(1000), 5)));
        Coupon expired = activeCoupon(DiscountType.PERCENTAGE, BigDecimal.TEN);
        expired.setExpiryDate(LocalDate.now().minusDays(1));
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(expired));
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1L, 1);

        OrderPricingCalculator.PricingResult result = calculator.calculate("courier", List.of(item), "SAVE10");

        assertThat(result.discountAmount()).isEqualByComparingTo("0");
        assertThat(result.appliedCoupon()).isNull();
    }

    @Test
    void couponBelowMinOrderAmountIsRejected() {
        when(productColorVariantRepository.findById(1L)).thenReturn(Optional.of(variant(1L, BigDecimal.valueOf(100), 5)));
        Coupon coupon = activeCoupon(DiscountType.FLAT, BigDecimal.valueOf(50));
        coupon.setMinOrderAmount(BigDecimal.valueOf(500));
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1L, 1);

        OrderPricingCalculator.PricingResult result = calculator.calculate("courier", List.of(item), "SAVE10");

        assertThat(result.discountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void couponAtMaxUsesIsRejected() {
        when(productColorVariantRepository.findById(1L)).thenReturn(Optional.of(variant(1L, BigDecimal.valueOf(1000), 5)));
        Coupon coupon = activeCoupon(DiscountType.FLAT, BigDecimal.valueOf(50));
        coupon.setMaxUses(3);
        coupon.setUsesCount(3);
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1L, 1);

        OrderPricingCalculator.PricingResult result = calculator.calculate("courier", List.of(item), "SAVE10");

        assertThat(result.discountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void couponAlreadyRedeemedByCustomerIsRejected() {
        when(productColorVariantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(variant(1L, BigDecimal.valueOf(1000), 5)));
        Coupon coupon = activeCoupon(DiscountType.FLAT, BigDecimal.valueOf(50));
        when(couponRepository.findByCodeIgnoreCaseForUpdate("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.existsByCoupon_IdAndCustomerEmailIgnoreCase(1L, "buyer@example.com")).thenReturn(true);
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1L, 1);

        OrderPricingCalculator.PricingResult result = calculator.calculate(
                "courier", List.of(item), "SAVE10", "buyer@example.com", true);

        assertThat(result.discountAmount()).isEqualByComparingTo("0");
        assertThat(result.appliedCoupon()).isNull();
    }

    @Test
    void lockForUpdateUsesLockingRepositoryMethods() {
        when(productColorVariantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(variant(1L, BigDecimal.valueOf(1000), 5)));
        OrderItemRequestDto item = new OrderItemRequestDto(1L, 1L, 1);

        calculator.calculate("courier", List.of(item), null, null, true);

        org.mockito.Mockito.verify(productColorVariantRepository).findByIdForUpdate(1L);
        org.mockito.Mockito.verify(productColorVariantRepository, org.mockito.Mockito.never()).findById(anyLong());
    }
}
