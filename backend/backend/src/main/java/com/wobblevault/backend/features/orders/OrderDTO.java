package com.wobblevault.backend.features.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.wobblevault.backend.entity.Order;
import com.wobblevault.backend.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private UUID id;
    private String jobOrderNo;
    private String customerName;
    private List<ItemDTO> items;
    private BigDecimal discount;
    private BigDecimal price;
    private BigDecimal payment;
    private BigDecimal balance;
    private String paymentMethod;
    private String shop;
    private LocalDate orderDate;
    private String notes;
    private String status;
    private Boolean inventoryDeducted;
    private Long version;
    private LocalDateTime createdAt;

    public OrderDTO(Order order) {
        this.id = order.getId();
        this.jobOrderNo = order.getJobOrderNo();
        this.customerName = order.getCustomerName();
        this.items = order.getItems().stream()
                .map(ItemDTO::new)
                .collect(Collectors.toList());
        this.discount = order.getDiscount();
        this.price = order.getPrice();
        this.payment = order.getPayment();
        this.balance = calculateBalance(order);
        this.paymentMethod = order.getPaymentMethod();
        this.shop = order.getShop();
        this.orderDate = order.getOrderDate();
        this.notes = order.getNotes();
        this.status = order.getStatus();
        this.inventoryDeducted = order.getInventoryDeducted();
        this.version = order.getVersion();
        this.createdAt = order.getCreatedAt();
    }

    private static BigDecimal calculateBalance(Order order) {
        BigDecimal price = order.getPrice() != null ? order.getPrice() : BigDecimal.ZERO;
        BigDecimal payment = order.getPayment() != null ? order.getPayment() : BigDecimal.ZERO;
        BigDecimal balance = price.subtract(payment);
        return balance.max(BigDecimal.ZERO);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {

        private UUID id;
        private UUID inventoryId;
        private String productName;
        private String size;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;

        public ItemDTO(OrderItem item) {
            this.id = item.getId();
            this.inventoryId = item.getInventoryId();
            this.productName = item.getProductName();
            this.size = item.getSize();
            this.unitPrice = item.getUnitPrice();
            this.quantity = item.getQuantity();
            this.lineTotal = item.getUnitPrice() != null && item.getQuantity() != null
                    ? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    : BigDecimal.ZERO;
        }
    }

    public static List<OrderDTO> listFrom(List<Order> orders) {
        return orders.stream().map(OrderDTO::new).collect(Collectors.toCollection(ArrayList::new));
    }
}
