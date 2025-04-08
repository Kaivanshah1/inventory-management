package com.example.vendor_item.model

data class Item(
    val id: String?,
    val name: String,
    val price: Double,
    val stockLevel: Int,
    val reorderPoint: Int,
    val vendorId: String?,
    val description: String?,
    val imageUrl: String?,
    val createdAt: Long?,
    val status: String?,
    val totalQuantity: Int? = null
)

data class ListItem(
    val search: String?,
    val page: Int?,
    val size: Int?,
    val getAll: Boolean?,
    val vendorId: String? = null,
    val status: List<String>?
)