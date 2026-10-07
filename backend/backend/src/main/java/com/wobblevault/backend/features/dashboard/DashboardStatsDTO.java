package com.wobblevault.backend.features.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    private long totalInventoryItems;
    private long lowStockItems;
    private long totalOrders;
    private long activeOrders;
    private long archivedOrders;
    private long cancelledOrders;
    private BigDecimal monthlySalesIncome;
    private BigDecimal monthlyLiquidation;
    private BigDecimal monthlyNetIncome;
}
