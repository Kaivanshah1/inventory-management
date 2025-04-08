package com.example.vendor_item.repository

import com.example.vendor_item.model.Customer
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository

@Repository
class CustomerRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
        Customer(
            id = rs.getString("id"),
            name = rs.getString("name"),
            phoneNo = rs.getString("phone_no"),
            createdAt = rs.getLong("created_at")
        )
    }

    fun save(customer: Customer): Int {
        val sql = """
            INSERT INTO customers (id, name, phone_no, created_at)
            VALUES (?, ?, ?, ?)
        """
        return jdbcTemplate.update(
            sql,
            customer.id,
            customer.name,
            customer.phoneNo,
            customer.createdAt
        )
    }

    fun update(customer: Customer): Int {
        val sql = """
        UPDATE customers 
        SET name = ?, phone_no = ?, created_at = ?
        WHERE id = ?
    """
        return jdbcTemplate.update(
            sql,
            customer.name,
            customer.phoneNo,
            customer.createdAt,
            customer.id
        )
    }

    fun findById(id: String): Customer? {
        val sql = "SELECT * FROM customers WHERE id = ?"
        return jdbcTemplate.query(sql, rowMapper, id).firstOrNull()
    }

    fun findAll(
        search: String?,
        page: Int?,
        size: Int?,
        getAll: Boolean? = false
    ): List<Customer> {
        var sql = "SELECT * FROM customers"
        val params = mutableListOf<Any>()
        val whereClauses = mutableListOf<String>()

        if (!search.isNullOrEmpty()) {
            whereClauses.add("(name ILIKE ? OR phone_no ILIKE ?)")
            params.add("%$search%")
            params.add("%$search%")
        }

        if (whereClauses.isNotEmpty()) {
            sql += " WHERE " + whereClauses.joinToString(" AND ")
        }

        if (getAll != true) {
            val limit = if (size != null && size > 0) size else 10
            val offset = if (page != null && page > 0) (page - 1) * limit else 0
            sql += " LIMIT ? OFFSET ?"
            params.add(limit)
            params.add(offset)
        }

        return jdbcTemplate.query(sql, params.toTypedArray(), rowMapper)
    }

    fun deleteById(id: String): Int {
        val sql = "DELETE FROM customers WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }

    fun getByPhoneNo(phoneNo: String): Customer? {
        val sql = "select * from customers where phone_no = ?"
        return jdbcTemplate.queryForObject(sql, rowMapper, phoneNo)
    }
}
