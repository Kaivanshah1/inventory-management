package com.example.vendor_item.repository

import com.example.vendor_item.model.PurchaseOrderItem
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class PurchaseOrderItemRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
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

    fun save(item: PurchaseOrderItem): Int {
        val sql = "INSERT INTO purchase_order_items (id, po_id, item_id, quantity, rate, tax, created_at) VALUES (?, ?, ?, ?, ?, ?)"
        return jdbcTemplate.update(sql, item.id, item.poId, item.itemId, item.quantity, item.rate, item.tax, item.createdAt)
    }

    fun findByPoId(poId: String): List<PurchaseOrderItem> {
        val sql = "SELECT * FROM purchase_order_items WHERE po_id = ?"
        return jdbcTemplate.query(sql, rowMapper, poId)
    }

    fun deleteById(id: String): Int {
        val sql = "DELETE FROM purchase_order_items WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }
}
