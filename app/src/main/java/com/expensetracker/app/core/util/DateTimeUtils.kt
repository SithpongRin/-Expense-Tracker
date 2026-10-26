package com.expensetracker.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    val PHNOM_PENH_ZONE: ZoneId = ZoneId.of("Asia/Phnom_Penh")

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.US)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
    private val DAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("dd MMM", Locale.US)

    fun nowMillis(): Long {
        return System.currentTimeMillis()
    }

    fun toLocalDateTime(epochMillis: Long): LocalDateTime {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(PHNOM_PENH_ZONE)
            .toLocalDateTime()
    }

    fun toLocalDate(epochMillis: Long): LocalDate {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(PHNOM_PENH_ZONE)
            .toLocalDate()
    }

    fun toEpochMillis(localDateTime: LocalDateTime): Long {
        return localDateTime.atZone(PHNOM_PENH_ZONE).toInstant().toEpochMilli()
    }

    fun toEpochMillis(localDate: LocalDate): Long {
        return localDate.atStartOfDay(PHNOM_PENH_ZONE).toInstant().toEpochMilli()
    }

    fun formatDate(epochMillis: Long): String {
        return toLocalDate(epochMillis).format(DATE_FORMATTER)
    }

    fun formatDateTime(epochMillis: Long): String {
        return toLocalDateTime(epochMillis).format(DATE_TIME_FORMATTER)
    }

    fun formatTime(epochMillis: Long): String {
        return toLocalDateTime(epochMillis).format(TIME_FORMATTER)
    }

    fun formatMonthYear(yearMonth: YearMonth): String {
        return yearMonth.format(MONTH_YEAR_FORMATTER)
    }

    fun formatMonthYear(epochMillis: Long): String {
        val dt = toLocalDate(epochMillis)
        return YearMonth.from(dt).format(MONTH_YEAR_FORMATTER)
    }

    fun toMonthKey(epochMillis: Long): String {
        val dt = toLocalDate(epochMillis)
        return YearMonth.from(dt).format(MONTH_KEY_FORMATTER)
    }

    fun toMonthKey(yearMonth: YearMonth): String {
        return yearMonth.format(MONTH_KEY_FORMATTER)
    }

    fun formatDayMonth(epochMillis: Long): String {
        return toLocalDate(epochMillis).format(DAY_MONTH_FORMATTER)
    }

    fun getStartOfDay(epochMillis: Long): Long {
        val date = toLocalDate(epochMillis)
        return date.atStartOfDay(PHNOM_PENH_ZONE).toInstant().toEpochMilli()
    }

    fun getEndOfDay(epochMillis: Long): Long {
        val date = toLocalDate(epochMillis)
        return date.plusDays(1).atStartOfDay(PHNOM_PENH_ZONE).toInstant().toEpochMilli() - 1
    }

    fun getStartOfMonth(yearMonth: YearMonth): Long {
        return yearMonth.atDay(1).atStartOfDay(PHNOM_PENH_ZONE).toInstant().toEpochMilli()
    }

    fun getEndOfMonth(yearMonth: YearMonth): Long {
        return yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(PHNOM_PENH_ZONE).toInstant().toEpochMilli() - 1
    }

    fun getStartOfYear(year: Int): Long {
        return LocalDate.of(year, 1, 1).atStartOfDay(PHNOM_PENH_ZONE).toInstant().toEpochMilli()
    }

    fun getEndOfYear(year: Int): Long {
        return LocalDate.of(year + 1, 1, 1).atStartOfDay(PHNOM_PENH_ZONE).toInstant().toEpochMilli() - 1
    }
}
