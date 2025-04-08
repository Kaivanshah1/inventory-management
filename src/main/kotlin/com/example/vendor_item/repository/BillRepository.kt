package com.example.vendor_item.repository

import com.example.vendor_item.model.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class BillRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
        Bill(
            id = rs.getString("id"),
            customerName = rs.getString("customer_name"),
            customerPhoneNo = rs.getString("customer_phone_no"),
            status = rs.getString("status"),
            items = emptyList(),
            createdAt = rs.getLong("created_at")
        )
    }

    private val billItemRowMapper = RowMapper { rs, _ ->
        BillItems(
            id = rs.getString("id"),
            bId = rs.getString("b_id"),
            itemId = rs.getString("item_id"),
            quantity = rs.getInt("quantity"),
            rate = rs.getDouble("price"),
            tax = rs.getBigDecimal("tax"),
            createdAt = rs.getLong("created_at")
        )
    }

    fun save(bill: Bill): Bill? {
        try {
            val billSql =
                "INSERT INTO bill (id, customer_name, customer_phone_no, status, created_at) VALUES (?, ?, ?, ?, ?)"
            val billResult = jdbcTemplate.update(
                billSql,
                bill.id,
                bill.customerName,
                bill.customerPhoneNo,
                bill.status,
                bill.createdAt
            )

            if (bill.items.isNotEmpty()) {
                val itemSql =
                    "INSERT INTO billitems (id, b_id, item_id, quantity, rate, tax, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
                bill.items.forEach { item ->
                    jdbcTemplate.update(
                        itemSql,
                        UUID.randomUUID().toString(),
                        bill.id,
                        item.itemId,
                        item.quantity,
                        item.rate,
                        item.tax,
                        item.createdAt
                    )
                }
            }
            return findById(bill.id!!)
        }catch (e: Exception){
            throw e;
        }
    }

    fun findAll(): List<ListBill> {
        val sql = """
        SELECT b.id as id, b.customer_name, b.customer_phone_no, b.status, b.created_at,
               bi.id as bi_id, bi.b_id, bi.item_id, i.name as item_name, bi.quantity, bi.rate, bi.tax, bi.created_at as item_created_at
        FROM bill b
        LEFT JOIN billitems bi ON b.id = bi.b_id
        LEFT JOIN item i ON bi.item_id = i.id
    """

        val billMap = mutableMapOf<String, ListBill>()

        jdbcTemplate.query(sql) { rs, _ ->
            val id = rs.getString("id")
            val bill = billMap.getOrPut(id) {
                ListBill(
                    id = id,
                    customerName = rs.getString("customer_name"),
                    customerPhoneNo = rs.getString("customer_phone_no"),
                    status = rs.getString("status"),
                    items = mutableListOf(),
                    createdAt = rs.getLong("created_at")
                )
            }

            if (rs.getString("bi_id") != null) {
                val item = ListBillItem(
                    id = rs.getString("bi_id"), // Using the correct column
                    bId = rs.getString("b_id"),
                    itemId = rs.getString("item_id"),
                    itemName = rs.getString("item_name"), // Added field from joined items table
                    quantity = rs.getInt("quantity"),
                    rate = rs.getDouble("rate"), // Changed from "price" to "rate" to match DB schema
                    tax = rs.getDouble("tax"), // Changed from BigDecimal to Double to match data class
                    createdAt = rs.getLong("item_created_at") // Use aliased column to avoid ambiguity
                )
                (bill.items as MutableList<ListBillItem>).add(item)
            }
        }

        return billMap.values.toList()
    }

    fun findById(id: String): Bill? {
        val sql = """
            SELECT b.*, bi.id as id, bi.b_id, bi.item_id, bi.quantity, bi.rate, bi.tax, bi.created_at as item_created_at
            FROM bill b
            LEFT JOIN billitems bi ON b.id = bi.b_id
            WHERE b.id = ?
        """

        var bill: Bill? = null

        jdbcTemplate.query(sql, { rs, _ ->
            if (bill == null) {
                bill = Bill(
                    id = rs.getString("id"),
                    customerName = rs.getString("customer_name"),
                    customerPhoneNo = rs.getString("customer_phone_no"),
                    status = rs.getString("status"),
                    items = mutableListOf(),
                    createdAt = rs.getLong("created_at")
                )
            }

            if (rs.getString("item_id") != null) {
                val item = BillItems(
                    id = rs.getString("id"),
                    bId = rs.getString("b_id"),
                    itemId = rs.getString("item_id"),
                    quantity = rs.getInt("quantity"),
                    rate = rs.getDouble("rate"),
                    tax = rs.getBigDecimal("tax"),
                    createdAt = rs.getLong("created_at")
                )
                (bill!!.items as MutableList<BillItems>).add(item)
            }
        }, id)

        return bill
    }

    fun update(bill: Bill): Bill? {
        try {
            println("updating bill: $bill")

            // Step 1: Get the current items for this bill from the database
            val currentItemsQuery = "SELECT id FROM billitems WHERE b_id = ?"
            val currentItemIds = jdbcTemplate.queryForList(currentItemsQuery, String::class.java, bill.id)

            // Step 2: Extract ids of items in the incoming request
            val updatedItemIds = bill.items.mapNotNull { it.id }

            // Step 3: Find items that are in the database but not in the request (need to be deleted)
            val itemsToDelete = currentItemIds.filter { currentId ->
                !updatedItemIds.contains(currentId)
            }

            // Step 4: Delete items that are no longer present
            if (itemsToDelete.isNotEmpty()) {
                val deleteItemSql = "DELETE FROM billitems WHERE id = ?"
                itemsToDelete.forEach { itemId ->
                    jdbcTemplate.update(deleteItemSql, itemId)
                }
            }

            // Step 5: Update the bill details
            val billSql = "UPDATE bill SET customer_name = ?, customer_phone_no = ?, status = ? WHERE id = ?"
            jdbcTemplate.update(
                billSql,
                bill.customerName,
                bill.customerPhoneNo,
                bill.status,
                bill.id,
            )

            // Step 6: Process all items (update existing ones and insert new ones)
            if (bill.items.isNotEmpty()) {
                val updateItemSql = "UPDATE billitems SET item_id = ?, quantity = ?, rate = ?, tax = ? WHERE id = ?"
                val insertItemSql = "INSERT INTO billitems (id, b_id, item_id, quantity, rate, tax, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"

                bill.items.forEach { item ->
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
                            bill.id,
                            item.itemId,
                            item.quantity,
                            item.rate,
                            item.tax,
                            System.currentTimeMillis()
                        )
                    }
                }
            }

            // Fetch and return the updated bill
            return findById(bill.id!!)
        } catch(e: Exception) {
            throw e
        }
    }

    fun deleteById(id: String): Int {
        val sql = "DELETE FROM bill WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }

    fun getMonthlySales(): List<Pair<String, Double>> {
        val sql = """
            SELECT 
                TO_CHAR(TO_TIMESTAMP(b.created_at / 1000), 'YYYY-MM') AS month,
                SUM(bi.quantity * bi.rate) AS total_sales
            FROM billitems bi
            JOIN bill b ON bi.b_id = b.id
            GROUP BY month
            ORDER BY month
        """

        return jdbcTemplate.query(sql) { rs, _ ->
            rs.getString("month") to rs.getDouble("total_sales")
        }
    }

    fun getTotalBills(): Int {
        val sql = """
        SELECT COUNT(*) 
        FROM bill 
        WHERE 
            EXTRACT(MONTH FROM TO_TIMESTAMP(created_at/1000)) = EXTRACT(MONTH FROM CURRENT_DATE)
            AND EXTRACT(YEAR FROM TO_TIMESTAMP(created_at/1000)) = EXTRACT(YEAR FROM CURRENT_DATE)
    """

        return jdbcTemplate.queryForObject(sql, Int::class.java) ?: 0
    }

    fun getTopSellingItems(period: String = "this_month", limit: Int = 2): List<Item> {
        val timeCondition = when (period.lowercase()) {
            "this_month" -> """
            EXTRACT(MONTH FROM TO_TIMESTAMP(bi.created_at/1000)) = EXTRACT(MONTH FROM CURRENT_DATE) AND
            EXTRACT(YEAR FROM TO_TIMESTAMP(bi.created_at/1000)) = EXTRACT(YEAR FROM CURRENT_DATE)
        """
            "last_month" -> """
            (EXTRACT(MONTH FROM TO_TIMESTAMP(bi.created_at/1000)) = EXTRACT(MONTH FROM CURRENT_DATE) - 1 AND
             EXTRACT(YEAR FROM TO_TIMESTAMP(bi.created_at/1000)) = EXTRACT(YEAR FROM CURRENT_DATE))
            OR
            (EXTRACT(MONTH FROM TO_TIMESTAMP(bi.created_at/1000)) = 12 AND
             EXTRACT(MONTH FROM CURRENT_DATE) = 1 AND
             EXTRACT(YEAR FROM TO_TIMESTAMP(bi.created_at/1000)) = EXTRACT(YEAR FROM CURRENT_DATE) - 1)
        """
            "all_time" -> "1=1" // No time restriction
            else -> throw IllegalArgumentException("Invalid period. Use 'this_month', 'last_month', or 'all_time'")
        }

        val sql = """
        SELECT 
            i.id, i.name, i.price, i.stock_level as stockLevel, i.reorder_point as reorderPoint, 
            i.vendor_id as vendorId, i.description, i.image_url as imageUrl, 
            i.created_at as createdAt, i.status,
            top_items.total_quantity as total_quantity
        FROM 
            item i
        JOIN 
            (SELECT 
                item_id, 
                SUM(quantity) as total_quantity
             FROM 
                billitems bi
             WHERE 
                $timeCondition
             GROUP BY 
                item_id
             ORDER BY 
                total_quantity DESC
             LIMIT ?
            ) top_items ON i.id = top_items.item_id
        ORDER BY 
            top_items.total_quantity DESC
    """

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                Item(
                    id = rs.getString("id"),
                    name = rs.getString("name"),
                    price = rs.getDouble("price"),
                    stockLevel = rs.getInt("stockLevel"),
                    reorderPoint = rs.getInt("reorderPoint"),
                    vendorId = rs.getString("vendorId"),
                    description = rs.getString("description"),
                    imageUrl = rs.getString("imageUrl"),
                    createdAt = rs.getLong("createdAt"),
                    status = rs.getString("status"),
                    totalQuantity = rs.getInt("total_quantity")
                )
            },
            limit
        )
    }

    fun totalItemSold(): Int {
        val sql = """
        SELECT SUM(quantity) FROM billitems 
        WHERE 
        EXTRACT(MONTH FROM TO_TIMESTAMP(created_at/1000)) = EXTRACT(MONTH FROM CURRENT_DATE)
        AND EXTRACT(YEAR FROM TO_TIMESTAMP(created_at/1000)) = EXTRACT(YEAR FROM CURRENT_DATE)
    """.trimIndent()
        return jdbcTemplate.queryForObject(sql, Int::class.java) ?: 0
    }

    fun countNoOfRows(): Int? {
        val sql = """
        select count(*) from bill;
    """.trimIndent()

        return jdbcTemplate.queryForObject(sql, Int::class.java)
    }
}