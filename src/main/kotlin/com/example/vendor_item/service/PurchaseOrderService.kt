package com.example.vendor_item.service

import com.example.vendor_item.model.*
import com.example.vendor_item.repository.ItemRepository
import com.example.vendor_item.repository.PurchaseOrderRepository
import com.example.vendor_item.repository.VendorRepository
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

@Service
class PurchaseOrderService(private val repository: PurchaseOrderRepository, private val vendorRepository: VendorRepository, private val itemRepository: ItemRepository) {

    fun save(purchaseOrder: PurchaseOrder): Int {
        val count = repository.countNoOfRows()?.plus(1)
        val paddedCount = count.toString().padStart(3, '0')
        val purchaseOrderCreated = purchaseOrder.copy(
            id = "PO-$paddedCount",
            createdAt = Instant.now().toEpochMilli()
        )

        purchaseOrderCreated.items.map { item ->
            itemRepository.updatePurchaseOrderItem(purchaseOrderCreated.id!!, item.itemId!!, item.quantity)
        }

        return repository.save(purchaseOrderCreated)
    }

    fun update(purchaseOrder: PurchaseOrder): PurchaseOrder? {
        purchaseOrder.items.map { item ->
            itemRepository.updatePurchaseOrderItem(purchaseOrder.id!!, item.itemId!!, item.quantity)
        }

        return repository.update(purchaseOrder)
    }

    fun findAll(search: String): List<ListPurchaseOrder> = repository.findAll(search)

    fun findById(id: String): PurchaseOrder? = repository.findById(id)

    fun findItemsOrdered(monthType: String): Int? {
        return repository.findTotalItemOrdered(monthType)
    }

    fun deleteById(id: String): Int = repository.deleteById(id)

    // Define colors
    private val headerBackgroundColor = BaseColor(51, 51, 51) // Dark gray
    private val borderColor = BaseColor(200, 200, 200) // Light gray
    private val alternateRowColor = BaseColor(245, 245, 245) // Very light gray

    fun generatePurchaseOrderPdf(poDetails: PurchaseOrderWithDetails): ByteArray {
            val baos = ByteArrayOutputStream()
            val document = Document(PageSize.A4, 36f, 36f, 36f, 36f) // Margins
            val writer = PdfWriter.getInstance(document, baos)

            document.open()

            // Add header section
            addHeaderSection(document, poDetails.purchaseOrder.id ?: "")

            // Add address section
            addAddressSection(document, poDetails.vendor, poDetails)

            // Add order details section
//            addOrderDetailsSection(document, poDetails.purchaseOrder)

            // Add items table
            addItemsTable(document, poDetails.itemDetails)

            // Add totals section
            addTotalsSection(document, poDetails.itemDetails)

            // Add footer
            addFooter(document)

            document.close()

            return baos.toByteArray()
        }

    fun addHeaderSection(document: Document, poId: String) {
        // Main header table with two columns
        val headerTable = PdfPTable(2)
        headerTable.widthPercentage = 100f
        headerTable.setWidths(floatArrayOf(1f, 1f))

        // ------ Left column: Company info ------
        val companyInfoCell = PdfPCell()
        companyInfoCell.border = Rectangle.NO_BORDER
        companyInfoCell.paddingBottom = 15f

        // Company name/logo
        val logoText = Paragraph("K MART", Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD))
        logoText.spacingAfter = 8f
        companyInfoCell.addElement(logoText)

        // Company address and contact info with proper formatting
        val addressBlock = PdfPTable(1)
        addressBlock.widthPercentage = 100f

        val addressLines = arrayOf(
            "21 Market Street",
            "Springfield, IL 62704",
            "Tel: (555) 123-4567",
            "Email: contact@kmart.com",
            "www.kmart.com"
        )

        addressLines.forEach { line ->
            val addressCell = PdfPCell(Paragraph(line, Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL)))
            addressCell.border = Rectangle.NO_BORDER
            addressCell.paddingBottom = 2f
            addressBlock.addCell(addressCell)
        }

        companyInfoCell.addElement(addressBlock)

        // ------ Right column: PO info ------
        val poInfoCell = PdfPCell()
        poInfoCell.border = Rectangle.NO_BORDER
        poInfoCell.horizontalAlignment = Element.ALIGN_RIGHT

        // PO Title
        val poTitle = Paragraph("PURCHASE ORDER", Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD))
        poTitle.alignment = Element.ALIGN_RIGHT
        poTitle.spacingAfter = 10f
        poInfoCell.addElement(poTitle)

        // PO Details table
        val poDetailsTable = PdfPTable(2)
        poDetailsTable.widthPercentage = 100f

        // Add cells to main header table
        headerTable.addCell(companyInfoCell)
        headerTable.addCell(poInfoCell)

        // Add the header table to document
        document.add(headerTable)

        // Add horizontal line with some spacing
        document.add(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 2f, Font.NORMAL)))
        val lineSeparator = LineSeparator(1f, 100f, BaseColor.BLACK, Element.ALIGN_CENTER, 0f)
        document.add(Chunk(lineSeparator))
        document.add(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 5f, Font.NORMAL)))
    }

    fun addAddressSection(document: Document, vendor: Vendor, poDetails: PurchaseOrderWithDetails) {
        // Main table with two columns
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(1f, 1f))

        // ------ Left column: Vendor details ------
        val vendorCell = PdfPCell()
        vendorCell.border = Rectangle.NO_BORDER
        vendorCell.paddingBottom = 10f

        // "Bill to" header
        val billToHeader = Paragraph("Bill to", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD))
        billToHeader.spacingAfter = 5f
        vendorCell.addElement(billToHeader)

        // Vendor details
        vendorCell.addElement(Paragraph(vendor.name, Font(Font.FontFamily.HELVETICA, 10f)))

        if (vendor.address != null) {
            vendorCell.addElement(Paragraph("Address: ${vendor.address}", Font(Font.FontFamily.HELVETICA, 10f)))
        }

        if (vendor.phone != null) {
            vendorCell.addElement(Paragraph("Phone: ${vendor.phone}", Font(Font.FontFamily.HELVETICA, 10f)))
        }

        // ------ Right column: Date and PO info ------
        val datePoCell = PdfPCell()
        datePoCell.border = Rectangle.NO_BORDER
        datePoCell.horizontalAlignment = Element.ALIGN_RIGHT

        // Create date and PO info table
        val dateTable = PdfPTable(2)
        dateTable.widthPercentage = 100f
        dateTable.horizontalAlignment = Element.ALIGN_RIGHT

        // Get date formatting ready
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val dateStr = if (poDetails.purchaseOrder.createdAt != null)
            dateFormat.format(Date(poDetails.purchaseOrder.createdAt))
        else
            dateFormat.format(Date())

        // Date row
        addLabelValueRow(dateTable, "Date", dateStr)

        // PO # row
        addLabelValueRow(dateTable, "PO #", poDetails.purchaseOrder.id ?: "")

        // Expected date row
        val expectedDateMillis: Long = poDetails.purchaseOrder.expectedDate ?: 0L
        val date = Date(expectedDateMillis * 1000)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val expectedDateString: String = formatter.format(date)

        addLabelValueRow(dateTable, "Expected Date #", expectedDateString)

        datePoCell.addElement(dateTable)

        // Add cells to main table
        table.addCell(vendorCell)
        table.addCell(datePoCell)

        // Add the main table to the document
        document.add(table)

        // Add spacing
        document.add(Paragraph(" "))
    }

    // Helper function to add label-value rows to the date/PO table
    private fun addLabelValueRow(table: PdfPTable, label: String, value: String) {
        val labelCell = PdfPCell(Phrase(label, Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)))
        labelCell.horizontalAlignment = Element.ALIGN_LEFT
        labelCell.border = Rectangle.NO_BORDER
        table.addCell(labelCell)

        val valueCell = PdfPCell(Phrase(value, Font(Font.FontFamily.HELVETICA, 10f)))
        valueCell.horizontalAlignment = Element.ALIGN_RIGHT
        valueCell.border = Rectangle.NO_BORDER
        table.addCell(valueCell)
    }

    private fun addItemsTable(document: Document, items: List<ItemWithDetails>) {
            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(3f, 1f, 1.5f, 1f, 1.5f))

            // Add headers
            addHeaderCell(table, "ITEM")
            addHeaderCell(table, "QTY")
            addHeaderCell(table, "UNIT PRICE")
            addHeaderCell(table, "TAX(%)")
            addHeaderCell(table, "TOTAL")

            // Add items
            var alternate = false
            for (item in items) {
                val baseColor = if (alternate) alternateRowColor else BaseColor.WHITE
                alternate = !alternate

                table.addCell(createCell(item.item.name, Element.ALIGN_LEFT, baseColor))
                table.addCell(createCell(item.quantity.toString(), Element.ALIGN_CENTER, baseColor))
                table.addCell(createCell("₹${item.rate}", Element.ALIGN_RIGHT, baseColor))
                table.addCell(createCell(item.tax?.multiply(BigDecimal(100)).toString(), Element.ALIGN_RIGHT, baseColor))
                table.addCell(createCell("₹${item.amount}", Element.ALIGN_RIGHT, baseColor))
            }

            document.add(table)
            document.add(Paragraph(" ")) // spacing
        }

    private fun addTotalsSection(document: Document, items: List<ItemWithDetails>) {
            val totalAmount = items.sumOf { it.amount }
            val totalTaxAmount = items.sumOf {
                val itemTotal = it.rate * (it.quantity).toDouble()
                itemTotal * (it.tax)?.toDouble()!!
            }
            val table = PdfPTable(2)
            table.widthPercentage = 40f // Width of just this table
            table.horizontalAlignment = Element.ALIGN_RIGHT
            table.setWidths(floatArrayOf(1f, 1f))

            // Add subtotal
            table.addCell(createRightAlignedLabelCell("Subtotal"))
            table.addCell(createRightAlignedValueCell("₹$totalAmount"))

            // Add tax (assumed 0 in this case)
            table.addCell(createRightAlignedLabelCell("Tax"))
            table.addCell(createRightAlignedValueCell(totalTaxAmount.toString()))

            // Add total with bold font
            val totalLabelCell = PdfPCell(Phrase("TOTAL", Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)))
            totalLabelCell.horizontalAlignment = Element.ALIGN_RIGHT
            totalLabelCell.border = Rectangle.TOP
            totalLabelCell.paddingTop = 5f
            totalLabelCell.paddingBottom = 5f
            table.addCell(totalLabelCell)
            val total = totalAmount+totalTaxAmount;
            val totalValueCell = PdfPCell(Phrase("₹$total", Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)))
            totalValueCell.horizontalAlignment = Element.ALIGN_RIGHT
            totalValueCell.border = Rectangle.TOP
            totalValueCell.paddingTop = 5f
            totalValueCell.paddingBottom = 5f
            table.addCell(totalValueCell)

            document.add(table)
            document.add(Paragraph(" ")) // spacing
        }

    private fun addFooter(document: Document) {
            val paragraph = Paragraph()
            paragraph.add(Paragraph("Thank you for your business!", Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC)))
            paragraph.add(Paragraph("For any questions regarding this purchase order, please contact support@yourcompany.com",
                Font(Font.FontFamily.HELVETICA, 8f)))
            paragraph.alignment = Element.ALIGN_CENTER
            document.add(paragraph)
        }

    private fun addHeaderCell(table: PdfPTable, text: String) {
            val cell = PdfPCell(Phrase(text, Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.WHITE)))
            cell.backgroundColor = headerBackgroundColor
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.verticalAlignment = Element.ALIGN_MIDDLE
            cell.paddingTop = 5f
            cell.paddingBottom = 5f
            table.addCell(cell)
        }

    private fun createCell(text: String, alignment: Int = Element.ALIGN_LEFT, backgroundColor: BaseColor = BaseColor.WHITE): PdfPCell {
            val cell = PdfPCell(Phrase(text, Font(Font.FontFamily.HELVETICA, 9f)))
            cell.horizontalAlignment = alignment
            cell.verticalAlignment = Element.ALIGN_MIDDLE
            cell.paddingTop = 5f
            cell.paddingBottom = 5f
            cell.backgroundColor = backgroundColor
            return cell
        }

    private fun createRightAlignedLabelCell(text: String): PdfPCell {
            val cell = PdfPCell(Phrase(text, Font(Font.FontFamily.HELVETICA, 10f)))
            cell.horizontalAlignment = Element.ALIGN_RIGHT
            cell.border = Rectangle.NO_BORDER
            cell.paddingTop = 2f
            cell.paddingBottom = 2f
            return cell
        }

    private fun createRightAlignedValueCell(text: String): PdfPCell {
            val cell = PdfPCell(Phrase(text, Font(Font.FontFamily.HELVETICA, 10f)))
            cell.horizontalAlignment = Element.ALIGN_RIGHT
            cell.border = Rectangle.NO_BORDER
            cell.paddingTop = 2f
            cell.paddingBottom = 2f
            return cell
        }

    fun getPurchaseOrderWithDetails(id: String): PurchaseOrderWithDetails? {
        val purchaseOrder = repository.findById(id) ?: return null
        val vendor = purchaseOrder.vendorId?.let { vendorRepository.findById(it) } ?: return null

        val itemDetails = purchaseOrder.items.mapNotNull { poItem ->
            poItem.itemId?.let { itemId ->
                itemRepository.findById(itemId)?.let { item ->
                    ItemWithDetails(
                        item = item,
                        quantity = poItem.quantity,
                        rate = poItem.rate,
                        tax = poItem.tax,
                        amount = poItem.quantity * poItem.rate
                    )
                }
            }
        }

        return PurchaseOrderWithDetails(
            purchaseOrder = purchaseOrder,
            vendor = vendor,
            itemDetails = itemDetails
        )
    }

}



//    fun generatePurchaseOrderPdf(poDetails: PurchaseOrderWithDetails): ByteArray {
//        val baos = ByteArrayOutputStream()
//        val document = Document(PageSize.A4)
//        val writer = PdfWriter.getInstance(document, baos)
//
//        document.open()
//
//        // Add title
//        val headerFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
//        val title = Paragraph("Purchase Order: ${poDetails.purchaseOrder.id}", headerFont)
//        title.alignment = Element.ALIGN_CENTER
//        document.add(title)
//        document.add(Paragraph(" ")) // spacing
//
//        // Add vendor and order information
//        val infoTable = PdfPTable(2)
//        infoTable.widthPercentage = 100f
//        infoTable.setWidths(floatArrayOf(1f, 1f))
//
//        // Vendor information
//        val vendorCell = PdfPCell()
//        vendorCell.border = Rectangle.BOX
//        vendorCell.paddingBottom = 10f
//
//        val vendorHeaderFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
//        val vendorHeader = Paragraph("Vendor Information", vendorHeaderFont)
//        vendorCell.addElement(vendorHeader)
//        vendorCell.addElement(Paragraph("Name: ${poDetails.vendor.name}"))
//        poDetails.vendor.phone?.let { vendorCell.addElement(Paragraph("Phone: $it")) }
//        poDetails.vendor.address?.let { vendorCell.addElement(Paragraph("Address: $it")) }
//
//        // Order information
//        val orderCell = PdfPCell()
//        orderCell.border = Rectangle.BOX
//        orderCell.paddingBottom = 10f
//
//        val orderHeader = Paragraph("Order Information", vendorHeaderFont)
//        orderCell.addElement(orderHeader)
//        poDetails.purchaseOrder.createdAt?.let {
//            orderCell.addElement(Paragraph("Date Created: ${formatDate(it)}"))
//        }
//        poDetails.purchaseOrder.expectedDate?.let {
//            orderCell.addElement(Paragraph("Expected Delivery: ${formatDate(it)}"))
//        }
//
//        val totalAmount = poDetails.itemDetails.sumOf { it.amount }
//        orderCell.addElement(Paragraph("Total Amount: ₹$totalAmount"))
//
//        infoTable.addCell(vendorCell)
//        infoTable.addCell(orderCell)
//        document.add(infoTable)
//        document.add(Paragraph(" ")) // spacing
//
//        // Add items table
//        document.add(Paragraph("Items", vendorHeaderFont))
//        val itemsTable = PdfPTable(5)
//        itemsTable.widthPercentage = 100f
//        itemsTable.setWidths(floatArrayOf(3f, 1f, 1.5f, 1f, 1.5f))
//
//        // Add table headers
//        val tableHeaderFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)
//        arrayOf("Item", "Quantity", "Rate", "Tax", "Amount").forEach {
//            val cell = PdfPCell(Phrase(it, tableHeaderFont))
//            cell.horizontalAlignment = Element.ALIGN_CENTER
//            cell.verticalAlignment = Element.ALIGN_MIDDLE
//            cell.paddingBottom = 5f
//            itemsTable.addCell(cell)
//        }
//
//        // Add items
//        for (itemDetail in poDetails.itemDetails) {
//            itemsTable.addCell(createCell(itemDetail.item.name))
//            itemsTable.addCell(createCell(itemDetail.quantity.toString(), Element.ALIGN_CENTER))
//            itemsTable.addCell(createCell("₹${itemDetail.rate}", Element.ALIGN_RIGHT))
//            itemsTable.addCell(createCell("₹0.00", Element.ALIGN_RIGHT)) // Assuming tax is 0 as per your example
//            itemsTable.addCell(createCell("₹${itemDetail.amount}", Element.ALIGN_RIGHT))
//        }
//
//        // Add total row
//        itemsTable.addCell(createCell("", Rectangle.NO_BORDER, 3))
//        val totalCell = createCell("Total", Element.ALIGN_RIGHT)
//        totalCell.colspan = 1
//        itemsTable.addCell(totalCell)
//        itemsTable.addCell(createCell("₹$totalAmount", Element.ALIGN_RIGHT))
//
//        document.add(itemsTable)
//        document.close()
//
//        return baos.toByteArray()
//    }
//
//    private fun createCell(text: String, alignment: Int = Element.ALIGN_LEFT, colspan: Int = 1): PdfPCell {
//        val cell = PdfPCell(Phrase(text))
//        cell.horizontalAlignment = alignment
//        cell.verticalAlignment = Element.ALIGN_MIDDLE
//        cell.paddingBottom = 5f
//        cell.colspan = colspan
//        return cell
//    }
//
//    private fun formatDate(timestamp: Long): String {
//        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
//        return dateFormat.format(Date(timestamp))
//    }
