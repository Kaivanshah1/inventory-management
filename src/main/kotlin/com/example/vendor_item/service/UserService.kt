package com.example.vendor_item.service

import com.example.vendor_item.model.ListUser
import com.example.vendor_item.model.User
import com.example.vendor_item.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class UserService(private val userRepository: UserRepository) {

    fun getAllUsers(listUser: ListUser): List<User> {
        return userRepository.findAll(listUser.search, listUser.page, listUser.size, listUser.getAll)
    }

    fun getUserById(id: String): User? {
        return userRepository.findById(id)
    }

    fun createUser(user: User): User? {
        val userToBeCreated = user.copy(
            id = UUID.randomUUID().toString(),
            createdAt = Instant.now().toEpochMilli()
        )

        return userRepository.save(userToBeCreated)
    }

    fun updateUser(user: User): Int {
        return userRepository.update(user)
    }

    fun deleteUser(id: String): Int {
        return userRepository.deleteById(id)
    }
}
