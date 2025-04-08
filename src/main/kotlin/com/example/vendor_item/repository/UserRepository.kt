package com.example.vendor_item.repository

import com.example.vendor_item.model.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class UserRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
        User(
            id = rs.getString("id"),
            name = rs.getString("name"),
            email = rs.getString("email"),
            phone = rs.getString("phone"),
            createdAt = rs.getLong("created_at")
        )
    }

    fun findAll(search: String?, page: Int?, size: Int?, getAll: Boolean?): List<User> {
        var sql = "SELECT * FROM users"
        val params = mutableListOf<Any>()
        val whereClauses = mutableListOf<String>()

        if (!search.isNullOrEmpty()) {
            whereClauses.add("name ILIKE ?")
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

    fun findById(id: String): User? {
        val sql = "SELECT * FROM users WHERE id = ?"
        return jdbcTemplate.query(sql, rowMapper, id).firstOrNull()
    }

    fun save(user: User): User? {
        val sql = "INSERT INTO users (id, name, email, phone, created_at) VALUES (?, ?, ?, ?, ?)"
        jdbcTemplate.update(sql, user.id, user.name, user.email, user.phone, user.createdAt)
        return findById(user.id!!);
    }

    fun update(user: User): Int {
        val sql = "UPDATE users SET name = ?, email = ?, phone = ?, created_at = ? WHERE id = ?"
        return jdbcTemplate.update(sql, user.name, user.email, user.phone, user.createdAt, user.id)
    }

    fun deleteById(id: String): Int {
        val sql = "DELETE FROM users WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }
}