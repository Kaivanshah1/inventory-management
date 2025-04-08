package com.example.vendor_item.controller

import com.example.vendor_item.model.ListUser
import com.example.vendor_item.model.User
import com.example.vendor_item.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    @PostMapping("/list")
    fun getAllUsers(@RequestBody listUser: ListUser): ResponseEntity<List<User>> {
        return ResponseEntity.ok(userService.getAllUsers(listUser))
    }

    @GetMapping("/get/{id}")
    fun getUserById(@PathVariable id: String): ResponseEntity<User> {
        val user = userService.getUserById(id)
        return if (user != null) ResponseEntity.ok(user) else ResponseEntity.notFound().build()
    }

    @PostMapping("/create")
    fun createUser(@RequestBody user: User): ResponseEntity<User?> {
       return ResponseEntity.ok(userService.createUser(user))
    }

    @PostMapping("/update")
    fun updateUser(@RequestBody user: User): ResponseEntity<String> {
        return if (userService.updateUser(user) > 0)
            ResponseEntity.ok("User updated successfully")
        else
            ResponseEntity.badRequest().body("Failed to update user")
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: String): ResponseEntity<String> {
        return if (userService.deleteUser(id) > 0)
            ResponseEntity.ok("User deleted successfully")
        else
            ResponseEntity.notFound().build()
    }
}