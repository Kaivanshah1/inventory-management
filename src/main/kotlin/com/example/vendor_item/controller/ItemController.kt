package com.example.vendor_item.controller

import com.example.vendor_item.model.Item
import com.example.vendor_item.model.ListItem
import com.example.vendor_item.service.ItemService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
    
@CrossOrigin
@RestController
@RequestMapping("/api/v1/items")
class ItemController(private val itemService: ItemService) {

    @PostMapping("/list")
    fun getAllItems(@RequestBody listItem: ListItem): ResponseEntity<List<Item>> {
        return ResponseEntity.ok(itemService.getAllItems(listItem))
    }

    @GetMapping("/get/{id}")
    fun getItemById(@PathVariable id: String): ResponseEntity<Item> {
        val item = itemService.getItemById(id)
        return if (item != null) ResponseEntity.ok(item) else ResponseEntity.notFound().build()
    }

    @PostMapping("/create")
    fun createItem(@RequestBody item: Item): ResponseEntity<String> {
        return if (itemService.createItem(item) > 0)
            ResponseEntity.ok("Item created successfully")
        else
            ResponseEntity.badRequest().body("Failed to create item")
    }

    @PostMapping("/update")
    fun updateItem(@RequestBody item: Item): ResponseEntity<String> {
        return if (itemService.updateItem(item) > 0)
            ResponseEntity.ok("Item updated successfully")  
        else
            ResponseEntity.badRequest().body("Failed to update item")
    }

    @DeleteMapping("/delete/{id}")
    fun deleteItem(@PathVariable id: String): ResponseEntity<String> {
        return if (itemService.deleteItem(id) > 0)
            ResponseEntity.ok("Item deleted successfully")
        else
            ResponseEntity.notFound().build()
    }

    @GetMapping("/get-stock-in-hand")
    fun getStockInHand(): ResponseEntity<Int>{
        return ResponseEntity.ok(itemService.stockInHand())
    }

    @GetMapping("/total-items-sold")
    fun getTotalItems(): ResponseEntity<Int>{
        return ResponseEntity.ok(itemService.totalItemSold())
    }
}