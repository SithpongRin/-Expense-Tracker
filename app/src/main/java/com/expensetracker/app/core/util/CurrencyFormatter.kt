package com.expensetracker.app.core.util

import com.expensetracker.app.core.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    /**
     * Formats a [BigDecimal] amount with the given [currency].
     * USD typically uses 2 decimal places: "$1,234.50"
     * KHR typically uses 0 decimal places: "5,000 ៛"
     */
    fun format(
        amount: BigDecimal,
        currencyCode: String = Currency.USD.code,
        includeSymbol: Boolean = true,
        showPositiveSign: Boolean = false
    ): String {
        val currency = Currency.values().find { it.code.equals(currencyCode, ignoreCase = true) }
            ?: Currency.USD

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val pattern = when (currency) {
            Currency.KHR -> "#,##0"
            else -> "#,##0.00"
        }

        val decimalFormat = DecimalFormat(pattern, symbols)
        val roundedAmount = when (currency) {
            Currency.KHR -> amount.setScale(0, RoundingMode.HALF_UP)
            else -> amount.setScale(2, RoundingMode.HALF_EVEN)
        }

        val formattedNumber = decimalFormat.format(roundedAmount.abs())
        val sign = when {
            amount.signum() < 0 -> "-"
            showPositiveSign && amount.signum() > 0 -> "+"
            else -> ""
        }

        return if (!includeSymbol) {
            "$sign$formattedNumber"
        } else {
            when (currency) {
                Currency.KHR -> "$sign$formattedNumber ${currency.symbol}"
                else -> "$sign${currency.symbol}$formattedNumber"
            }
        }
    }

    /**
     * Parses a string representation into a safe [BigDecimal]. Returns [BigDecimal.ZERO] on error.
     */
    fun parse(amountString: String): BigDecimal {
        return try {
            val sanitized = amountString
                .replace("$", "")
                .replace("៛", "")
                .replace("€", "")
                .replace("£", "")
                .replace("฿", "")
                .replace(",", "")
                .trim()
            if (sanitized.isBlank()) BigDecimal.ZERO else BigDecimal(sanitized)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
}
