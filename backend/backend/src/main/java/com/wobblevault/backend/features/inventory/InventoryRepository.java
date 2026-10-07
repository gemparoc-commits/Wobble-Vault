package com.wobblevault.backend.features.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wobblevault.backend.entity.Inventory;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    List<Inventory> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(String name, String brand);

    List<Inventory> findByQuantityLessThan(Integer quantity);

    long countByQuantityLessThan(Integer quantity);
}
