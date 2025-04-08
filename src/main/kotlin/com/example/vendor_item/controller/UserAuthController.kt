package com.example.vendor_item.controller

import com.example.vendor_item.model.AuthResponse
import com.example.vendor_item.model.User
import com.example.vendor_item.model.UserLogin
import com.example.vendor_item.model.UserRegistration
import com.example.vendor_item.service.UserAuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("/api/v1/auth")
class UserAuthController(private val userAuthService: UserAuthService) {

    @PostMapping("/login")
    fun login(@RequestBody userLogin: UserLogin): ResponseEntity<AuthResponse>{
        return ResponseEntity.ok(userAuthService.loginUser(userLogin.username, userLogin.password))
    }

    @PostMapping("/register")
    fun register(@RequestBody userRegistration: UserRegistration): ResponseEntity<User?>{
        return ResponseEntity.ok(userAuthService.registerUser(userRegistration))
    }
}