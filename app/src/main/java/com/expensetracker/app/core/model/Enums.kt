package com.expensetracker.app.core.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class WalletType {
    CASH,
    ABA,
    ACLEDA,
    WING,
    CUSTOM
}

enum class DebtType {
    I_OWE,      // Payable: Money user owes to someone
    THEY_OWE    // Receivable: Money someone owes to user
}

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    KHMER("km", "ភាសាខ្មែរ (Khmer)")
}

enum class Currency(val code: String, val symbol: String, val displayName: String) {
    USD("USD", "$", "US Dollar ($)"),
    KHR("KHR", "៛", "Khmer Riel (៛)"),
    EUR("EUR", "€", "Euro (€)"),
    GBP("GBP", "£", "British Pound (£)"),
    THB("THB", "฿", "Thai Baht (฿)")
}
