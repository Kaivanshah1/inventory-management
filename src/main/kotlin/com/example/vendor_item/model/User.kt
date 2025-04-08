package com.example.vendor_item.model

data class User(
    val id: String?,
    val name: String,
    val email: String?,
    val phone: String?,
    val createdAt: Long?
)

data class ListUser(
    val search: String?,
    val page: Int?,
    val size: Int?,
    val getAll: Boolean?
)

data class UserAuth(
    val id: String?,
    val userId: String,
    val username: String,
    val hashPassword: String,
    val roles: List<String>?,
    val createdAt: Long?
)

data class RefreshToken(
    val id: String,
    val userId: String,
    var token: String,
    val expiresAt: Long?,
    val createdAt: Long?
)

data class UserRegistration(
    val name: String,
    val email: String,
    val password: String,
)

data class UserLogin(
    val username: String,
    val password: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)