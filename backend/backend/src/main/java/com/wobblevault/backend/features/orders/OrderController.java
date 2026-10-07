package com.wobblevault.backend.features.orders;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDERS')")
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderDTO orderDTO = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDTO);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('ORDERS', 'SALES')")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/job-order-no/{jobOrderNo}")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('ORDERS', 'SALES')")
    public ResponseEntity<OrderDTO> getOrderByJobOrderNo(@PathVariable String jobOrderNo) {
        return ResponseEntity.ok(orderService.getOrderByJobOrderNo(jobOrderNo));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('ORDERS', 'SALES')")
    public ResponseEntity<List<OrderDTO>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(orderService.getOrdersByDateRange(startDate, endDate));
    }

    @GetMapping("/year-month")
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('ORDERS', 'SALES')")
    public ResponseEntity<List<OrderDTO>> getOrdersByYearMonth(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(orderService.getOrdersByYearMonth(year, month));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('ORDERS', 'SALES')")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(status, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDERS')")
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable UUID id,
                                                @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
