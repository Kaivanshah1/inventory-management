package com.example.vendor_item.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PurchaseOrder (
    val id: String?,
    val vendorId: String?,
    val items: List<PurchaseOrderItem>,
    val status: String? = "done",
    val expectedDate: Long?,
    val createdAt: Long? = Instant.now().toEpochMilli()
)

data class PurchaseOrderItem(
    val id: String? = UUID.randomUUID().toString(),
    val poId: String?,
    val itemId: String?,
    val quantity: Int,
    val rate: Double,
    val tax: BigDecimal? = BigDecimal("0.00"),
    val createdAt: Long? = Instant.now().toEpochMilli(),
)

data class ListPurchaseOrder(
    val id: String?,
    val vendorId: String?,
    val vendorName: String?,
    val items: List<ListPurchaseOrderItem>,
    val status: String? = "done",
    val expectedDate: Long?,
    val createdAt: Long? = Instant.now().toEpochMilli()
)

data class ListPurchaseOrderItem(
    val id: String?,
    val poId: String?,
    val itemId: String?,
    val itemName: String?,
    val quantity: Int,
    val rate: Double,
    val tax: BigDecimal?,
    val createdAt: Long?,
)

data class PurchaseOrderWithDetails(
    val purchaseOrder: PurchaseOrder,
    val vendor: Vendor,
    val itemDetails: List<ItemWithDetails>
)

data class ItemWithDetails(
    val item: Item,
    val quantity: Int,
    val rate: Double,
    val tax: BigDecimal?,
    val amount: Double
)
