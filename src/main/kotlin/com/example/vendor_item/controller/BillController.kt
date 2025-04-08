package com.example.vendor_item.controller

import com.example.vendor_item.model.Bill
import com.example.vendor_item.model.Item
import com.example.vendor_item.model.ListBill
import com.example.vendor_item.service.BillService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/bills")
class BillController(private val billService: BillService) {

    @PostMapping("/create")
    fun createBill(@RequestBody bill: Bill): ResponseEntity<Bill?> {
        val result = billService.createBill(bill)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/list")
    fun listBill(): ResponseEntity<List<ListBill>> {
        val result = billService.getAllBills()
        return ResponseEntity.ok(result)
    }

    @PostMapping("/update")
    fun update(@RequestBody bill: Bill): ResponseEntity<Bill?>{
        return ResponseEntity.ok(billService.update(bill));
    }

    @GetMapping("/get/{id}")
    fun getBillById(@PathVariable id: String): ResponseEntity<Bill?> {
        val bill = billService.getBillById(id)
        return ResponseEntity.ok(bill)
    }

    @DeleteMapping("/delete/{id}")
    fun deleteBill(@PathVariable id: String): ResponseEntity<Int> {
        val result = billService.deleteBill(id)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/total-bills")
    fun getTotalBills(): ResponseEntity<Int> {
        return ResponseEntity.ok(billService.getTotalBills());
    }

    @GetMapping("/monthly-sales")
    fun getMonthlySales(): List<Map<String, Any>> {
        return billService.getMonthlySales()
    }

    @GetMapping("/top-sellling-items/{monthType}")
    fun getTotalItems(@PathVariable monthType: String): ResponseEntity<List<Item>>{
        return ResponseEntity.ok(billService.getTopSellingItems(monthType));
    }

    @GetMapping("/pdf/{id}/export")
    fun exportBill(@PathVariable id: String, response: HttpServletResponse) {
        val billDetails = billService.getBillWithDetails(id)

        if (billDetails == null) {
            response.status = HttpServletResponse.SC_NOT_FOUND
            return
        }

        val pdfContent = billService.generateBillPdf(billDetails)

        response.contentType = "application/pdf"
        response.setHeader("Content-Disposition", "attachment; filename=bill-${id}.pdf")
        response.setContentLength(pdfContent.size)

        val outputStream = response.outputStream
        outputStream.write(pdfContent)
        outputStream.flush()
    }
}
