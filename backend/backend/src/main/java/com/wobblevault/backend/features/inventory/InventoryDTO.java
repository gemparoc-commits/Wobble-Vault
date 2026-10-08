package com.wobblevault.backend.features.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.wobblevault.backend.entity.Inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDTO {

    private UUID id;
    private String brand;
    private String name;
    private String size;
    private String gender;
    private String sizingSystem;
    private String notes;
    private Integer quantity;
    private BigDecimal price;
    private LocalDateTime createdAt;

    public InventoryDTO(Inventory inventory) {
        this.id = inventory.getId();
        this.brand = inventory.getBrand();
        this.name = inventory.getName();
        this.size = inventory.getSize();
        this.gender = inventory.getGender();
        this.sizingSystem = inventory.getSizingSystem();
        this.notes = inventory.getNotes();
        this.quantity = inventory.getQuantity();
        this.price = inventory.getPrice();
        this.createdAt = inventory.getCreatedAt();
    }
}
