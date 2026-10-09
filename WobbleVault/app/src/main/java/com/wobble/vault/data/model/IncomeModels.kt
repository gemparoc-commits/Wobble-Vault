package com.wobble.vault.data.model

data class IncomeSourceDto(
    val id: String? = null,
    val shopType: String? = null,
    val paymentMethod: String? = null,
    val incomeDate: String? = null,
    val customerName: String? = null,
    val jobOrderNo: String? = null,
    val amount: Double? = null,
    val referenceNumber: String? = null,
    val paymentCategory: String? = null,
    val remarks: String? = null,
    val createdAt: String? = null
) {
    fun isLiquidation(): Boolean =
        paymentCategory.equals("LIQUIDATION", ignoreCase = true)
}

data class CreateIncomeSourceRequest(
    val shopType: String,
    val paymentMethod: String,
    val incomeDate: String,
    val customerName: String? = null,
    val jobOrderNo: String? = null,
    val amount: Double,
    val referenceNumber: String? = null,
    val paymentCategory: String? = null,
    val remarks: String? = null
)
