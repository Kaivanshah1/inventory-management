package com.example.vendor_item.config

import com.example.vendor_item.model.User
import com.example.vendor_item.model.UserAuth
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtUtil {
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.access.expiration}")
    private var accessTokenExpiration: Long = 0

    @Value("\${jwt.refresh.expiration}")
    private var refreshTokenExpiration: Long = 0

    fun generateAccessToken(user: User, userAuth: UserAuth, userDetails: UserDetails): String {
            return Jwts.builder()
            .setSubject(userDetails.username) // Username as subject
            .setHeaderParam("alg", "HS256")
            .setHeaderParam("typ", "JWT") // Setting Token Type
            .claim("userId", user.id) // Adding userId
            .claim("email", user.email) // Adding email
            .claim("roles", userAuth.roles) // Adding roles
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour expiry
            .signWith(getSignKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    fun generateRefreshToken(username: String): String {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // 7 days expiry
            .signWith(getSignKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    private fun getSignKey(): Key {
        val keyBytes = Decoders.BASE64.decode(jwtSecret)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }

    fun extractRoles(token: String): List<String> {
        val claims = extractAllClaims(token)
        return claims["roles"] as List<String>
    }

    fun isTokenExpired(token: String): Boolean {
        return extractAllClaims(token).expiration.before(Date())
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(getSignKey())
            .build()
            .parseClaimsJws(token)
            .body
    }

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        return (extractUsername(token) == userDetails.username && !isTokenExpired(token))
    }
}