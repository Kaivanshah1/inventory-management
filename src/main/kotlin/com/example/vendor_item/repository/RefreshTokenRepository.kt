package com.example.vendor_item.repository

import com.example.vendor_item.model.RefreshToken
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class RefreshTokenRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper<RefreshToken> { rs: ResultSet, _: Int ->
        RefreshToken(
            id = rs.getString("id"),
            userId = rs.getString("userId"),
            token = rs.getString("token"),
            expiresAt = rs.getLong("expiresat"),
            createdAt = rs.getLong("createdat")
        )
    }

    fun saveRefreshToken(id: String, userId: String, token: String, expiresAt: Long, updatedAt: Long) {
        val sql = "INSERT INTO refresh_tokens (id, userid, token, expiresat, createdat) VALUES (?, ?, ?, ?, ?)"
        jdbcTemplate.update(sql, id, userId, token, expiresAt, updatedAt)
    }

    fun findRefreshToken(token: String): RefreshToken? {
        val sql = "SELECT * FROM refresh_tokens WHERE token = ?"
        return jdbcTemplate.query(sql, arrayOf(token)) { rs, _ ->
            RefreshToken(
                id = rs.getString("id"),
                userId = rs.getString("userid"),
                token = rs.getString("token"),
                expiresAt = rs.getLong("expiresat"),
                createdAt = rs.getLong("createdat")
            )
        }.firstOrNull()
    }

    fun deleteByUserAuth(userId: String) {
        val sql = "DELETE FROM refresh_tokens WHERE userid = ?"
        jdbcTemplate.update(sql, userId)
    }

    fun updateRefreshToken(id: String, newToken: String) {
        val sql = """
        UPDATE refresh_tokens 
        SET token = ?, expiresat = ? 
        WHERE id = ?
    """
        val expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
        jdbcTemplate.update(sql, newToken, expiresAt, id)
    }
}