package com.example.vendor_item.model

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class Bill (
  val id: String?,
  val customerName: String?,
  val customerPhoneNo: String?,
  val items: List<BillItems>,
  val status: String?,
  val createdAt: Long?
)

data class BillItems(
    val id: String? = UUID.randomUUID().toString(),
    val bId: String?,
    val itemId: String?,
    val quantity: Int,
    val rate: Double,
    val tax: BigDecimal,
    val createdAt: Long? = Instant.now().toEpochMilli(),
)

data class ListBill(
    val id: String?,
    val customerName: String?,
    val customerPhoneNo: String?,
    val items: List<ListBillItem>,
    val status: String? = "done",
    val createdAt: Long? = Instant.now().toEpochMilli()
)

data class ListBillItem(
    val id: String?,
    val bId: String?,
    val itemId: String?,
    val itemName: String?,
    val quantity: Int,
    val rate: Double,
    val tax: Double,
    val createdAt: Long?,
)

data class BillWithDetails(
    val bill: Bill,
    val itemDetails: List<BillItemWithDetails>
)

data class BillItemWithDetails(
    val item: Item,
    val quantity: Int,
    val rate: Double,
    val tax: BigDecimal,
    val amount: Double
)
