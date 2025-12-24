package com.condosuper.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.condosuper.app.managers.ThemeManager

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    secondary = BrandBlue,
    tertiary = GradientTeal,
    background = DarkBackground,
    surface = DarkCard,
    onPrimary = DarkText,
    onSecondary = DarkText,
    onTertiary = DarkText,
    onBackground = DarkText,
    onSurface = DarkText,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0077B6),
    secondary = Color(0xFF00B4D8),
    tertiary = Color(0xFF48CAE4),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF8F9FA),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
)

@Composable
fun CondoSuperTheme(
    darkTheme: Boolean? = null, // null = use ThemeManager
    dynamicColor: Boolean = false, // Disable dynamic color to use custom themes
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Load theme on first composition
    DisposableEffect(Unit) {
        ThemeManager.loadTheme(context)
        onDispose { }
    }
    
    // Determine if we should use dark theme based on ThemeManager
    val isDarkTheme = darkTheme ?: when (ThemeManager.currentTheme) {
        ThemeManager.AppTheme.DARK, 
        ThemeManager.AppTheme.AURORA, 
        ThemeManager.AppTheme.ROYAL -> true
        ThemeManager.AppTheme.LIGHT, 
        ThemeManager.AppTheme.CONTRACTOR -> false
    }
    
    // Build color scheme from ThemeManager
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDarkTheme -> {
            // Use ThemeManager colors for dark themes
            darkColorScheme(
                primary = ThemeManager.accentColor,
                secondary = ThemeManager.primaryGradient.firstOrNull() ?: BrandBlue,
                tertiary = ThemeManager.accentGradient.firstOrNull() ?: GradientTeal,
                background = ThemeManager.backgroundColor,
                surface = ThemeManager.cardColor,
                onPrimary = ThemeManager.textPrimary,
                onSecondary = ThemeManager.textPrimary,
                onTertiary = ThemeManager.textPrimary,
                onBackground = ThemeManager.textPrimary,
                onSurface = ThemeManager.textPrimary,
            )
        }
        else -> {
            // Use ThemeManager colors for light themes
            lightColorScheme(
                primary = ThemeManager.accentColor,
                secondary = ThemeManager.primaryGradient.firstOrNull() ?: Color(0xFF00B4D8),
                tertiary = ThemeManager.accentGradient.firstOrNull() ?: Color(0xFF48CAE4),
                background = ThemeManager.backgroundColor,
                surface = ThemeManager.cardColor,
                onPrimary = ThemeManager.textPrimary,
                onSecondary = ThemeManager.textPrimary,
                onTertiary = ThemeManager.textPrimary,
                onBackground = ThemeManager.textPrimary,
                onSurface = ThemeManager.textPrimary,
            )
        }
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


