package com.expensetracker.app.data.local

import androidx.room.TypeConverter
import com.expensetracker.app.core.model.DebtType
import com.expensetracker.app.core.model.RecurringFrequency
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.model.WalletType
import java.math.BigDecimal

class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { enumValueOf<TransactionType>(it) }
    }

    @TypeConverter
    fun fromWalletType(type: WalletType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toWalletType(value: String?): WalletType? {
        return value?.let { enumValueOf<WalletType>(it) }
    }

    @TypeConverter
    fun fromDebtType(type: DebtType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toDebtType(value: String?): DebtType? {
        return value?.let { enumValueOf<DebtType>(it) }
    }

    @TypeConverter
    fun fromRecurringFrequency(frequency: RecurringFrequency?): String? {
        return frequency?.name
    }

    @TypeConverter
    fun toRecurringFrequency(value: String?): RecurringFrequency? {
        return value?.let { enumValueOf<RecurringFrequency>(it) }
    }
}
