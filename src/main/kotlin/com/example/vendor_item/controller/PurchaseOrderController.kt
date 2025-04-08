package com.example.vendor_item.controller

import com.example.vendor_item.model.ListPurchaseOrder
import com.example.vendor_item.model.PurchaseOrder
import com.example.vendor_item.service.PurchaseOrderService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/purchase-orders")
class PurchaseOrderController(private val purchaseOrderService: PurchaseOrderService) {

    @PostMapping("/create")
    fun createPurchaseOrder(@RequestBody purchaseOrder: PurchaseOrder): Int {
        return purchaseOrderService.save(purchaseOrder)
    }

    @PostMapping("/update")
    fun updatePurchaseOrder(@RequestBody purchaseOrder: PurchaseOrder): PurchaseOrder? {
        return purchaseOrderService.update(purchaseOrder)
    }

    @PostMapping("/list")
    fun getAllPurchaseOrders(@RequestParam search: String): List<ListPurchaseOrder> {
        return purchaseOrderService.findAll(search)
    }

    @GetMapping("/total/{monthType}")
    fun findItemsOrdered(@PathVariable monthType: String): ResponseEntity<Int?>{
        return ResponseEntity.ok(purchaseOrderService.findItemsOrdered(monthType))
    }

    @GetMapping("/get/{id}")
    fun getPurchaseOrderById(@PathVariable id: String): PurchaseOrder? {
        return purchaseOrderService.findById(id)
    }

    @GetMapping("/pdf/{id}/export")
    fun exportPurchaseOrder(@PathVariable id: String, response: HttpServletResponse) {
        val purchaseOrderDetails = purchaseOrderService.getPurchaseOrderWithDetails(id)

        if (purchaseOrderDetails == null) {
            response.status = HttpServletResponse.SC_NOT_FOUND
            return
        }

        val pdfContent = purchaseOrderService.generatePurchaseOrderPdf(purchaseOrderDetails)

        response.contentType = "application/pdf"
        response.setHeader("Content-Disposition", "attachment; filename=purchase-order-${id}.pdf")
        response.setContentLength(pdfContent.size)

        val outputStream = response.outputStream
        outputStream.write(pdfContent)
        outputStream.flush()
    }
}
