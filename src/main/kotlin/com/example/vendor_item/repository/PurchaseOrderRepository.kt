package com.example.vendor_item.repository

import com.example.vendor_item.model.ListPurchaseOrder
import com.example.vendor_item.model.ListPurchaseOrderItem
import com.example.vendor_item.model.PurchaseOrder
import com.example.vendor_item.model.PurchaseOrderItem
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Repository
class PurchaseOrderRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
        PurchaseOrder(
            id = rs.getString("id"),
            vendorId = rs.getString("vendor_id"),
            status = rs.getString("status"),
            items = emptyList(),
            expectedDate = rs.getLong("expected_date"),
            createdAt = rs.getLong("created_at")
        )
    }

    private val purchaseOrderItemRowMapper = RowMapper { rs, _ ->
        PurchaseOrderItem(
            id = rs.getString("id"),
            poId = rs.getString("po_id"),
            itemId = rs.getString("item_id"),
            quantity = rs.getInt("quantity"),
            rate = rs.getDouble("rate"),
            tax = rs.getBigDecimal("tax"),
            createdAt = rs.getLong("created_at")
        )
    }

    fun countNoOfRows(): Int? {
        val sql = """
        select count(*) from purchase_orders;
    """.trimIndent()

        return jdbcTemplate.queryForObject(sql, Int::class.java)
    }

    fun save(purchaseOrder: PurchaseOrder): Int {
        // First, insert the purchase order
        val poSql = "INSERT INTO purchase_orders (id, vendor_id, status, expected_date, created_at) VALUES (?, ?, ?, ?, ?)"
        val poResult = jdbcTemplate.update(
            poSql,
            purchaseOrder.id,
            purchaseOrder.vendorId,
            purchaseOrder.status,
            purchaseOrder.expectedDate,
            purchaseOrder.createdAt
        )
        // Then, insert each purchase order item
            if (purchaseOrder.items.isNotEmpty()) {
            val itemSql = "INSERT INTO purchase_order_items (id, po_id, item_id, quantity, rate, tax, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"

            purchaseOrder.items.forEach { item ->
                jdbcTemplate.update(
                    itemSql,
                    UUID.randomUUID().toString(),
                    purchaseOrder.id, // Ensure the poId is set to the parent purchase order id
                    item.itemId,
                    item.quantity,
                    item.rate,
                    item.tax,
                    item.createdAt?: Instant.now().toEpochMilli().toInt()
                )
            }
        }

        return poResult
    }

    fun update(purchaseOrder: PurchaseOrder): PurchaseOrder? {
        try {
            println("updating purchase order: $purchaseOrder")

            // Step 1: Get the current items for this purchase order from the database
            val currentItemsQuery = "SELECT id FROM purchase_order_items WHERE po_id = ?"
            val currentItemIds = jdbcTemplate.queryForList(currentItemsQuery, String::class.java, purchaseOrder.id)

            // Step 2: Extract ids of items in the incoming request
            val updatedItemIds = purchaseOrder.items.mapNotNull { it.id }

            // Step 3: Find items that are in the database but not in the request (need to be deleted)
            val itemsToDelete = currentItemIds.filter { currentId ->
                !updatedItemIds.contains(currentId)
            }

            // Step 4: Delete items that are no longer present
            if (itemsToDelete.isNotEmpty()) {
                val deleteItemSql = "DELETE FROM purchase_order_items WHERE id = ?"
                itemsToDelete.forEach { itemId ->
                    jdbcTemplate.update(deleteItemSql, itemId)
                }
            }

            // Step 5: Update the purchase order details
            val poSql = "UPDATE purchase_orders SET vendor_id = ?, status = ?, expected_date = ? WHERE id = ?"
            jdbcTemplate.update(
                poSql,
                purchaseOrder.vendorId,
                purchaseOrder.status,
                purchaseOrder.expectedDate,
                purchaseOrder.id,
            )

            // Step 6: Process all items (update existing ones and insert new ones)
            if (purchaseOrder.items.isNotEmpty()) {
                val updateItemSql = "UPDATE purchase_order_items SET item_id = ?, quantity = ?, rate = ?, tax = ? WHERE id = ?"
                val insertItemSql = "INSERT INTO purchase_order_items (id, po_id, item_id, quantity, rate, tax, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"

                purchaseOrder.items.forEach { item ->
                    if (item.id != null && currentItemIds.contains(item.id)) {
                        // Update existing item
                        jdbcTemplate.update(
                            updateItemSql,
                            item.itemId,
                            item.quantity,
                            item.rate,
                            item.tax,
                            item.id
                        )
                    } else {
                        // Insert new item
                        val newItemId = UUID.randomUUID().toString()
                        jdbcTemplate.update(
                            insertItemSql,
                            newItemId,
                            purchaseOrder.id,
                            item.itemId,
                            item.quantity,
                            item.rate,
                            item.tax,
                            System.currentTimeMillis()
                        )
                    }
                }
            }


            // Fetch and return the updated purchase order
            return findById(purchaseOrder.id!!)
        } catch(e: Exception) {
            throw e
        }
    }

    fun findAll(search: String): List<ListPurchaseOrder> {
        var sql = """
    SELECT po.id as po_id, po.vendor_id, po.status, po.expected_date, po.created_at,
           v.name as vendor_name, 
           poi.id as item_id, poi.po_id, poi.item_id as product_item_id, 
           poi.quantity, poi.rate, poi.tax, poi.created_at as item_created_at, 
           p.name as item_name
    FROM purchase_orders po
    LEFT JOIN vendor v ON po.vendor_id = v.id
    LEFT JOIN purchase_order_items poi ON po.id = poi.po_id
    LEFT JOIN item p ON poi.item_id = p.id
    """

        val params = mutableListOf<Any>()
        val whereClauses = mutableListOf<String>()

        if (!search.isNullOrEmpty()) {
            whereClauses.add("v.name ILIKE ?")
            params.add("%${search.trim()}%")
        }

        // Apply WHERE clause if needed
        if (whereClauses.isNotEmpty()) {
            sql += " WHERE " + whereClauses.joinToString(" AND ")
        }

        // Add ordering
        sql += " ORDER BY po.created_at DESC"

        val purchaseOrderMap = mutableMapOf<String, ListPurchaseOrder>()

        // Using queryForList to properly handle params
        jdbcTemplate.query(
            sql,
            params.toTypedArray(),
            { rs, _ ->
                val poId = rs.getString("po_id")

                // Get or create ListPurchaseOrder
                val purchaseOrder = purchaseOrderMap.getOrPut(poId) {
                    ListPurchaseOrder(
                        id = poId,
                        vendorId = rs.getString("vendor_id"),
                        vendorName = rs.getString("vendor_name"),
                        items = mutableListOf(),
                        status = rs.getString("status"),
                        expectedDate = rs.getLong("expected_date"),
                        createdAt = rs.getLong("created_at")
                    )
                }

                // Add item if it exists
                if (rs.getString("item_id") != null) {
                    val item = ListPurchaseOrderItem(
                        id = rs.getString("item_id"),
                        poId = rs.getString("po_id"),
                        itemId = rs.getString("product_item_id"),
                        itemName = rs.getString("item_name"),
                        quantity = rs.getInt("quantity"),
                        rate = rs.getDouble("rate"),
                        tax = rs.getBigDecimal("tax"),
                        createdAt = rs.getLong("item_created_at")
                    )
                    (purchaseOrder.items as MutableList<ListPurchaseOrderItem>).add(item)
                }
            }
        )

        return purchaseOrderMap.values.toList()
    }

    fun findById(id: String): PurchaseOrder? {
        val sql = """
        SELECT po.*, poi.id as item_id, poi.po_id, poi.item_id as product_item_id, 
               poi.quantity, poi.rate, poi.tax, poi.created_at as item_created_at
        FROM purchase_orders po
        LEFT JOIN purchase_order_items poi ON po.id = poi.po_id
        WHERE po.id = ?
    """

        var purchaseOrder: PurchaseOrder? = null

        jdbcTemplate.query(sql, { rs, _ ->
            if (purchaseOrder == null) {
                purchaseOrder = PurchaseOrder(
                    id = rs.getString("id"),
                    vendorId = rs.getString("vendor_id"),
                    items = mutableListOf(),
                    status = rs.getString("status"),
                    expectedDate = rs.getLong("expected_date"),
                    createdAt = rs.getLong("created_at")
                )
            }

            // Add item if it exists
            if (rs.getString("item_id") != null) {
                val item = PurchaseOrderItem(
                    id = rs.getString("item_id"),
                    poId = rs.getString("po_id"),
                    itemId = rs.getString("product_item_id"),
                    quantity = rs.getInt("quantity"),
                    rate = rs.getDouble("rate"),
                    tax = rs.getBigDecimal("tax"),
                    createdAt = rs.getLong("item_created_at")
                )
                (purchaseOrder!!.items as MutableList<PurchaseOrderItem>).add(item)
            }

            purchaseOrder
        }, id)

        return purchaseOrder
    }

    fun findTotalItemOrdered(monthType: String): Int {
        val sql = """
        SELECT sum(quantity) 
        FROM purchase_order_items 
        WHERE created_at BETWEEN ? AND ?
    """

        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val startDate: Long
        val endDate: Long

        when (monthType.lowercase()) {
            "this_month" -> {
                startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
                endDate = now.withDayOfMonth(1).plusMonths(1).minusNanos(1).toInstant().toEpochMilli()
            }
            "last_month" -> {
                val lastMonth = now.minusMonths(1)
                startDate = lastMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
                endDate = lastMonth.withDayOfMonth(1).plusMonths(1).minusNanos(1).toInstant().toEpochMilli()
            }
            "previous_quarter" -> {
                val currentQuarter = (now.monthValue - 1) / 3 + 1
                val previousQuarter = if (currentQuarter > 1) currentQuarter - 1 else 4
                val year = if (previousQuarter == 4 && currentQuarter == 1) now.year - 1 else now.year

                val startMonth = (previousQuarter - 1) * 3 + 1
                startDate = ZonedDateTime.of(year, startMonth, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
                endDate = ZonedDateTime.of(year, startMonth, 1, 0, 0, 0, 0, ZoneOffset.UTC).plusMonths(3).minusNanos(1).toInstant().toEpochMilli()
            }
            "ytd" -> {
                startDate = ZonedDateTime.of(now.year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
                endDate = now.toInstant().toEpochMilli()
            }
            else -> throw IllegalArgumentException("Invalid monthType: $monthType. Valid options are 'this_month', 'last_month', 'previous_quarter', or 'ytd'")
        }

        return try {
            // Pass the epoch milliseconds directly as Long values
            jdbcTemplate.queryForObject(sql, arrayOf(startDate, endDate), Int::class.java) ?: 0
        } catch (e: Exception) {
            // Log the exception
            // logger.error("Error querying purchase orders", e)
            0 // Return 0 as fallback
        }
    }

    fun deleteById(id: String): Int {
        val sql = "DELETE FROM purchase_orders WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }
}
