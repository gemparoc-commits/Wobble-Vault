package com.wobblevault.backend.features.orders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.wobblevault.backend.entity.Inventory;
import com.wobblevault.backend.entity.Order;
import com.wobblevault.backend.entity.OrderItem;
import com.wobblevault.backend.features.income.CreateIncomeSourceRequest;
import com.wobblevault.backend.features.income.IncomeSourceService;
import com.wobblevault.backend.features.inventory.InventoryRepository;
import com.wobblevault.backend.support.IdempotencyService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private IncomeSourceService incomeSourceService;

    @Mock
    private JobOrderNumberService jobOrderNumberService;

    private OrderService orderService;

    private UUID inventoryId;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, inventoryRepository,
                incomeSourceService, jobOrderNumberService, new IdempotencyService());

        inventoryId = UUID.randomUUID();
        inventory = new Inventory();
        inventory.setId(inventoryId);
        inventory.setBrand("Nike");
        inventory.setName("Air Zoom");
        inventory.setSize("9");
        inventory.setQuantity(10);
        inventory.setPrice(new BigDecimal("2500.00"));

        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findByRequestFingerprint(any())).thenReturn(Optional.empty());
        when(jobOrderNumberService.generateJobOrderNumber(any(LocalDate.class))).thenReturn("071026-01");
    }

    private CreateOrderRequest request(BigDecimal quantityPrice, int quantity, BigDecimal payment, String status) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Cruz");
        request.setItems(List.of(new CreateOrderRequest.ItemRequest(
                inventoryId, "Air Zoom", "9", quantityPrice, quantity)));
        request.setDiscount(BigDecimal.ZERO);
        request.setPrice(quantityPrice.multiply(BigDecimal.valueOf(quantity)));
        request.setPayment(payment);
        request.setShop("store");
        request.setPaymentMethod("cash");
        request.setOrderDate(LocalDate.now());
        request.setNotes("rush");
        request.setStatus(status);
        return request;
    }

    @Test
    void createOrder_deductsStock_andCapturesPayment() {
        OrderDTO dto = orderService.createOrder(request(new BigDecimal("2500.00"), 2,
                new BigDecimal("5000.00"), null));

        assertEquals(OrderService.STATUS_ACTIVE, dto.getStatus());
        assertEquals(Boolean.TRUE, dto.getInventoryDeducted());
        assertEquals(8, inventory.getQuantity());
        assertEquals(new BigDecimal("5000.00"), dto.getPrice());
        assertEquals(0, dto.getBalance().compareTo(BigDecimal.ZERO));

        verify(incomeSourceService).syncOrderPayment(any(CreateIncomeSourceRequest.class));
    }

    @Test
    void createOrder_cancelledStatus_doesNotDeductStock() {
        OrderDTO dto = orderService.createOrder(request(new BigDecimal("2500.00"), 3,
                BigDecimal.ZERO, "CANCELLED"));

        assertEquals(OrderService.STATUS_CANCELLED, dto.getStatus());
        assertEquals(Boolean.FALSE, dto.getInventoryDeducted());
        assertEquals(10, inventory.getQuantity());
    }

    @Test
    void createOrder_insufficientStock_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(request(new BigDecimal("2500.00"), 99,
                        BigDecimal.ZERO, null)));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertEquals(10, inventory.getQuantity());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_duplicateFingerprint_returnsExistingOrder() {
        Order existing = new Order();
        existing.setId(UUID.randomUUID());
        existing.setJobOrderNo("071026-01");
        existing.setPrice(new BigDecimal("5000.00"));
        when(orderRepository.findByRequestFingerprint(any())).thenReturn(Optional.of(existing));

        OrderDTO dto = orderService.createOrder(request(new BigDecimal("2500.00"), 2,
                BigDecimal.ZERO, null));

        assertEquals(existing.getId(), dto.getId());
        assertEquals(10, inventory.getQuantity());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_invalidStatus_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(request(new BigDecimal("2500.00"), 1,
                        BigDecimal.ZERO, "COMPLETED")));
    }

    @Test
    void createOrder_invalidShop_throws() {
        CreateOrderRequest request = request(new BigDecimal("2500.00"), 1, BigDecimal.ZERO, null);
        request.setShop("bogus");
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
    }

    @Test
    void updateOrder_cancelRestoresStock() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setJobOrderNo("071026-01");
        order.setShop("store");
        order.setPaymentMethod("cash");
        order.setOrderDate(LocalDate.now());
        order.setStatus(OrderService.STATUS_ACTIVE);
        order.setInventoryDeducted(true);
        order.setPrice(new BigDecimal("5000.00"));
        order.setPayment(new BigDecimal("5000.00"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setInventoryId(inventoryId);
        item.setProductName("Air Zoom");
        item.setSize("9");
        item.setUnitPrice(new BigDecimal("2500.00"));
        item.setQuantity(2);
        order.getItems().add(item);

        inventory.setQuantity(8);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        CreateOrderRequest request = request(new BigDecimal("2500.00"), 2,
                new BigDecimal("5000.00"), "CANCELLED");
        OrderDTO dto = orderService.updateOrder(order.getId(), request);

        assertEquals(OrderService.STATUS_CANCELLED, dto.getStatus());
        assertEquals(Boolean.FALSE, dto.getInventoryDeducted());
        assertEquals(10, inventory.getQuantity());
        verify(incomeSourceService).syncOrderPayment(any(CreateIncomeSourceRequest.class));
    }

    @Test
    void deleteOrder_restoresStock_andRemovesPaymentIncome() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setJobOrderNo("071026-01");
        order.setStatus(OrderService.STATUS_ACTIVE);
        order.setInventoryDeducted(true);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setInventoryId(inventoryId);
        item.setProductName("Air Zoom");
        item.setSize("9");
        item.setUnitPrice(new BigDecimal("2500.00"));
        item.setQuantity(4);
        order.getItems().add(item);

        inventory.setQuantity(6);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.deleteOrder(order.getId());

        assertEquals(10, inventory.getQuantity());
        verify(incomeSourceService).deletePaymentsForJobOrderNo(eq("071026-01"),
                eq(IncomeSourceService.CATEGORY_PAYMENT));
        verify(orderRepository).delete(order);
    }

    @Test
    void getAllOrders_filtersByStatus() {
        when(orderRepository.findByStatusOrderByCreatedAtDesc(eq(OrderService.STATUS_ARCHIVED), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        orderService.getAllOrders("ARCHIVED", org.springframework.data.domain.Pageable.unpaged());

        verify(orderRepository).findByStatusOrderByCreatedAtDesc(eq(OrderService.STATUS_ARCHIVED), any());
        verify(orderRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    void getOrdersByYearMonth_rejectsInvalidMonth() {
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrdersByYearMonth(2026, 13));
    }

    @Test
    void createOrder_paymentExceedsTotal_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(
                request(new BigDecimal("2500.00"), 2, new BigDecimal("5000.01"), null)));

        assertTrue(ex.getMessage().contains("exceed"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void recordPayment_addsToPayment_andSyncsIncome() {
        Order order = activeOrder(new BigDecimal("5000.00"), new BigDecimal("2000.00"));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        OrderDTO dto = orderService.recordPayment(order.getId(), new BigDecimal("1500.00"), "gcash");

        assertEquals(0, dto.getPayment().compareTo(new BigDecimal("3500.00")));
        assertEquals("gcash", dto.getPaymentMethod());
        assertEquals(0, dto.getBalance().compareTo(new BigDecimal("1500.00")));
        verify(orderRepository).save(order);
        verify(incomeSourceService).syncOrderPayment(any(CreateIncomeSourceRequest.class));
    }

    @Test
    void recordPayment_overpay_throws() {
        Order order = activeOrder(new BigDecimal("5000.00"), new BigDecimal("4000.00"));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.recordPayment(order.getId(), new BigDecimal("1500.00"), "cash"));

        assertTrue(ex.getMessage().contains("exceed"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(incomeSourceService, never()).syncOrderPayment(any(CreateIncomeSourceRequest.class));
    }

    @Test
    void recordPayment_nonActiveOrder_throws() {
        Order order = activeOrder(new BigDecimal("5000.00"), BigDecimal.ZERO);
        order.setStatus(OrderService.STATUS_CANCELLED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.recordPayment(order.getId(), new BigDecimal("100.00"), "cash"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    private Order activeOrder(BigDecimal price, BigDecimal payment) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setJobOrderNo("071026-01");
        order.setShop("store");
        order.setPaymentMethod("cash");
        order.setOrderDate(LocalDate.now());
        order.setStatus(OrderService.STATUS_ACTIVE);
        order.setInventoryDeducted(false);
        order.setPrice(price);
        order.setPayment(payment);
        return order;
    }
}
