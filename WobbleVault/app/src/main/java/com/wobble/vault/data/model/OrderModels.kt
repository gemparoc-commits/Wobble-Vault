package com.wobble.vault.data.model

data class OrderItemRequest(
    val inventoryId: String? = null,
    val productName: String? = null,
    val size: String? = null,
    val unitPrice: Double? = null,
    val quantity: Int? = null
)

data class CreateOrderRequest(
    val customerName: String? = null,
    val items: List<OrderItemRequest> = emptyList(),
    val discount: Double = 0.0,
    val price: Double,
    val payment: Double = 0.0,
    val paymentMethod: String,
    val shop: String,
    val orderDate: String,
    val notes: String? = null,
    val status: String
)

data class OrderItemDto(
    val id: String? = null,
    val inventoryId: String? = null,
    val productName: String? = null,
    val size: String? = null,
    val unitPrice: Double? = null,
    val quantity: Int? = null,
    val lineTotal: Double? = null
)

data class OrderDto(
    val id: String? = null,
    val jobOrderNo: String? = null,
    val customerName: String? = null,
    val items: List<OrderItemDto> = emptyList(),
    val discount: Double? = null,
    val price: Double? = null,
    val payment: Double? = null,
    val balance: Double? = null,
    val paymentMethod: String? = null,
    val shop: String? = null,
    val orderDate: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val inventoryDeducted: Boolean? = null,
    val version: Long? = null,
    val createdAt: String? = null
)
