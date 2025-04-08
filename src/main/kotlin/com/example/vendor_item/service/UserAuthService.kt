package com.example.vendor_item.service

import com.example.vendor_item.config.JwtUtil
import com.example.vendor_item.model.*
import com.example.vendor_item.repository.RefreshTokenRepository
import com.example.vendor_item.repository.UserAuthRepository
import com.example.vendor_item.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class UserAuthService(
    private val userRepository: UserRepository,
    private val userAuthRepository: UserAuthRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val userDetailsService: UserDetailsServiceImpl,
    private val authenticationManager: AuthenticationManager
) {
    fun registerUser(userRegistration: UserRegistration): User? {
        val passwordHash = passwordEncoder.encode(userRegistration.password) //when registering the hash password is stored, not the plain text password
        val user = User(
            id = UUID.randomUUID().toString(),
            name = userRegistration.name,
            email = userRegistration.email,
            createdAt = Instant.now().toEpochMilli(),
            phone = ""
        )
        val userAuth = UserAuth(
            id = UUID.randomUUID().toString(),
            hashPassword = passwordHash,
            userId = user.id!!,
            username = generateUsername(user.id, user.name),
            roles = listOf("Employee"),
            createdAt = Instant.now().toEpochMilli()
        )
        val userCreated = userRepository.save(user)
        userAuthRepository.save(userAuth)
        return userCreated
    }

    fun loginUser(username: String, password: String): AuthResponse {    // Handles user login and JWT generation.
        // does verification of username and password
        val authToken = UsernamePasswordAuthenticationToken(username, password)

        val userAuth = userAuthRepository.findByUsername(username)
        val user = userRepository.findById(userAuth?.userId!!)
        authenticationManager.authenticate(authToken)
        val userDetails = userDetailsService.loadUserByUsername(username)
        val accessToken = user?.let { jwtUtil.generateAccessToken(it, userAuth, userDetails) }
        val refreshToken = jwtUtil.generateRefreshToken(userAuth.username)

        val refreshTokenToBeCreated = RefreshToken(
            id = UUID.randomUUID().toString(),
            token = refreshToken,
            userId = user?.id!!,
            expiresAt = Instant.now().plus(Duration.ofDays(7)).toEpochMilli(),
            createdAt = Instant.now().toEpochMilli()
        )
        refreshTokenRepository.saveRefreshToken(refreshTokenToBeCreated.id, refreshTokenToBeCreated.userId, refreshTokenToBeCreated.token, refreshTokenToBeCreated.expiresAt!!, refreshTokenToBeCreated.createdAt!!)
        return AuthResponse(accessToken = accessToken!!, refreshToken = refreshToken)
    }

    fun refreshToken(refreshToken: String): AuthResponse? {
        val username = jwtUtil.extractUsername(refreshToken)     //the jwt refresh token in Header.Payload.Signature format so from the payload the username is extracted
        val userDetails = userDetailsService.loadUserByUsername(username)
        val userAuth = userAuthRepository.findByUsername(username)
        val user = userRepository.findById(userAuth?.userId!!)
        var token = refreshTokenRepository.findRefreshToken(refreshToken)
        if (token?.token == refreshToken ) { //matches the refresher token and assignes new tokens
            val newAccessToken = jwtUtil.generateAccessToken(user!!, userAuth, userDetails)
            val newRefreshToken = jwtUtil.generateRefreshToken(username)
            token.token = newRefreshToken
            return AuthResponse(accessToken = newAccessToken, refreshToken = newRefreshToken)
        }
        return null
    }

    fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#\$%^&*"
        val random = SecureRandom()
        return (1..8)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    fun generateUsername(userId: String, name: String): String {
        // Extract first and last name from the full name
        val nameParts = name.trim().split("\\s+".toRegex()) // Split by spaces
        val firstName = nameParts[0].lowercase() // First name in lowercase
        val lastName = if (nameParts.size > 1) nameParts[1].lowercase() else "" // Last name in lowercase (if exists)

        // Combine first name, last name, and numeric part to create the username
        return "${firstName}${lastName}"
    }
}