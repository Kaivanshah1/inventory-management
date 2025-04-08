package com.example.vendor_item.repository

import com.example.vendor_item.model.Item
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class ItemRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
        Item(
            id = rs.getString("id"),
            name = rs.getString("name"),
            price = rs.getDouble("price"),
            stockLevel = rs.getInt("stock_level"),
            reorderPoint = rs.getInt("reorder_point"),
            vendorId = rs.getString("vendor_id"),
            description = rs.getString("description"),
            imageUrl = rs.getString("image_url"),
            createdAt = rs.getLong("created_at"),
            status = rs.getString("status")
        )
    }

    fun findAll(search: String?, page: Int?, size: Int?, getAll: Boolean?, vendorId: String? = null, status: List<String>?): List<Item> {
        var sql = "SELECT * FROM item"
        val params = mutableListOf<Any>()
        val whereClauses = mutableListOf<String>()

        if (!search.isNullOrEmpty()) {
            whereClauses.add("name ILIKE ?")
            params.add("%$search%")
        }

        if (!vendorId.isNullOrEmpty()) {
            whereClauses.add("vendor_id = ?")
            params.add(vendorId)
        }

        if (!status.isNullOrEmpty()) {
            whereClauses.add("status IN (${status.joinToString(",") { "?" }})")
            params.addAll(status)
        }

        // add where conditions if any
        if (whereClauses.isNotEmpty()) {
            sql += " WHERE " + whereClauses.joinToString(" AND ")
        }

        if (getAll != true) { // Only add limit and offset if getAll is NOT true.
            val limit = if (size != null && size > 0) size else 10
            val offset = if (page != null && page > 0) (page - 1) * limit else 0
            sql += " LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return jdbcTemplate.query(sql, params.toTypedArray(), rowMapper)
    }

    fun findById(id: String): Item? {
        val sql = "SELECT * FROM item WHERE id = ?"
        return jdbcTemplate.query(sql, rowMapper, id).firstOrNull()
    }

    fun save(item: Item): Int {
        try {
            val sql =
                "INSERT INTO item (id, name, price, stock_level, reorder_point, description, image_url, created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            return jdbcTemplate.update(
                sql,
                item.id,
                item.name,
                item.price,
                item.stockLevel,
                item.reorderPoint,
                item.description,
                item.imageUrl,
                item.createdAt,
                item.status
            )
        }catch (e: Exception){
            throw e;
        }
    }

        fun update(item: Item): Int {
            val stockSql = "SELECT stock_level, reorder_point FROM item WHERE id = ?"
            val result: Map<String, Any>? = try {
                jdbcTemplate.queryForMap(stockSql, item.id)
            } catch (e: EmptyResultDataAccessException) {
                println("Item with id ${item.id} not found.")
                return 0 // Or throw exception
            }

            if (result == null) {
                return 0
            }

            val newStockLevel = item.stockLevel // Assuming item.stockLevel is the new value

            //Safely extract the reorderPoint from the Map, providing a default value
            //Or throw an exception if the value is missing/invalid
            val reorderPoint: Int = (result["reorder_point"] as? Int) ?: throw IllegalStateException("Invalid reorder_point in database")

            val newStatus = when {
                newStockLevel > 0 && newStockLevel > reorderPoint -> "In Stock"
                newStockLevel > 0 && newStockLevel <= reorderPoint -> "Low Stock"
                else -> "Out of Stock"
            }

            val sql = "UPDATE item SET name = ?, price = ?, stock_level = ?, reorder_point = ?, vendor_id = ?, description = ?, image_url = ?, created_at = ?, status = ? WHERE id = ?"
            return jdbcTemplate.update(
                sql,
                item.name,
                item.price,
                newStockLevel,
                reorderPoint,
                item.vendorId,
                item.description,
                item.imageUrl,
                item.createdAt,
                newStatus,
                item.id
            )
        }


    fun deleteById(id: String): Int {
        val sql = "DELETE FROM item WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }

    fun stockInHand(): Int{
        val sql = "select sum(stock_level) from item";
        return jdbcTemplate.queryForObject(sql, Int::class.java) ?: 0
    }

    fun updatePurchaseOrderItem(poId: String, itemId: String, newQuantity: Int): Int {
        try {
            // Try to retrieve the old quantity from the purchase_order_items table
            val oldQuantity = try {
                val oldQuantitySql = """
            SELECT quantity FROM purchase_order_items 
            WHERE po_id = ? AND item_id = ?
            """.trimIndent()

                jdbcTemplate.queryForObject(
                    oldQuantitySql,
                    Int::class.java,
                    poId,
                    itemId
                )
            } catch (e: EmptyResultDataAccessException) {
                // If no record found, treat as a new PO item with quantity 0
                0
            }

            // Calculate the difference between new and old quantities
            val quantityDifference = newQuantity - oldQuantity!!

            // First get the current item details to check reorder point
            val getItemSql = """
        SELECT stock_level, reorder_point FROM item WHERE id = ?
        """.trimIndent()

            val itemDetails = jdbcTemplate.queryForMap(getItemSql, itemId)
            val currentStockLevel = itemDetails["stock_level"] as Int
            val reorderPoint = itemDetails["reorder_point"] as Int

            // Calculate new stock level
            val newStockLevel = currentStockLevel + quantityDifference

            // Determine the new status based on stock level and reorder point
            val newStatus = when {
                newStockLevel > 0 && newStockLevel > reorderPoint -> "In Stock"
                newStockLevel > 0 && newStockLevel <= reorderPoint -> "Low Stock"
                else -> "Out of Stock"
            }

            // Update the item stock level and status
            val updateItemSql = """
        UPDATE item SET stock_level = ?, status = ? WHERE id = ?
        """.trimIndent()

            return jdbcTemplate.update(updateItemSql, newStockLevel, newStatus, itemId)
        } catch (e: Exception) {
            throw e
        }
    }

    fun updateBillItem(billId: String, itemId: String, newQuantity: Int): Int {
        try {
            // Try to retrieve the old quantity from the bill_items table
            val oldQuantity = try {
                val oldQuantitySql = """
                SELECT quantity FROM billitems 
                WHERE b_id = ? AND item_id = ?
            """.trimIndent()

                jdbcTemplate.queryForObject(
                    oldQuantitySql,
                    Int::class.java,
                    billId,
                    itemId
                )
            } catch (e: EmptyResultDataAccessException) {
                // If no record found, treat as a new bill item with quantity 0
                0
            }

            // Calculate the difference between new and old quantities
            val quantityDifference = newQuantity - oldQuantity!!

            // Get the current item details to check reorder point
            val getItemSql = """
            SELECT stock_level, reorder_point FROM item WHERE id = ?
        """.trimIndent()

            val itemDetails = jdbcTemplate.queryForMap(getItemSql, itemId)
            val currentStockLevel = itemDetails["stock_level"] as Int
            val reorderPoint = itemDetails["reorder_point"] as Int

            // Calculate new stock level - for bills, we subtract from inventory
            // (opposite of purchase orders which add to inventory)
            val newStockLevel = currentStockLevel - quantityDifference

            // Determine the new status based on stock level and reorder point
            val newStatus = when {
                newStockLevel > 0 && newStockLevel > reorderPoint -> "In Stock"
                newStockLevel > 0 && newStockLevel <= reorderPoint -> "Low Stock"
                else -> "Out of Stock"
            }

            // Update the item stock level and status
            val updateItemSql = """
            UPDATE item SET stock_level = ?, status = ? WHERE id = ?
        """.trimIndent()

            return jdbcTemplate.update(updateItemSql, newStockLevel, newStatus, itemId)
        } catch (e: Exception) {
            throw e
        }
    }
}