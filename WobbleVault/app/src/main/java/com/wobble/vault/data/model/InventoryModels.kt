package com.wobble.vault.data.model

data class InventoryItem(
    val id: String? = null,
    val brand: String? = null,
    val name: String? = null,
    val size: String? = null,
    val gender: String? = null,
    val sizingSystem: String? = null,
    val notes: String? = null,
    val quantity: Int? = null,
    val price: Double? = null,
    val createdAt: String? = null
)

data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0
)

data class InventoryRequest(
    val brand: String,
    val name: String,
    val gender: String? = null,
    val sizingSystem: String? = null,
    val size: String? = null,
    val quantity: Int? = null,
    val price: Double? = null,
    val notes: String? = null
)
