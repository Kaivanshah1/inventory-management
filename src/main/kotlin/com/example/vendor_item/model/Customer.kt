package com.example.vendor_item.model

import java.util.UUID

data class Customer (
    val id: String? = UUID.randomUUID().toString(),
    val name: String?,
    val phoneNo: String?,
    val createdAt: Long?
)

data class ListCustomer(
    val search: String?,
    val page: Int,
    val size: Int?,
    val getAll: Boolean?
)