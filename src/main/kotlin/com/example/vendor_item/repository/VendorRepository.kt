package com.example.vendor_item.repository

import com.example.vendor_item.model.Vendor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class VendorRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
        Vendor(
            id = rs.getString("id"),
            name = rs.getString("name"),
            phone = rs.getString("phone"),
            address = rs.getString("address"),
            createdAt = rs.getLong("created_at"),
            status = rs.getString("status")
        )
    }

    fun findAll(search: String?, page: Int?, size: Int?, getAll: Boolean?, status: List<String>?): List<Vendor> {
        var sql = "SELECT * FROM vendor"
        val params = mutableListOf<Any>()
        val whereClauses = mutableListOf<String>()

        if (!search.isNullOrEmpty()) {
            whereClauses.add("name ILIKE ?")
            params.add("%$search%")
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

    fun findById(id: String): Vendor? {
        val sql = "SELECT * FROM vendor WHERE id = ?"
        return jdbcTemplate.query(sql, rowMapper, id).firstOrNull()
    }

    fun save(vendor: Vendor): Int {
        val sql = "INSERT INTO vendor (id, name, phone, address, created_at, status) VALUES (?, ?, ?, ?, ?, ?)"
        return jdbcTemplate.update(sql, vendor.id, vendor.name, vendor.phone, vendor.address, vendor.createdAt, vendor.status)
    }

    fun update(vendor: Vendor): Int {
        val sql = "UPDATE vendor SET name = ?, phone = ?, address = ?, created_at = ? WHERE id = ?"
        return jdbcTemplate.update(sql, vendor.name, vendor.phone, vendor.address, vendor.createdAt, vendor.id)
    }

    fun deleteById(id: String): Int {
        val sql = "DELETE FROM vendor WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }

    fun deactivateVendor(id: String): Int{
        val sql = "update vendor set status = 'Inactive' where id = ?"
        return jdbcTemplate.update(sql, id);
    }
}