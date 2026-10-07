package com.wobblevault.backend.features.orders;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @Size(max = 255, message = "Customer name must be at most 255 characters")
    private String customerName;

    @NotEmpty(message = "Order must contain at least one item")
    private List<ItemRequest> items;

    @DecimalMin(value = "0.0", message = "Discount must be non-negative")
    private BigDecimal discount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Order total must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Payment must be non-negative")
    private BigDecimal payment;

    @NotBlank(message = "Shop is required")
    private String shop;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;

    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {

        @NotNull(message = "Inventory item is required")
        private UUID inventoryId;

        private String productName;

        private String size;

        private BigDecimal unitPrice;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
