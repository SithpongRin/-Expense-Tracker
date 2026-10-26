package com.expensetracker.app.core.util

import com.expensetracker.app.core.model.Currency
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class CurrencyFormatterTest {

    @Test
    fun `formats USD amounts with two decimal places and dollar symbol`() {
        val amount = BigDecimal("1234.50")
        val formatted = CurrencyFormatter.format(amount, Currency.USD.code)
        assertThat(formatted).isEqualTo("$1,234.50")
    }

    @Test
    fun `formats KHR amounts with zero decimals and riel symbol`() {
        val amount = BigDecimal("50000.75")
        val formatted = CurrencyFormatter.format(amount, Currency.KHR.code)
        assertThat(formatted).isEqualTo("50,001 ៛")
    }

    @Test
    fun `formats negative amounts correctly with minus sign`() {
        val amount = BigDecimal("-45.20")
        val formatted = CurrencyFormatter.format(amount, Currency.USD.code)
        assertThat(formatted).isEqualTo("-$45.20")
    }

    @Test
    fun `formats positive sign when requested`() {
        val amount = BigDecimal("150.00")
        val formatted = CurrencyFormatter.format(amount, Currency.USD.code, showPositiveSign = true)
        assertThat(formatted).isEqualTo("+$150.00")
    }

    @Test
    fun `parses formatted string back into BigDecimal correctly`() {
        val parsed = CurrencyFormatter.parse("$1,234.50")
        assertThat(parsed.compareTo(BigDecimal("1234.50"))).isEqualTo(0)
    }

    @Test
    fun `returns zero on malformed string parse`() {
        val parsed = CurrencyFormatter.parse("invalid_currency")
        assertThat(parsed.compareTo(BigDecimal.ZERO)).isEqualTo(0)
    }
}
