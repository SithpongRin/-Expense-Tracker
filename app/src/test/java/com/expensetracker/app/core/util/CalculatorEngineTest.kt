package com.expensetracker.app.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CalculatorEngineTest {

    private lateinit var calculator: CalculatorEngine

    @Before
    fun setup() {
        calculator = CalculatorEngine()
    }

    @Test
    fun `evaluates simple addition`() {
        val result = calculator.evaluate("10 + 2.50")
        assertThat(result.compareTo(BigDecimal("12.5"))).isEqualTo(0)
    }

    @Test
    fun `evaluates simple subtraction`() {
        val result = calculator.evaluate("100 - 35.75")
        assertThat(result.compareTo(BigDecimal("64.25"))).isEqualTo(0)
    }

    @Test
    fun `evaluates simple multiplication with unicode symbol`() {
        val result = calculator.evaluate("3.5 × 2")
        assertThat(result.compareTo(BigDecimal("7.0"))).isEqualTo(0)
    }

    @Test
    fun `evaluates simple division with unicode symbol`() {
        val result = calculator.evaluate("20 ÷ 4")
        assertThat(result.compareTo(BigDecimal("5.0"))).isEqualTo(0)
    }

    @Test
    fun `evaluates operator precedence correctly`() {
        // Multiplication before addition: 2 + 3 * 4 = 14
        val result = calculator.evaluate("2 + 3 × 4")
        assertThat(result.compareTo(BigDecimal("14"))).isEqualTo(0)
    }

    @Test
    fun `evaluates parentheses precedence correctly`() {
        // (2 + 3) * 4 = 20
        val result = calculator.evaluate("(2 + 3) × 4")
        assertThat(result.compareTo(BigDecimal("20"))).isEqualTo(0)
    }

    @Test
    fun `evaluates complex expression`() {
        // 10 + 20 ÷ 4 - 2 × 3 = 10 + 5 - 6 = 9
        val result = calculator.evaluate("10 + 20 ÷ 4 - 2 × 3")
        assertThat(result.compareTo(BigDecimal("9"))).isEqualTo(0)
    }

    @Test
    fun `evaluates decimal division with proper rounding`() {
        // 10 / 3
        val result = calculator.evaluate("10 ÷ 3")
        assertThat(result.toDouble()).isWithin(0.001).of(3.3333)
    }

    @Test
    fun `throws arithmetic exception on division by zero`() {
        val exception = assertThrows(ArithmeticException::class.java) {
            calculator.evaluate("15 ÷ 0")
        }
        assertThat(exception).hasMessageThat().contains("Division by zero")
    }

    @Test
    fun `throws illegal argument exception on malformed expressions`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.evaluate("10 +")
        }
        assertThrows(IllegalArgumentException::class.java) {
            calculator.evaluate("10 * * 5")
        }
        assertThrows(IllegalArgumentException::class.java) {
            calculator.evaluate("10 / / 5")
        }
        assertThrows(IllegalArgumentException::class.java) {
            calculator.evaluate("(10 + 5")
        }
        assertThrows(IllegalArgumentException::class.java) {
            calculator.evaluate("10.5.2")
        }
    }

    @Test
    fun `handles empty or blank string gracefully`() {
        val result = calculator.evaluate("")
        assertThat(result.compareTo(BigDecimal.ZERO)).isEqualTo(0)
    }
}
