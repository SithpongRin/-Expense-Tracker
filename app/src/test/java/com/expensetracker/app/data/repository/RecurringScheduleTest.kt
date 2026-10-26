package com.expensetracker.app.data.repository

import com.expensetracker.app.core.model.RecurringFrequency
import com.expensetracker.app.core.util.DateTimeUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class RecurringScheduleTest {

    @Test
    fun `calculates next occurrence for daily frequency`() {
        val date = LocalDate.of(2026, 10, 26)
        val millis = DateTimeUtils.toEpochMillis(date)

        val localDate = DateTimeUtils.toLocalDate(millis)
        val nextDate = localDate.plusDays(1)
        val nextMillis = DateTimeUtils.toEpochMillis(nextDate)

        val resultLocalDate = DateTimeUtils.toLocalDate(nextMillis)
        assertThat(resultLocalDate.dayOfMonth).isEqualTo(27)
        assertThat(resultLocalDate.monthValue).isEqualTo(10)
    }

    @Test
    fun `calculates next occurrence for monthly frequency`() {
        val date = LocalDate.of(2026, 1, 31)
        val millis = DateTimeUtils.toEpochMillis(date)

        val localDate = DateTimeUtils.toLocalDate(millis)
        val nextDate = localDate.plusMonths(1)
        val nextMillis = DateTimeUtils.toEpochMillis(nextDate)

        val resultLocalDate = DateTimeUtils.toLocalDate(nextMillis)
        assertThat(resultLocalDate.dayOfMonth).isEqualTo(28) // Feb in non-leap year
        assertThat(resultLocalDate.monthValue).isEqualTo(2)
    }
}
