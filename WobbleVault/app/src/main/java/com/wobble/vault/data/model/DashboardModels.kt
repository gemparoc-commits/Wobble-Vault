package com.wobble.vault.data.model

data class DashboardStats(
    val totalInventoryItems: Long = 0,
    val lowStockItems: Long = 0,
    val totalOrders: Long = 0,
    val activeOrders: Long = 0,
    val archivedOrders: Long = 0,
    val cancelledOrders: Long = 0,
    val monthlySalesIncome: Double = 0.0,
    val monthlyLiquidation: Double = 0.0,
    val monthlyNetIncome: Double = 0.0
)
