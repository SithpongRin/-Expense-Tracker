package com.expensetracker.app.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateTimeUtilsTest {

    @Test
    fun `formats date and month in Phnom Penh timezone correctly`() {
        val date = LocalDate.of(2026, 10, 26)
        val epochMillis = DateTimeUtils.toEpochMillis(date)

        val formattedDate = DateTimeUtils.formatDate(epochMillis)
        assertThat(formattedDate).isEqualTo("Oct 26, 2026")

        val monthKey = DateTimeUtils.toMonthKey(epochMillis)
        assertThat(monthKey).isEqualTo("2026-10")

        val monthYear = DateTimeUtils.formatMonthYear(epochMillis)
        assertThat(monthYear).isEqualTo("October 2026")
    }

    @Test
    fun `calculates start and end of month precisely`() {
        val ym = YearMonth.of(2026, 2) // February 2026 (28 days)
        val startMillis = DateTimeUtils.getStartOfMonth(ym)
        val endMillis = DateTimeUtils.getEndOfMonth(ym)

        val startDate = DateTimeUtils.toLocalDate(startMillis)
        val endDate = DateTimeUtils.toLocalDate(endMillis)

        assertThat(startDate.dayOfMonth).isEqualTo(1)
        assertThat(endDate.dayOfMonth).isEqualTo(28)
        assertThat(endMillis).isGreaterThan(startMillis)
    }
}
