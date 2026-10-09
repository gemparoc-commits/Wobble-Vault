package com.wobblevault.backend.features.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wobblevault.backend.entity.Inventory;
import com.wobblevault.backend.entity.Order;
import com.wobblevault.backend.entity.OrderItem;
import com.wobblevault.backend.features.income.CreateIncomeSourceRequest;
import com.wobblevault.backend.features.income.IncomeSourceService;
import com.wobblevault.backend.features.inventory.InventoryRepository;
import com.wobblevault.backend.support.IdempotencyService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final List<String> ALLOWED_STATUSES = List.of(STATUS_ACTIVE, STATUS_ARCHIVED, STATUS_CANCELLED);
    private static final List<String> ALLOWED_SHOPS = List.of("store", "online");
    private static final List<String> ALLOWED_PAYMENT_METHODS = List.of("cash", "gcash");

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final IncomeSourceService incomeSourceService;
    private final JobOrderNumberService jobOrderNumberService;
    private final IdempotencyService idempotencyService;

    public OrderService(OrderRepository orderRepository,
                        InventoryRepository inventoryRepository,
                        IncomeSourceService incomeSourceService,
                        JobOrderNumberService jobOrderNumberService,
                        IdempotencyService idempotencyService) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.incomeSourceService = incomeSourceService;
        this.jobOrderNumberService = jobOrderNumberService;
        this.idempotencyService = idempotencyService;
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        String status = normalizeStatus(request.getStatus());
        String shop = normalizeShop(request.getShop());
        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());

        List<CreateOrderRequest.ItemRequest> items = normalizeItems(request.getItems());
        BigDecimal discount = zeroIfNull(request.getDiscount());
        BigDecimal payment = zeroIfNull(request.getPayment());
        BigDecimal total = computeTotal(items).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot exceed the order total");
        }
        if (payment.compareTo(total) > 0) {
            throw new IllegalArgumentException("Payment cannot exceed the order total");
        }

        String fingerprint = buildFingerprint(request, shop, paymentMethod, status, discount, payment);
        Order existing = orderRepository.findByRequestFingerprint(fingerprint).orElse(null);
        if (existing != null) {
            return new OrderDTO(existing);
        }

        Order order = idempotencyService.execute("order:create:" + fingerprint, () -> {
            Order draft = new Order();
            draft.setJobOrderNo(jobOrderNumberService.generateJobOrderNumber(request.getOrderDate()));
            draft.setRequestFingerprint(fingerprint);
            draft.setCustomerName(blankToNull(request.getCustomerName()));
            draft.setDiscount(discount);
            draft.setPrice(total);
            draft.setShop(shop);
            draft.setOrderDate(request.getOrderDate());
            draft.setPaymentMethod(paymentMethod);
            draft.setPayment(payment);
            draft.setNotes(blankToNull(request.getNotes()));
            draft.setStatus(status);
            draft.setInventoryDeducted(false);

            for (CreateOrderRequest.ItemRequest item : items) {
                OrderItem orderItem = buildOrderItem(draft, item);
                draft.getItems().add(orderItem);
            }

            if (!STATUS_CANCELLED.equals(status)) {
                deductStock(draft.getItems());
                draft.setInventoryDeducted(true);
            }

            Order savedOrder = orderRepository.save(draft);
            syncOrderIncome(savedOrder);
            return savedOrder;
        });

        return new OrderDTO(order);
    }

    public OrderDTO getOrderById(UUID id) {
        return new OrderDTO(requireOrder(id));
    }

    public OrderDTO getOrderByJobOrderNo(String jobOrderNo) {
        Order order = orderRepository.findByJobOrderNo(jobOrderNo)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return new OrderDTO(order);
    }

    public Page<OrderDTO> getAllOrders(String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return orderRepository.findAllByOrderByCreatedAtDesc(pageable).map(OrderDTO::new);
        }
        return orderRepository.findByStatusOrderByCreatedAtDesc(normalizeStatus(status), pageable).map(OrderDTO::new);
    }

    public List<OrderDTO> getOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        return OrderDTO.listFrom(orderRepository.findByOrderDateBetween(startDate, endDate));
    }

    public List<OrderDTO> getOrdersByYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        return OrderDTO.listFrom(orderRepository.findByYearAndMonth(year, month));
    }

    public OrderDTO updateOrder(UUID id, CreateOrderRequest request) {
        Order order = requireOrder(id);

        String status = normalizeStatus(request.getStatus() != null ? request.getStatus() : order.getStatus());
        String shop = normalizeShop(request.getShop() != null ? request.getShop() : order.getShop());
        String paymentMethod = normalizePaymentMethod(
                request.getPaymentMethod() != null ? request.getPaymentMethod() : order.getPaymentMethod());
        LocalDate orderDate = request.getOrderDate() != null ? request.getOrderDate() : order.getOrderDate();

        List<CreateOrderRequest.ItemRequest> items = normalizeItems(request.getItems());
        BigDecimal discount = zeroIfNull(request.getDiscount());
        BigDecimal payment = zeroIfNull(request.getPayment());
        BigDecimal total = computeTotal(items).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot exceed the order total");
        }
        if (payment.compareTo(total) > 0) {
            throw new IllegalArgumentException("Payment cannot exceed the order total");
        }

        if (order.getInventoryDeducted()) {
            restoreStock(order.getItems());
        }
        order.getItems().clear();

        order.setCustomerName(blankToNull(request.getCustomerName()));
        order.setDiscount(discount);
        order.setPrice(total);
        order.setShop(shop);
        order.setOrderDate(orderDate);
        order.setPaymentMethod(paymentMethod);
        order.setPayment(payment);
        order.setNotes(blankToNull(request.getNotes()));
        order.setStatus(status);
        order.setInventoryDeducted(false);

        for (CreateOrderRequest.ItemRequest item : items) {
            order.getItems().add(buildOrderItem(order, item));
        }

        if (!STATUS_CANCELLED.equals(status)) {
            deductStock(order.getItems());
            order.setInventoryDeducted(true);
        }

        Order savedOrder = orderRepository.save(order);
        syncOrderIncome(savedOrder);
        return new OrderDTO(savedOrder);
    }

    public OrderDTO recordPayment(UUID id, BigDecimal amount, String paymentMethod) {
        Order order = requireOrder(id);
        if (!STATUS_ACTIVE.equals(order.getStatus())) {
            throw new IllegalArgumentException("Only active orders can receive payments");
        }

        BigDecimal additional = amount != null ? amount : BigDecimal.ZERO;
        if (additional.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        BigDecimal total = order.getPrice() != null ? order.getPrice() : BigDecimal.ZERO;
        BigDecimal paid = order.getPayment() != null ? order.getPayment() : BigDecimal.ZERO;
        BigDecimal newPayment = paid.add(additional);
        if (newPayment.compareTo(total) > 0) {
            throw new IllegalArgumentException("Payment cannot exceed the order total");
        }

        order.setPayment(newPayment);
        order.setPaymentMethod(normalizePaymentMethod(paymentMethod));

        Order savedOrder = orderRepository.save(order);
        syncOrderIncome(savedOrder);
        return new OrderDTO(savedOrder);
    }

    public void deleteOrder(UUID id) {
        Order order = requireOrder(id);

        if (order.getInventoryDeducted()) {
            restoreStock(order.getItems());
        }
        if (order.getJobOrderNo() != null) {
            incomeSourceService.deletePaymentsForJobOrderNo(order.getJobOrderNo(), IncomeSourceService.CATEGORY_PAYMENT);
        }
        orderRepository.delete(order);
    }

    private Order requireOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    private OrderItem buildOrderItem(Order order, CreateOrderRequest.ItemRequest request) {
        Inventory inventory = inventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + request.getInventoryId()));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setInventoryId(inventory.getId());
        item.setProductName(inventory.getName());
        item.setSize(inventory.getSize());
        item.setUnitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : inventory.getPrice());
        item.setQuantity(request.getQuantity());
        return item;
    }

    private void deductStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Inventory inventory = inventoryRepository.findById(item.getInventoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + item.getInventoryId()));
            int available = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            if (available < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + inventory.getName() + " (available: " + available + ")");
            }
            inventory.setQuantity(available - item.getQuantity());
            inventoryRepository.save(inventory);
        }
    }

    private void restoreStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.getInventoryId() == null || item.getQuantity() == null) {
                continue;
            }
            inventoryRepository.findById(item.getInventoryId()).ifPresent(inventory -> {
                int current = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
                inventory.setQuantity(current + item.getQuantity());
                inventoryRepository.save(inventory);
            });
        }
    }

    private void syncOrderIncome(Order order) {
        BigDecimal payment = order.getPayment() != null ? order.getPayment() : BigDecimal.ZERO;
        if (payment.compareTo(BigDecimal.ZERO) <= 0) {
            incomeSourceService.syncOrderPayment(new CreateIncomeSourceRequest(
                    order.getShop(), order.getPaymentMethod(), order.getOrderDate(),
                    order.getCustomerName(), order.getJobOrderNo(), BigDecimal.ZERO,
                    null, IncomeSourceService.CATEGORY_PAYMENT, order.getNotes()));
            return;
        }

        incomeSourceService.syncOrderPayment(new CreateIncomeSourceRequest(
                order.getShop(), order.getPaymentMethod(), order.getOrderDate(),
                order.getCustomerName(), order.getJobOrderNo(), payment,
                order.getJobOrderNo(), IncomeSourceService.CATEGORY_PAYMENT, order.getNotes()));
    }

    private BigDecimal computeTotal(List<CreateOrderRequest.ItemRequest> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.ItemRequest item : items) {
            if (item.getUnitPrice() == null) {
                throw new IllegalArgumentException("Item price is required");
            }
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    private List<CreateOrderRequest.ItemRequest> normalizeItems(List<CreateOrderRequest.ItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        for (CreateOrderRequest.ItemRequest item : items) {
            if (item.getInventoryId() == null) {
                throw new IllegalArgumentException("Each order item must reference an inventory row");
            }
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new IllegalArgumentException("Order item quantity must be at least 1");
            }
        }
        return items;
    }

    private String buildFingerprint(CreateOrderRequest request,
                                    String shop,
                                    String paymentMethod,
                                    String status,
                                    BigDecimal discount,
                                    BigDecimal payment) {
        StringBuilder builder = new StringBuilder();
        String customerName = request.getCustomerName() != null
                ? request.getCustomerName().toLowerCase(Locale.ROOT)
                : "";
        builder.append(customerName).append('|');
        builder.append(shop).append('|');
        builder.append(request.getOrderDate()).append('|');
        builder.append(paymentMethod).append('|');
        builder.append(payment).append('|');
        builder.append(discount).append('|');
        builder.append(status);
        for (CreateOrderRequest.ItemRequest item : request.getItems()) {
            builder.append('|').append(item.getInventoryId()).append(':').append(item.getQuantity());
        }
        return sha256(builder.toString());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_ACTIVE;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Status must be one of: ACTIVE, ARCHIVED, CANCELLED");
        }
        return normalized;
    }

    private String normalizeShop(String shop) {
        String normalized = shop != null ? shop.trim().toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_SHOPS.contains(normalized)) {
            throw new IllegalArgumentException("Shop must be one of: store, online");
        }
        return normalized;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod != null ? paymentMethod.trim().toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_PAYMENT_METHODS.contains(normalized)) {
            throw new IllegalArgumentException("Payment method must be one of: cash, gcash");
        }
        return normalized;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
