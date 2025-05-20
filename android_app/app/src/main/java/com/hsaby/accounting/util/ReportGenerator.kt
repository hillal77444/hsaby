package com.hsaby.accounting.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportGenerator(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.getDefault())
    
    fun generatePdfReport(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        startDate: Date? = null,
        endDate: Date? = null
    ): File {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.pdf")
        
        PdfWriter(file).use { writer ->
            val pdf = PdfDocument(writer)
            Document(pdf).use { document ->
                // إضافة العنوان
                document.add(
                    Paragraph("تقرير الحسابات والمعاملات")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(20f)
                )
                
                // إضافة التاريخ
                document.add(
                    Paragraph("تاريخ التقرير: ${dateFormat.format(Date())}")
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(12f)
                )
                
                // إضافة إجمالي الأرصدة
                document.add(Paragraph("\nإجمالي الأرصدة").setFontSize(16f))
                val balanceTable = Table(UnitValue.createPercentArray(2)).useAllAvailableWidth()
                balanceTable.addCell(Cell().add(Paragraph("العملة")))
                balanceTable.addCell(Cell().add(Paragraph("المبلغ")))
                
                val yemeniTotal = accounts.filter { it.currency == Constants.CURRENCY_YEMENI }
                    .sumOf { it.balance }
                val saudiTotal = accounts.filter { it.currency == Constants.CURRENCY_SAUDI }
                    .sumOf { it.balance }
                val dollarTotal = accounts.filter { it.currency == Constants.CURRENCY_DOLLAR }
                    .sumOf { it.balance }
                
                balanceTable.addCell(Cell().add(Paragraph("ريال يمني")))
                balanceTable.addCell(Cell().add(Paragraph("$yemeniTotal")))
                balanceTable.addCell(Cell().add(Paragraph("ريال سعودي")))
                balanceTable.addCell(Cell().add(Paragraph("$saudiTotal")))
                balanceTable.addCell(Cell().add(Paragraph("دولار")))
                balanceTable.addCell(Cell().add(Paragraph("$dollarTotal")))
                
                document.add(balanceTable)
                
                // إضافة إجمالي المعاملات
                document.add(Paragraph("\nإجمالي المعاملات").setFontSize(16f))
                val transactionTable = Table(UnitValue.createPercentArray(2)).useAllAvailableWidth()
                transactionTable.addCell(Cell().add(Paragraph("النوع")))
                transactionTable.addCell(Cell().add(Paragraph("المبلغ")))
                
                val toTotal = transactions.filter { it.type == Constants.TRANSACTION_TYPE_TO }
                    .sumOf { it.amount }
                val fromTotal = transactions.filter { it.type == Constants.TRANSACTION_TYPE_FROM }
                    .sumOf { it.amount }
                
                transactionTable.addCell(Cell().add(Paragraph("له")))
                transactionTable.addCell(Cell().add(Paragraph("$toTotal")))
                transactionTable.addCell(Cell().add(Paragraph("عليه")))
                transactionTable.addCell(Cell().add(Paragraph("$fromTotal")))
                
                document.add(transactionTable)
                
                // إضافة تفاصيل المعاملات
                if (transactions.isNotEmpty()) {
                    document.add(Paragraph("\nتفاصيل المعاملات").setFontSize(16f))
                    val detailsTable = Table(UnitValue.createPercentArray(5)).useAllAvailableWidth()
                    detailsTable.addCell(Cell().add(Paragraph("التاريخ")))
                    detailsTable.addCell(Cell().add(Paragraph("النوع")))
                    detailsTable.addCell(Cell().add(Paragraph("المبلغ")))
                    detailsTable.addCell(Cell().add(Paragraph("العملة")))
                    detailsTable.addCell(Cell().add(Paragraph("الوصف")))
                    
                    transactions.forEach { transaction ->
                        detailsTable.addCell(Cell().add(Paragraph(dateFormat.format(Date(transaction.date)))))
                        detailsTable.addCell(Cell().add(Paragraph(transaction.type)))
                        detailsTable.addCell(Cell().add(Paragraph(transaction.amount.toString())))
                        detailsTable.addCell(Cell().add(Paragraph(transaction.currency)))
                        detailsTable.addCell(Cell().add(Paragraph(transaction.description)))
                    }
                    
                    document.add(detailsTable)
                }
            }
        }
        
        return file
    }
    
    fun generateExcelReport(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        startDate: Date? = null,
        endDate: Date? = null
    ): File {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.xlsx")
        
        XSSFWorkbook().use { workbook ->
            // ورقة الأرصدة
            val balanceSheet = workbook.createSheet("الأرصدة")
            var rowNum = 0
            
            // إضافة العنوان
            val titleRow = balanceSheet.createRow(rowNum++)
            titleRow.createCell(0).setCellValue("تقرير الحسابات والمعاملات")
            
            // إضافة التاريخ
            val dateRow = balanceSheet.createRow(rowNum++)
            dateRow.createCell(0).setCellValue("تاريخ التقرير: ${dateFormat.format(Date())}")
            
            // إضافة إجمالي الأرصدة
            rowNum++
            val headerRow = balanceSheet.createRow(rowNum++)
            headerRow.createCell(0).setCellValue("العملة")
            headerRow.createCell(1).setCellValue("المبلغ")
            
            val yemeniTotal = accounts.filter { it.currency == Constants.CURRENCY_YEMENI }
                .sumOf { it.balance }
            val saudiTotal = accounts.filter { it.currency == Constants.CURRENCY_SAUDI }
                .sumOf { it.balance }
            val dollarTotal = accounts.filter { it.currency == Constants.CURRENCY_DOLLAR }
                .sumOf { it.balance }
            
            val yemeniRow = balanceSheet.createRow(rowNum++)
            yemeniRow.createCell(0).setCellValue("ريال يمني")
            yemeniRow.createCell(1).setCellValue(yemeniTotal)
            
            val saudiRow = balanceSheet.createRow(rowNum++)
            saudiRow.createCell(0).setCellValue("ريال سعودي")
            saudiRow.createCell(1).setCellValue(saudiTotal)
            
            val dollarRow = balanceSheet.createRow(rowNum++)
            dollarRow.createCell(0).setCellValue("دولار")
            dollarRow.createCell(1).setCellValue(dollarTotal)
            
            // ورقة المعاملات
            val transactionSheet = workbook.createSheet("المعاملات")
            rowNum = 0
            
            // إضافة إجمالي المعاملات
            val transactionHeaderRow = transactionSheet.createRow(rowNum++)
            transactionHeaderRow.createCell(0).setCellValue("النوع")
            transactionHeaderRow.createCell(1).setCellValue("المبلغ")
            
            val toTotal = transactions.filter { it.type == Constants.TRANSACTION_TYPE_TO }
                .sumOf { it.amount }
            val fromTotal = transactions.filter { it.type == Constants.TRANSACTION_TYPE_FROM }
                .sumOf { it.amount }
            
            val toRow = transactionSheet.createRow(rowNum++)
            toRow.createCell(0).setCellValue("له")
            toRow.createCell(1).setCellValue(toTotal)
            
            val fromRow = transactionSheet.createRow(rowNum++)
            fromRow.createCell(0).setCellValue("عليه")
            fromRow.createCell(1).setCellValue(fromTotal)
            
            // إضافة تفاصيل المعاملات
            if (transactions.isNotEmpty()) {
                rowNum++
                val detailsHeaderRow = transactionSheet.createRow(rowNum++)
                detailsHeaderRow.createCell(0).setCellValue("التاريخ")
                detailsHeaderRow.createCell(1).setCellValue("النوع")
                detailsHeaderRow.createCell(2).setCellValue("المبلغ")
                detailsHeaderRow.createCell(3).setCellValue("العملة")
                detailsHeaderRow.createCell(4).setCellValue("الوصف")
                
                transactions.forEach { transaction ->
                    val detailsRow = transactionSheet.createRow(rowNum++)
                    detailsRow.createCell(0).setCellValue(dateFormat.format(Date(transaction.date)))
                    detailsRow.createCell(1).setCellValue(transaction.type)
                    detailsRow.createCell(2).setCellValue(transaction.amount)
                    detailsRow.createCell(3).setCellValue(transaction.currency)
                    detailsRow.createCell(4).setCellValue(transaction.description)
                }
            }
            
            // حفظ الملف
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
        }
        
        return file
    }
    
    fun shareReport(file: File) {
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".pdf")) "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة التقرير"))
    }
} 