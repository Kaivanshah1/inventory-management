package com.example.vendor_item.repository

import com.example.vendor_item.model.UserAuth
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class UserAuthRepository(private val jdbcTemplate: JdbcTemplate) {
    private val rowMapper = RowMapper<UserAuth> { rs: ResultSet, _: Int ->
        UserAuth(
            id = rs.getString("id"),
            userId = rs.getString("userId"),
            username = rs.getString("username"),
            hashPassword = rs.getString("hashPassword"),
            roles = rs.getArray("roles")?.array?.let { it as Array<String> }?.toList(),
            createdAt = rs.getLong("createdAt")
        )
    }

    fun save(userAuth: UserAuth): Int {
        return jdbcTemplate.update(
            "INSERT INTO UserAuth (id, userId, username, hashPassword, roles, createdAt) VALUES (?, ?, ?, ?, ?, ?)",
            userAuth.id,
            userAuth.userId,
            userAuth.username,
            userAuth.hashPassword,
            userAuth.roles?.toTypedArray(),  // Convert List<String> to Array for proper insertion
            userAuth.createdAt
        )
    }

    fun findById(id: String): UserAuth? {
        val sql = "SELECT * FROM UserAuth WHERE id = ?"
        return jdbcTemplate.query(sql, rowMapper, id).firstOrNull()
    }

    fun findByUsername(username: String): UserAuth? {
        val sql = "SELECT * FROM UserAuth WHERE username = ?"
        return jdbcTemplate.query(sql, rowMapper, username).firstOrNull()
    }

    fun findAll(): List<UserAuth> {
        val sql = "SELECT * FROM UserAuth"
        return jdbcTemplate.query(sql, rowMapper)
    }

    fun deleteById(id: String): Int {
        val sql = "DELETE FROM UserAuth WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }
}