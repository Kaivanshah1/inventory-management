package com.example.vendor_item.controller

import com.example.vendor_item.model.ListVendor
import com.example.vendor_item.model.Vendor
import com.example.vendor_item.service.VendorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/vendors")
class VendorController(private val vendorService: VendorService) {

    @PostMapping("/list")
    fun getAllVendors(@RequestBody listVendor: ListVendor): ResponseEntity<List<Vendor>> {
        val listVendors = vendorService.getAllVendors(listVendor)
        return ResponseEntity.ok(listVendors)
    }

    @GetMapping("/get/{id}")
    fun getVendorById(@PathVariable id: String): ResponseEntity<Vendor> {
        val vendor = vendorService.getVendorById(id)
        return if (vendor != null) ResponseEntity.ok(vendor) else ResponseEntity.notFound().build()
    }

    @PostMapping("/create")
    fun createVendor(@RequestBody vendor: Vendor): ResponseEntity<String> {
        return if (vendorService.createVendor(vendor) > 0)
            ResponseEntity.ok("Vendor created successfully")
        else
            ResponseEntity.badRequest().body("Failed to create vendor")
    }


    @PostMapping("/update")
    fun updateVendor(@RequestBody vendor: Vendor): ResponseEntity<String> {
        return if (vendorService.updateVendor(vendor) > 0)
            ResponseEntity.ok("Vendor updated successfully")
        else
            ResponseEntity.badRequest().body("Failed to update vendor")
    }

    @DeleteMapping("/delete/{id}")
    fun deleteVendor(@PathVariable id: String): ResponseEntity<String> {
        return if (vendorService.deleteVendor(id) > 0)
            ResponseEntity.ok("Vendor deleted successfully")
        else
            ResponseEntity.notFound().build()
    }

    @GetMapping("/deactivate/{id}")
    fun deactivateVendor(@PathVariable id: String): ResponseEntity<Int> {
        return ResponseEntity.ok(vendorService.deactivateVendor(id));
    }
}