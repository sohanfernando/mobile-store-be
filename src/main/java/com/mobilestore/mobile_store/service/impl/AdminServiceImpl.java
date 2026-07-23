package com.mobilestore.mobile_store.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mobilestore.mobile_store.dto.response.AdminAnalyticsResponseDto;
import com.mobilestore.mobile_store.dto.response.CategoryShareResponseDto;
import com.mobilestore.mobile_store.dto.response.CustomerResponseDto;
import com.mobilestore.mobile_store.dto.response.GlobalReviewResponseDto;
import com.mobilestore.mobile_store.dto.response.SalesTrendResponseDto;
import com.mobilestore.mobile_store.entity.Customer;
import com.mobilestore.mobile_store.entity.Order;
import com.mobilestore.mobile_store.entity.OrderItem;
import com.mobilestore.mobile_store.entity.OrderStatus;
import com.mobilestore.mobile_store.entity.Product;
import com.mobilestore.mobile_store.repository.CustomerRepository;
import com.mobilestore.mobile_store.repository.OrderRepository;
import com.mobilestore.mobile_store.repository.ProductColorVariantRepository;
import com.mobilestore.mobile_store.repository.ReviewRepository;
import com.mobilestore.mobile_store.service.AdminService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final OrderRepository orderRepository;
    private final ProductColorVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public AdminAnalyticsResponseDto getAnalytics() {
        List<Order> orders = orderRepository.findAll();
        
        List<Order> completedOrders = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        // 1. KPI Calculations
        BigDecimal totalRevenue = completedOrders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = completedOrders.size();

        long lowStockCount = variantRepository.findAll().stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() <= 10)
                .count();

        long totalCustomers = customerRepository.count();

        // 2. Category Share Calculations
        Map<String, BigDecimal> categoryRevenue = new HashMap<>();
        for (Order order : completedOrders) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null && product.getCategory() != null) {
                    String category = product.getCategory();
                    BigDecimal itemRevenue = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    categoryRevenue.put(category, categoryRevenue.getOrDefault(category, BigDecimal.ZERO).add(itemRevenue));
                }
            }
        }

        List<CategoryShareResponseDto> categoryShare = categoryRevenue.entrySet().stream()
                .map(entry -> CategoryShareResponseDto.builder()
                        .category(entry.getKey())
                        .revenue(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // 3. Monthly Sales Trend (Last 6 Months)
        Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--) {
            String monthKey = LocalDate.now().minusMonths(i).format(formatter);
            monthlyRevenue.put(monthKey, BigDecimal.ZERO);
        }

        for (Order order : completedOrders) {
            if (order.getCreatedAt() != null) {
                String orderMonth = order.getCreatedAt().format(formatter);
                if (monthlyRevenue.containsKey(orderMonth)) {
                    monthlyRevenue.put(orderMonth, monthlyRevenue.get(orderMonth).add(order.getTotal()));
                }
            }
        }

        List<SalesTrendResponseDto> salesTrend = monthlyRevenue.entrySet().stream()
                .map(entry -> SalesTrendResponseDto.builder()
                        .month(entry.getKey())
                        .revenue(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return AdminAnalyticsResponseDto.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .lowStockCount(lowStockCount)
                .totalCustomers(totalCustomers)
                .categoryShare(categoryShare)
                .salesTrend(salesTrend)
                .build();
    }

    @Override
    public List<GlobalReviewResponseDto> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(r -> GlobalReviewResponseDto.builder()
                        .id(r.getId())
                        .authorName(r.getAuthorName())
                        .rating(r.getRating())
                        .title(r.getTitle())
                        .text(r.getText())
                        .verifiedPurchase(r.isVerifiedPurchase())
                        .createdAt(r.getCreatedAt())
                        .productId(r.getProduct() != null ? r.getProduct().getId() : null)
                        .productName(r.getProduct() != null ? r.getProduct().getName() : null)
                        .productBrand(r.getProduct() != null ? r.getProduct().getBrand() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }

    @Override
    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(c -> CustomerResponseDto.builder()
                        .id(c.getId())
                        .email(c.getEmail())
                        .name(c.getName())
                        .phone(c.getPhone())
                        .address(c.getAddress())
                        .city(c.getCity())
                        .postalCode(c.getPostalCode())
                        .country(c.getCountry())
                        .active(c.isActive())
                        .orderCount(c.getOrders() != null ? c.getOrders().size() : 0)
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerResponseDto toggleCustomerStatus(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customer.setActive(!customer.isActive());
        Customer saved = customerRepository.save(customer);

        return CustomerResponseDto.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .name(saved.getName())
                .phone(saved.getPhone())
                .address(saved.getAddress())
                .city(saved.getCity())
                .postalCode(saved.getPostalCode())
                .country(saved.getCountry())
                .active(saved.isActive())
                .orderCount(saved.getOrders() != null ? saved.getOrders().size() : 0)
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
