package com.example.vendor_item.service

import com.example.vendor_item.repository.UserAuthRepository
import com.example.vendor_item.repository.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userAuthRepository: UserAuthRepository, private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {         //this function is of spring security and called during authentication to load user details by their username
        val userAuth = userAuthRepository.findByUsername(username)

        return User(
            userAuth?.username,
            userAuth?.hashPassword,
            userAuth?.roles?.map { it -> org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_$it")}
        )
    }
}