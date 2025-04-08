package com.example.vendor_item.service
import com.example.vendor_item.model.ListVendor
import com.example.vendor_item.model.Vendor
import com.example.vendor_item.repository.VendorRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class VendorService(private val vendorRepository: VendorRepository) {

    fun getAllVendors(listVendor: ListVendor): List<Vendor> {
        return vendorRepository.findAll(listVendor.search, listVendor.page, listVendor.size, listVendor.getAll, listVendor.status)
    }

    fun getVendorById(id: String): Vendor? {
        return vendorRepository.findById(id)
    }

    fun createVendor(vendor: Vendor): Int {
        val vendorToBeCreated = vendor.copy(
            id = UUID.randomUUID().toString(),
            status = "Active",
            createdAt = Instant.now().toEpochMilli()
        )
        return vendorRepository.save(vendorToBeCreated)
    }

    fun updateVendor(vendor: Vendor): Int {
        return vendorRepository.update(vendor)
    }

    fun deleteVendor(id: String): Int {
        return vendorRepository.deleteById(id)
    }

    fun deactivateVendor(id: String): Int{
        return vendorRepository.deactivateVendor(id);
    }

}
