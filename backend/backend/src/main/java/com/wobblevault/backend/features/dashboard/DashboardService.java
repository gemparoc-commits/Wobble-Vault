package com.wobblevault.backend.features.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wobblevault.backend.entity.IncomeSource;
import com.wobblevault.backend.features.income.IncomeSourceRepository;
import com.wobblevault.backend.features.income.IncomeSourceService;
import com.wobblevault.backend.features.inventory.InventoryRepository;
import com.wobblevault.backend.features.orders.OrderRepository;
import com.wobblevault.backend.features.orders.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final IncomeSourceRepository incomeSourceRepository;

    public DashboardService(InventoryRepository inventoryRepository,
                            OrderRepository orderRepository,
                            IncomeSourceRepository incomeSourceRepository) {
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.incomeSourceRepository = incomeSourceRepository;
    }

    public DashboardStatsDTO getStats() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal liquidation = BigDecimal.ZERO;
        for (IncomeSource income : incomeSourceRepository.findByIncomeDateBetween(monthStart, monthEnd)) {
            BigDecimal amount = income.getAmount() != null ? income.getAmount() : BigDecimal.ZERO;
            if (IncomeSourceService.CATEGORY_LIQUIDATION.equals(income.getPaymentCategory())) {
                liquidation = liquidation.add(amount);
            } else {
                sales = sales.add(amount);
            }
        }

        return new DashboardStatsDTO(
                inventoryRepository.count(),
                inventoryRepository.countByQuantityLessThan(LOW_STOCK_THRESHOLD),
                orderRepository.count(),
                orderRepository.countByStatus(OrderService.STATUS_ACTIVE),
                orderRepository.countByStatus(OrderService.STATUS_ARCHIVED),
                orderRepository.countByStatus(OrderService.STATUS_CANCELLED),
                sales,
                liquidation,
                sales.subtract(liquidation)
        );
    }
}
