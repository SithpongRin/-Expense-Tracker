package com.expensetracker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.expensetracker.app.core.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = PureWhite,
    secondary = LightTextDark,
    onSecondary = PureBlack,
    background = DarkBackground,
    onBackground = LightTextDark,
    surface = DarkSurface,
    onSurface = LightTextDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = GrayTextDark,
    outline = BorderDark,
    outlineVariant = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = SurfaceLight,
    onPrimaryContainer = PureBlack,
    secondary = DarkTextLight,
    onSecondary = PureWhite,
    background = PureWhite,
    onBackground = DarkTextLight,
    surface = OffWhite,
    onSurface = DarkTextLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = GrayTextLight,
    outline = BorderLight,
    outlineVariant = BorderLight
)

@Composable
fun ExpenseTrackerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
