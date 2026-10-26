package com.expensetracker.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.expensetracker.app.core.model.DebtType
import com.expensetracker.app.core.model.RecurringFrequency
import com.expensetracker.app.core.model.TransactionType
import com.expensetracker.app.core.model.WalletType
import com.expensetracker.app.core.util.SecurityUtils
import com.expensetracker.app.data.local.ExpenseTrackerDatabase
import com.expensetracker.app.data.local.entity.BudgetEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.DebtEntity
import com.expensetracker.app.data.local.entity.DebtRepaymentEntity
import com.expensetracker.app.data.local.entity.RecurringTransactionEntity
import com.expensetracker.app.data.local.entity.SavingsContributionEntity
import com.expensetracker.app.data.local.entity.SavingsGoalEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.WalletEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ExpenseTrackerDatabase
) {

    suspend fun createEncryptedBackup(password: String): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("schema_version", 1)
        root.put("exported_at", System.currentTimeMillis())

        // 1. Categories
        val categories = database.categoryDao().getAllActive().first()
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("nameResKey", c.nameResKey ?: "")
            obj.put("type", c.type.name)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("isDefault", c.isDefault)
            obj.put("isArchived", c.isArchived)
            obj.put("sortOrder", c.sortOrder)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // 2. Wallets
        val wallets = database.walletDao().getAllActive().first()
        val walletArray = JSONArray()
        for (w in wallets) {
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("name", w.name)
            obj.put("type", w.type.name)
            obj.put("initialBalance", w.initialBalance.toPlainString())
            obj.put("currentBalance", w.currentBalance.toPlainString())
            obj.put("currency", w.currency)
            obj.put("isArchived", w.isArchived)
            obj.put("sortOrder", w.sortOrder)
            walletArray.put(obj)
        }
        root.put("wallets", walletArray)

        // 3. Transactions
        val transactions = database.transactionDao().getAllWithDetails().first()
        val transArray = JSONArray()
        for (item in transactions) {
            val t = item.transaction
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type.name)
            obj.put("amount", t.amount.toPlainString())
            obj.put("currency", t.currency)
            obj.put("categoryId", t.categoryId ?: JSONObject.NULL)
            obj.put("walletId", t.walletId)
            obj.put("toWalletId", t.toWalletId ?: JSONObject.NULL)
            obj.put("date", t.date)
            obj.put("merchant", t.merchant)
            obj.put("notes", t.notes)
            obj.put("isRecurring", t.isRecurring)
            obj.put("createdAt", t.createdAt)
            transArray.put(obj)
        }
        root.put("transactions", transArray)

        val jsonString = root.toString()
        val encryptedBytes = SecurityUtils.encryptBackup(jsonString.toByteArray(Charsets.UTF_8), password)

        val backupFile = File(context.filesDir, "ExpenseTracker_Backup_${System.currentTimeMillis()}.etb")
        FileOutputStream(backupFile).use { out ->
            out.write(encryptedBytes)
        }
        backupFile
    }

    suspend fun restoreEncryptedBackup(backupBytes: ByteArray, password: String): Boolean = withContext(Dispatchers.IO) {
        val decryptedBytes = SecurityUtils.decryptBackup(backupBytes, password)
        val jsonString = String(decryptedBytes, Charsets.UTF_8)
        val root = JSONObject(jsonString)

        val version = root.getInt("version")
        if (version != 1) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }

        database.withTransaction {
            // Restore Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val cat = CategoryEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        nameResKey = if (obj.has("nameResKey") && !obj.isNull("nameResKey")) obj.getString("nameResKey") else null,
                        type = TransactionType.valueOf(obj.getString("type")),
                        iconName = obj.optString("iconName", "category"),
                        colorHex = obj.optString("colorHex", "#000000"),
                        isDefault = obj.optBoolean("isDefault", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                    database.categoryDao().insert(cat)
                }
            }

            // Restore Wallets
            if (root.has("wallets")) {
                val walletArray = root.getJSONArray("wallets")
                for (i in 0 until walletArray.length()) {
                    val obj = walletArray.getJSONObject(i)
                    val wallet = WalletEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        type = WalletType.valueOf(obj.getString("type")),
                        initialBalance = BigDecimal(obj.getString("initialBalance")),
                        currentBalance = BigDecimal(obj.getString("currentBalance")),
                        currency = obj.optString("currency", "USD"),
                        isArchived = obj.optBoolean("isArchived", false),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                    database.walletDao().insert(wallet)
                }
            }

            // Restore Transactions
            if (root.has("transactions")) {
                val transArray = root.getJSONArray("transactions")
                for (i in 0 until transArray.length()) {
                    val obj = transArray.getJSONObject(i)
                    val trans = TransactionEntity(
                        id = obj.getLong("id"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        amount = BigDecimal(obj.getString("amount")),
                        currency = obj.optString("currency", "USD"),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                        walletId = obj.getLong("walletId"),
                        toWalletId = if (obj.isNull("toWalletId")) null else obj.getLong("toWalletId"),
                        date = obj.getLong("date"),
                        merchant = obj.optString("merchant", ""),
                        notes = obj.optString("notes", ""),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = System.currentTimeMillis()
                    )
                    database.transactionDao().insert(trans)
                }
            }
        }
        true
    }
}
