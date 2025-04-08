package com.example.vendor_item.controller

import com.example.vendor_item.model.Customer
import com.example.vendor_item.model.ListCustomer
import com.example.vendor_item.service.CustomerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/customers")
class CustomerController(private val customerService: CustomerService) {

    @PostMapping("/create")
    fun createCustomer(@RequestBody customer: Customer): String {
        return customerService.createCustomer(customer)
    }

    @GetMapping("/get/{id}")
    fun getCustomer(@PathVariable id: String): Customer? {
        return customerService.getCustomer(id)
    }

    @PostMapping("/list")
    fun getAllCustomers(@RequestBody listCustomer: ListCustomer): List<Customer> {
        return customerService.getAllCustomers(listCustomer)
    }

    @PostMapping("/update")
    fun updateCustomer(
        @RequestBody customer: Customer
    ): String {
        val updated = customerService.updateCustomer(customer)
        return if (updated) "Customer updated successfully." else "Customer not found or update failed."
    }

    @PostMapping("/phone-no")
    fun getByPhoneNo(
        @RequestParam(required = false) phone: String
    ): ResponseEntity<Customer>? {
        if (phone.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(null)
        }

        return ResponseEntity.ok(customerService.getByPhoneNo(phone))
    }
}