package com.wobblevault.backend.features.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.wobblevault.backend.entity.Inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository);
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreateInventoryRequest request() {
        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setBrand("  Nike  ");
        request.setName(" Air Zoom ");
        request.setSize(" 9 ");
        request.setGender("  Men ");
        request.setSizingSystem(" US ");
        request.setQuantity(12);
        request.setPrice(new BigDecimal("2500.50"));
        request.setNotes("  restock soon  ");
        return request;
    }

    private Inventory storedItem(UUID id) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setBrand("Nike");
        inventory.setName("Air Zoom");
        inventory.setSize("9");
        inventory.setGender("Men");
        inventory.setSizingSystem("US");
        inventory.setQuantity(12);
        inventory.setPrice(new BigDecimal("2500.50"));
        inventory.setNotes("restock soon");
        return inventory;
    }

    @Test
    void createInventory_trimsInputAndReturnsDto() {
        InventoryDTO dto = inventoryService.createInventory(request());

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());

        assertEquals("Nike", captor.getValue().getBrand());
        assertEquals("Air Zoom", captor.getValue().getName());
        assertEquals("9", captor.getValue().getSize());
        assertEquals("Men", captor.getValue().getGender());
        assertEquals("US", captor.getValue().getSizingSystem());
        assertEquals("restock soon", captor.getValue().getNotes());
        assertEquals(12, captor.getValue().getQuantity());
        assertEquals(new BigDecimal("2500.50"), captor.getValue().getPrice());
        assertEquals("Nike", dto.getBrand());
        assertEquals("Men", dto.getGender());
        assertEquals("US", dto.getSizingSystem());
        assertEquals(12, dto.getQuantity());
    }

    @Test
    void createInventory_blankSizeAndNotes_becomeNull() {
        CreateInventoryRequest request = request();
        request.setSize("   ");
        request.setGender(null);
        request.setSizingSystem("  ");
        request.setNotes(null);

        inventoryService.createInventory(request);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertNull(captor.getValue().getSize());
        assertNull(captor.getValue().getGender());
        assertNull(captor.getValue().getSizingSystem());
        assertNull(captor.getValue().getNotes());
    }

    @Test
    void getInventoryById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(inventoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> inventoryService.getInventoryById(id));
    }

    @Test
    void updateInventory_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(inventoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.updateInventory(id, request()));
    }

    @Test
    void updateInventory_appliesNewValues() {
        UUID id = UUID.randomUUID();
        when(inventoryRepository.findById(id)).thenReturn(Optional.of(storedItem(id)));

        CreateInventoryRequest request = request();
        request.setBrand("Adidas");
        request.setGender("Women");
        request.setSizingSystem("EU");
        request.setQuantity(5);

        InventoryDTO dto = inventoryService.updateInventory(id, request);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertEquals("Adidas", captor.getValue().getBrand());
        assertEquals("Women", captor.getValue().getGender());
        assertEquals("EU", captor.getValue().getSizingSystem());
        assertEquals(5, captor.getValue().getQuantity());
        assertEquals("Adidas", dto.getBrand());
        assertEquals("Women", dto.getGender());
        assertEquals("EU", dto.getSizingSystem());
        assertEquals(5, dto.getQuantity());
    }

    @Test
    void deleteInventory_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(inventoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> inventoryService.deleteInventory(id));
        verify(inventoryRepository, never()).delete(any(Inventory.class));
    }

    @Test
    void deleteInventory_deletesFoundItem() {
        UUID id = UUID.randomUUID();
        Inventory item = storedItem(id);
        when(inventoryRepository.findById(id)).thenReturn(Optional.of(item));

        inventoryService.deleteInventory(id);

        verify(inventoryRepository).delete(item);
    }

    @Test
    void searchInventory_blankQuery_stillQueriesWithEmptyString() {
        when(inventoryRepository.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(storedItem(UUID.randomUUID())));

        List<InventoryDTO> results = inventoryService.searchInventory("   ");

        assertEquals(1, results.size());
        verify(inventoryRepository)
                .findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase("", "");
    }

    @Test
    void getLowStockInventory_mapsQuantityBelowThreshold() {
        Inventory low = storedItem(UUID.randomUUID());
        low.setQuantity(3);
        when(inventoryRepository.findByQuantityLessThan(10)).thenReturn(List.of(low));

        List<InventoryDTO> results = inventoryService.getLowStockInventory(10);

        assertEquals(1, results.size());
        assertEquals(3, results.get(0).getQuantity());
    }
}
