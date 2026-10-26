package com.expensetracker.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIcon(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "restaurant", "food" -> Icons.Default.Restaurant
            "local_bar", "drinks" -> Icons.Default.LocalBar
            "local_cafe", "coffee" -> Icons.Default.Coffee
            "directions_car", "transport" -> Icons.Default.DirectionsCar
            "school", "education" -> Icons.Default.School
            "home", "housing" -> Icons.Default.Home
            "receipt_long", "bills" -> Icons.Default.ReceiptLong
            "medical_services", "health" -> Icons.Default.MedicalServices
            "movie", "entertainment" -> Icons.Default.Movie
            "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
            "salary", "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            "storefront", "business" -> Icons.Default.Storefront
            "trending_up", "investment" -> Icons.Default.TrendingUp
            "card_giftcard", "gift" -> Icons.Default.CardGiftcard
            "bank", "wallet" -> Icons.Default.AccountBalance
            else -> Icons.Default.Category
        }
    }
}
