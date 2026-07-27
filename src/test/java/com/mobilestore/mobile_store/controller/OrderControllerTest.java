package com.mobilestore.mobile_store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mobilestore.mobile_store.dto.request.UpdateOrderStatusRequestDto;
import com.mobilestore.mobile_store.dto.response.OrderResponseDto;
import com.mobilestore.mobile_store.dto.response.OrderStatusUpdateResponseDto;
import com.mobilestore.mobile_store.entity.OrderStatus;
import com.mobilestore.mobile_store.repository.OrderRepository;
import com.mobilestore.mobile_store.service.OrderService;
import com.mobilestore.mobile_store.service.PdfService;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PdfService pdfService;

    private OrderController controller;

    private UpdateOrderStatusRequestDto request(OrderStatus status) {
        UpdateOrderStatusRequestDto dto = new UpdateOrderStatusRequestDto();
        dto.setStatus(status);
        return dto;
    }

    @Test
    void messageIsGenericSuccessWhenEmailSentTrue() {
        controller = new OrderController(orderService, orderRepository, pdfService);
        when(orderService.updateOrderStatus(1L, OrderStatus.SHIPPED))
                .thenReturn(new OrderStatusUpdateResponseDto(new OrderResponseDto(), true));

        var response = controller.updateOrderStatus(1L, request(OrderStatus.SHIPPED));

        assertThat(response.getBody().getMessage()).isEqualTo("Order status updated successfully");
    }

    @Test
    void messageWarnsWhenEmailSentFalse() {
        controller = new OrderController(orderService, orderRepository, pdfService);
        when(orderService.updateOrderStatus(1L, OrderStatus.DELIVERED))
                .thenReturn(new OrderStatusUpdateResponseDto(new OrderResponseDto(), false));

        var response = controller.updateOrderStatus(1L, request(OrderStatus.DELIVERED));

        assertThat(response.getBody().getMessage())
                .isEqualTo("Order status updated, but the notification email failed to send");
    }

    @Test
    void messageIsGenericSuccessWhenEmailSentNull() {
        controller = new OrderController(orderService, orderRepository, pdfService);
        when(orderService.updateOrderStatus(1L, OrderStatus.DELIVERED))
                .thenReturn(new OrderStatusUpdateResponseDto(new OrderResponseDto(), null));

        var response = controller.updateOrderStatus(1L, request(OrderStatus.DELIVERED));

        assertThat(response.getBody().getMessage()).isEqualTo("Order status updated successfully");
    }
}
