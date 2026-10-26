package com.expensetracker.app.core.util

import com.expensetracker.app.data.local.entity.TransactionWithDetails
import java.io.StringWriter

object CsvExporter {

    fun generateCsv(transactions: List<TransactionWithDetails>): String {
        val writer = StringWriter()
        // Header
        writer.append("ID,Date,Time,Type,Category,Wallet,ToWallet,Amount,Currency,Merchant,Notes\n")

        for (item in transactions) {
            val t = item.transaction
            val dateStr = DateTimeUtils.formatDate(t.date)
            val timeStr = DateTimeUtils.formatTime(t.date)
            val typeStr = t.type.name
            val catStr = escapeCsv(item.category?.name ?: "")
            val walletStr = escapeCsv(item.wallet.name)
            val toWalletStr = escapeCsv(item.toWallet?.name ?: "")
            val amountStr = t.amount.toPlainString()
            val currencyStr = t.currency
            val merchantStr = escapeCsv(t.merchant)
            val notesStr = escapeCsv(t.notes)

            writer.append("${t.id},\"$dateStr\",\"$timeStr\",$typeStr,\"$catStr\",\"$walletStr\",\"$toWalletStr\",$amountStr,$currencyStr,\"$merchantStr\",\"$notesStr\"\n")
        }

        return writer.toString()
    }

    private fun escapeCsv(text: String): String {
        return text.replace("\"", "\"\"")
    }
}
