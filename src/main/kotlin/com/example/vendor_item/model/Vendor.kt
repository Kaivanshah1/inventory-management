package com.example.vendor_item.model

data class Vendor(
    val id: String?,
    val name: String,
    val phone: String?,
    val address: String?,
    val createdAt: Long?,
    val status: String?
)

data class ListVendor(
    val search: String?,
    val page: Int?,
    val size: Int?,
    val getAll: Boolean?,
    val status: List<String>?
)