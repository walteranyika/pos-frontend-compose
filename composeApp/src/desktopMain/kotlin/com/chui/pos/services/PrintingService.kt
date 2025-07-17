package com.chui.pos.services

import com.chui.pos.dtos.SaleSummaryResponse
import com.chui.pos.managers.SettingsManager
import org.slf4j.LoggerFactory
import javax.print.*


class PrintingService(private val settingsManager: SettingsManager) {
    companion object{
        private const val PRINTER_NAME = "POS-80" // A common name for 80mm thermal printers
        private val logger = LoggerFactory.getLogger(PrintingService::class.java)
    }

    fun getAvailablePrinters(): List<String> {
        val printServices = PrintServiceLookup.lookupPrintServices(null, null)
        return printServices.map { it.name }
    }



    private fun findPrinter(printerName: String): PrintService? {
        if (printerName.isBlank()) {
            logger.warn ( "No printer has been configured in settings." )
            return null
        }
        val printServices = PrintServiceLookup.lookupPrintServices(null, null)
        val foundPrinter = printServices.find { it.name.equals(printerName, ignoreCase = true) }
        if (foundPrinter == null) {
            logger.error ("Configured printer '$printerName' not found." )
            logger.info ( "Available printers: ${printServices.joinToString { it.name }}" )
        }
        return foundPrinter
    }


    fun printReceipt(sale: SaleSummaryResponse) {
        val printerName = settingsManager.settings.value.printerName
        val printer = findPrinter(printerName)
        if (printer == null) {
            return
        }

        val receiptContent = formatReceiptForThermalPrinter(sale)

        try {
            val doc = SimpleDoc(receiptContent.toByteArray(), DocFlavor.BYTE_ARRAY.AUTOSENSE, null)
            val job = printer.createPrintJob()
            job.print(doc, null)
            logger.info ("Receipt for sale ${sale.ref} sent to printer '$PRINTER_NAME'." )
        } catch (e: PrintException) {
            logger.error("Error printing receipt for sale ${sale.ref}" )
        }
    }

    private fun formatReceiptForThermalPrinter(sale: SaleSummaryResponse): String {
        val receiptWidth = 42 // Standard character width for 80mm printers
        val sb = StringBuilder()

        fun center(text: String) = text.padStart((receiptWidth + text.length) / 2).padEnd(receiptWidth)
        fun addTotal(label: String, value: Double) {
            sb.append(label.padEnd(receiptWidth - 12))
            sb.append("%.2f".format(value).padStart(12))
            sb.appendLine()
        }

        // --- Header ---
        sb.appendLine(center("CHUI POS SYSTEM"))
        sb.appendLine(center("Your Store Address, City"))
        sb.appendLine(center("Phone: 123-456-7890"))
        sb.appendLine("-".repeat(receiptWidth))
        sb.appendLine("Ref: ${sale.ref}")
        sb.appendLine("Date: ${formatDateTime(sale.saleDate)}")
        sb.appendLine("Cashier: ${sale.cashier}")
        sb.appendLine("-".repeat(receiptWidth))

        // --- Items Header ---
        sb.append("Item".padEnd(20))
        sb.append("Qty".padStart(5))
        sb.append("Price".padStart(8))
        sb.append("Total".padStart(9))
        sb.appendLine()
        sb.appendLine("-".repeat(receiptWidth))

        // --- Items List ---
        sale.items.forEach { item ->
            sb.append(item.productName.take(20).padEnd(20))
            sb.append(item.quantity.toInt().toString().padStart(5))
            sb.append("%.2f".format(item.price).padStart(8))
            sb.append("%.2f".format(item.total).padStart(9))
            sb.appendLine()
        }
        sb.appendLine("-".repeat(receiptWidth))

        // --- Totals and Payments ---
        addTotal("Grand Total:", sale.grandTotal.toDouble())
        sale.payments.forEach { payment -> addTotal("Paid (${payment.method}):", payment.amount.toDouble()) }
        val change = sale.paidAmount.toDouble() - sale.grandTotal.toDouble()
        if (change > 0.005) {
            addTotal("Change:", change)
        }
        sb.appendLine("-".repeat(receiptWidth))
        sb.appendLine()
        sb.appendLine(center("Thank you!"))
        sb.appendLine()

        // --- ESC/POS Commands ---
        val partialCut = byteArrayOf(0x1d, 0x56, 0x42, 0x00)
        return sb.toString() + String(partialCut, Charsets.ISO_8859_1) // Use a charset that won't corrupt the command bytes
    }

    private fun formatDateTime(dateTimeString: String): String = try {
        java.time.OffsetDateTime.parse(dateTimeString)
            .format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"))
    } catch (e: Exception) { dateTimeString }
}