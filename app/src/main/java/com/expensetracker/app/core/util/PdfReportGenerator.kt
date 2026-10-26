package com.expensetracker.app.core.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.data.local.entity.TransactionWithDetails
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal

object PdfReportGenerator {

    fun generatePdfReport(
        context: Context,
        title: String,
        periodName: String,
        totalIncome: BigDecimal,
        totalExpense: BigDecimal,
        transactions: List<TransactionWithDetails>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val grayPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 50f

        // Header
        canvas.drawText("EXPENSE TRACKER FINANCIAL REPORT", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Period: $periodName | Generated: ${DateTimeUtils.formatDate(DateTimeUtils.nowMillis())}", 40f, y, grayPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Financial Summary Box
        canvas.drawText("SUMMARY", 40f, y, boldPaint)
        y += 18f
        canvas.drawText("Total Income:  ${CurrencyFormatter.format(totalIncome)}", 40f, y, bodyPaint)
        y += 16f
        canvas.drawText("Total Expense: ${CurrencyFormatter.format(totalExpense)}", 40f, y, bodyPaint)
        y += 16f
        val net = totalIncome.subtract(totalExpense)
        canvas.drawText("Net Balance:   ${CurrencyFormatter.format(net, showPositiveSign = true)}", 40f, y, boldPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Table Header
        canvas.drawText("RECENT TRANSACTIONS", 40f, y, boldPaint)
        y += 20f

        canvas.drawText("Date", 40f, y, boldPaint)
        canvas.drawText("Category", 120f, y, boldPaint)
        canvas.drawText("Merchant / Note", 230f, y, boldPaint)
        canvas.drawText("Account", 380f, y, boldPaint)
        canvas.drawText("Amount", 480f, y, boldPaint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        // Transactions rows (limit to first 30 on this page)
        for (item in transactions.take(30)) {
            val t = item.transaction
            val date = DateTimeUtils.formatDate(t.date)
            val cat = (item.category?.name ?: "Transfer").take(15)
            val note = (if (t.merchant.isNotBlank()) t.merchant else t.notes).take(22)
            val wallet = item.wallet.name.take(12)
            val sign = if (t.type == TransactionType.EXPENSE) "-" else if (t.type == TransactionType.INCOME) "+" else ""
            val amountStr = "$sign${CurrencyFormatter.format(t.amount, t.currency)}"

            canvas.drawText(date, 40f, y, grayPaint)
            canvas.drawText(cat, 120f, y, bodyPaint)
            canvas.drawText(note, 230f, y, bodyPaint)
            canvas.drawText(wallet, 380f, y, grayPaint)
            canvas.drawText(amountStr, 480f, y, boldPaint)

            y += 18f
            if (y > 790f) break
        }

        // Footer
        canvas.drawLine(40f, 800f, 555f, 800f, linePaint)
        canvas.drawText("Expense Tracker — End of statement", 40f, 815f, grayPaint)

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "expense_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
