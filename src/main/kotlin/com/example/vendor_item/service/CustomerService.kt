package com.example.vendor_item.service

import com.example.vendor_item.model.Customer
import com.example.vendor_item.model.ListCustomer
import com.example.vendor_item.repository.CustomerRepository
import org.springframework.stereotype.Service

@Service
class CustomerService(private val customerRepository: CustomerRepository) {

    fun createCustomer(customer: Customer): String {
        customerRepository.save(customer)
        return customer.id!!
    }

    fun updateCustomer(customer: Customer): Boolean {
        return customerRepository.update(customer) > 0
    }

    fun getCustomer(id: String): Customer? = customerRepository.findById(id)

    fun getAllCustomers(listCustomer: ListCustomer): List<Customer> = customerRepository.findAll(listCustomer.search, listCustomer.page, listCustomer.size, listCustomer.getAll)

    fun getByPhoneNo(phoneNo: String): Customer? {
        return customerRepository.getByPhoneNo(phoneNo)
    }

    fun deleteCustomer(id: String) = customerRepository.deleteById(id)
}
