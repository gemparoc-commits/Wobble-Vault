package com.wobblevault.backend.features.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wobblevault.backend.entity.Inventory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryDTO createInventory(CreateInventoryRequest request) {
        normalize(request);
        Inventory inventory = new Inventory();
        inventory.setBrand(request.getBrand());
        inventory.setName(request.getName());
        inventory.setSize(request.getSize());
        inventory.setNotes(request.getNotes());
        inventory.setQuantity(request.getQuantity());
        inventory.setPrice(request.getPrice());

        Inventory savedInventory = inventoryRepository.save(inventory);
        return new InventoryDTO(savedInventory);
    }

    public InventoryDTO getInventoryById(UUID id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));
        return new InventoryDTO(inventory);
    }

    public Page<InventoryDTO> getAllInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable)
                .map(InventoryDTO::new);
    }

    public List<InventoryDTO> searchInventory(String query) {
        String trimmed = query != null ? query.trim() : "";
        return inventoryRepository.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(trimmed, trimmed).stream()
                .map(InventoryDTO::new)
                .collect(Collectors.toList());
    }

    public List<InventoryDTO> getLowStockInventory(Integer threshold) {
        return inventoryRepository.findByQuantityLessThan(threshold).stream()
                .map(InventoryDTO::new)
                .collect(Collectors.toList());
    }

    public InventoryDTO updateInventory(UUID id, CreateInventoryRequest request) {
        normalize(request);
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

        inventory.setBrand(request.getBrand());
        inventory.setName(request.getName());
        inventory.setSize(request.getSize());
        inventory.setNotes(request.getNotes());
        inventory.setQuantity(request.getQuantity());
        inventory.setPrice(request.getPrice());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return new InventoryDTO(updatedInventory);
    }

    public void deleteInventory(UUID id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));
        inventoryRepository.delete(inventory);
    }

    private void normalize(CreateInventoryRequest request) {
        request.setBrand(request.getBrand().trim());
        request.setName(request.getName().trim());
        request.setSize(blankToNull(request.getSize()));
        request.setNotes(blankToNull(request.getNotes()));
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
