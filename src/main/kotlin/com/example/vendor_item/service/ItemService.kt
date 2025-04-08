package com.example.vendor_item.service

import com.example.vendor_item.model.Item
import com.example.vendor_item.model.ListItem
import com.example.vendor_item.repository.BillRepository
import com.example.vendor_item.repository.ItemRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ItemService(private val itemRepository: ItemRepository, private val billRepository: BillRepository) {

    fun getAllItems(listItem: ListItem): List<Item> {
        return itemRepository.findAll(listItem.search, listItem.page, listItem.size, listItem.getAll, listItem.vendorId, listItem.status)
    }

    fun getItemById(id: String): Item? {
        return itemRepository.findById(id)
    }

    fun createItem(item: Item): Int {
        val itemToBeCreated = item.copy(
            id = UUID.randomUUID().toString(),
            createdAt = Instant.now().toEpochMilli()
        )
        return itemRepository.save(itemToBeCreated)
    }

    fun updateItem(item: Item): Int {
        return itemRepository.update(item)
    }

    fun deleteItem(id: String): Int {
        return itemRepository.deleteById(id)
    }

    fun stockInHand(): Int{
        return itemRepository.stockInHand()
    }

    fun totalItemSold(): Int{
        return billRepository.totalItemSold();
    }
}
