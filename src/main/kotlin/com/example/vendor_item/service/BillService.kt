package com.example.vendor_item.service

import com.example.vendor_item.model.*
import com.example.vendor_item.repository.BillRepository
import com.example.vendor_item.repository.ItemRepository
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

@Service
class BillService(private val billRepository: BillRepository, private val itemRepository: ItemRepository) {

    fun createBill(bill: Bill): Bill? {
        val count = billRepository.countNoOfRows()?.plus(1)
        val paddedCount = count.toString().padStart(3, '0')
        val billToBeCreated = bill.copy(
            id = "BL-$paddedCount",
            createdAt = Instant.now().toEpochMilli(),
            status="Done"
        )
        bill.items.map { item ->
            itemRepository.updateBillItem(billToBeCreated.id!!, item.itemId!!, item.quantity)
        }

        return billRepository.save(billToBeCreated)
    }

    fun update(bill: Bill): Bill?{
        bill.items.map { item ->
            itemRepository.updateBillItem(bill.id!!, item.itemId!!, item.quantity)
        }

        return billRepository.update(bill)
    }

    fun getAllBills(): List<ListBill> {
       return billRepository.findAll()
    }

    fun getBillById(id: String): Bill? {
        return billRepository.findById(id)
    }

    fun deleteBill(id: String): Int {
        return billRepository.deleteById(id)
    }

    fun getTotalBills(): Int? {
        return billRepository.getTotalBills();
    }

    fun getMonthlySales(): List<Map<String, Any>> {
        return billRepository.getMonthlySales().map {
            mapOf("month" to it.first, "totalSales" to it.second)
        }
    }

    fun getTopSellingItems(monthType: String): List<Item>{
        return billRepository.getTopSellingItems(monthType)
    }


    private val headerBackgroundColor = BaseColor(51, 51, 51) // Dark gray
    private val borderColor = BaseColor(200, 200, 200) // Light gray
    private val alternateRowColor = BaseColor(245, 245, 245) // Very light gray

    fun generateBillPdf(billDetails: BillWithDetails): ByteArray {
        val baos = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f) // Margins
        val writer = PdfWriter.getInstance(document, baos)

        document.open()

        // Add header section
        addHeaderSection(document, billDetails.bill.id ?: "")

        // Add customer section
        addCustomerSection(document, billDetails)

        // Add items table
        addItemsTable(document, billDetails.itemDetails)

        // Add totals section
        addTotalsSection(document, billDetails.itemDetails)

        // Add footer
        addFooter(document)

        document.close()

        return baos.toByteArray()
    }

    fun addHeaderSection(document: Document, billId: String) {
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

        // ------ Right column: Bill info ------
        val billInfoCell = PdfPCell()
        billInfoCell.border = Rectangle.NO_BORDER
        billInfoCell.horizontalAlignment = Element.ALIGN_RIGHT

        // Bill Title
        val billTitle = Paragraph("INVOICE", Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD))
        billTitle.alignment = Element.ALIGN_RIGHT
        billTitle.spacingAfter = 10f
        billInfoCell.addElement(billTitle)

        // Add cells to main header table
        headerTable.addCell(companyInfoCell)
        headerTable.addCell(billInfoCell)

        // Add the header table to document
        document.add(headerTable)

        // Add horizontal line with some spacing
        document.add(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 2f, Font.NORMAL)))
        val lineSeparator = LineSeparator(1f, 100f, BaseColor.BLACK, Element.ALIGN_CENTER, 0f)
        document.add(Chunk(lineSeparator))
        document.add(Paragraph(" ", Font(Font.FontFamily.HELVETICA, 5f, Font.NORMAL)))
    }

    fun addCustomerSection(document: Document, billDetails: BillWithDetails) {
        // Main table with two columns
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(1f, 1f))

        // ------ Left column: Customer details ------
        val customerCell = PdfPCell()
        customerCell.border = Rectangle.NO_BORDER
        customerCell.paddingBottom = 10f

        // "Bill to" header
        val billToHeader = Paragraph("Bill to", Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD))
        billToHeader.spacingAfter = 5f
        customerCell.addElement(billToHeader)

        // Customer details
        customerCell.addElement(Paragraph(billDetails.bill.customerName ?: "Walk-in Customer",
            Font(Font.FontFamily.HELVETICA, 10f)))

        if (billDetails.bill.customerPhoneNo != null) {
            customerCell.addElement(Paragraph("Phone: ${billDetails.bill.customerPhoneNo}",
                Font(Font.FontFamily.HELVETICA, 10f)))
        }

        // ------ Right column: Date and Bill info ------
        val dateBillCell = PdfPCell()
        dateBillCell.border = Rectangle.NO_BORDER
        dateBillCell.horizontalAlignment = Element.ALIGN_RIGHT

        // Create date and bill info table
        val dateTable = PdfPTable(2)
        dateTable.widthPercentage = 100f
        dateTable.horizontalAlignment = Element.ALIGN_RIGHT

        // Get date formatting ready
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val dateStr = if (billDetails.bill.createdAt != null)
            dateFormat.format(Date(billDetails.bill.createdAt))
        else
            dateFormat.format(Date())

        // Date row
        addLabelValueRow(dateTable, "Date", dateStr)

        // Bill # row
        addLabelValueRow(dateTable, "Invoice #", billDetails.bill.id ?: "")


        dateBillCell.addElement(dateTable)

        // Add cells to main table
        table.addCell(customerCell)
        table.addCell(dateBillCell)

        // Add the main table to the document
        document.add(table)

        // Add spacing
        document.add(Paragraph(" "))
    }

    // Helper function to add label-value rows to the date/Bill table
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

    private fun addItemsTable(document: Document, items: List<BillItemWithDetails>) {
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
            table.addCell(createCell(item.tax.multiply(BigDecimal(100)).toString(), Element.ALIGN_RIGHT, baseColor))

            // Calculate subtotal for this item (quantity * rate)
            val itemSubtotal = item.quantity * item.rate
            // Calculate tax amount for this item
            val itemTaxAmount = itemSubtotal * item.tax.toDouble()
            // Calculate total amount for this item (subtotal + tax amount)
            val itemTotal = itemSubtotal + itemTaxAmount

            table.addCell(createCell("₹${String.format("%.2f", itemTotal)}", Element.ALIGN_RIGHT, baseColor))
        }

        document.add(table)
        document.add(Paragraph(" ")) // spacing
    }

    private fun addTotalsSection(document: Document, items: List<BillItemWithDetails>) {
        // Calculate subtotal (sum of quantity * rate for all items)
        val subtotal = items.sumOf { it.quantity * it.rate }

        // Calculate total tax amount
        val totalTaxAmount = items.sumOf {
            val itemSubtotal = it.quantity * it.rate
            itemSubtotal * it.tax.toDouble()
        }

        // Calculate grand total
        val grandTotal = subtotal + totalTaxAmount

        val table = PdfPTable(2)
        table.widthPercentage = 40f // Width of just this table
        table.horizontalAlignment = Element.ALIGN_RIGHT
        table.setWidths(floatArrayOf(1f, 1f))

        // Add subtotal
        table.addCell(createRightAlignedLabelCell("Subtotal"))
        table.addCell(createRightAlignedValueCell("₹${String.format("%.2f", subtotal)}"))

        // Add tax
        table.addCell(createRightAlignedLabelCell("Tax"))
        table.addCell(createRightAlignedValueCell("₹${String.format("%.2f", totalTaxAmount)}"))

        // Add total with bold font
        val totalLabelCell = PdfPCell(Phrase("TOTAL", Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)))
        totalLabelCell.horizontalAlignment = Element.ALIGN_RIGHT
        totalLabelCell.border = Rectangle.TOP
        totalLabelCell.paddingTop = 5f
        totalLabelCell.paddingBottom = 5f
        table.addCell(totalLabelCell)

        val totalValueCell = PdfPCell(Phrase("₹${String.format("%.2f", grandTotal)}", Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)))
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
        paragraph.add(Paragraph("For any questions regarding this invoice, please contact support@kmart.com",
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

    fun getBillWithDetails(id: String): BillWithDetails? {
        val bill = billRepository.findById(id) ?: return null

        val itemDetails = bill.items.mapNotNull { billItem ->
            billItem.itemId?.let { itemId ->
                itemRepository.findById(itemId)?.let { item ->
                    BillItemWithDetails(
                        item = item,
                        quantity = billItem.quantity,
                        rate = billItem.rate,
                        tax = billItem.tax,
                        amount = billItem.quantity * billItem.rate
                    )
                }
            }
        }

        return BillWithDetails(
            bill = bill,
            itemDetails = itemDetails
        )
    }
}
