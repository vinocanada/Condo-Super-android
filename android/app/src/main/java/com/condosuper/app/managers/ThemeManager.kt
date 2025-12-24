package com.condosuper.app.managers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.condosuper.app.ui.theme.*

/**
 * Manages app theme selection (Dark, Light, Aurora, Royal, or Contractor)
 * Similar to iOS ThemeManager
 */
object ThemeManager {
    var currentTheme: AppTheme by mutableStateOf(AppTheme.DARK)
        private set

    enum class AppTheme(val displayName: String, val icon: String) {
        DARK("Dark", "moon.fill"),
        LIGHT("Light", "sun.max.fill"),
        AURORA("Aurora", "sparkles"),
        ROYAL("Royal", "crown.fill"),
        CONTRACTOR("Contractor", "hammer.fill");

        companion object {
            fun fromString(value: String?): AppTheme {
                return when (value) {
                    "Dark" -> DARK
                    "Light" -> LIGHT
                    "Aurora" -> AURORA
                    "Royal" -> ROYAL
                    "Contractor" -> CONTRACTOR
                    else -> DARK
                }
            }
        }
    }

    fun setTheme(theme: AppTheme, context: android.content.Context) {
        currentTheme = theme
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("appTheme", theme.displayName).apply()
    }

    fun loadTheme(context: android.content.Context) {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("appTheme", null)
        currentTheme = AppTheme.fromString(savedTheme)
    }

    // MARK: - Theme Colors

    val backgroundColor: Color
        get() = when (currentTheme) {
            AppTheme.DARK -> Color(0xFF0A0A0A)
            AppTheme.LIGHT -> Color(0xFFFFFFFF)
            AppTheme.AURORA -> Color(0xFF1E1E2E)
            AppTheme.ROYAL -> Color(0xFF0D0A12)
            AppTheme.CONTRACTOR -> Color(0xFFFFFFFF)
        }

    val cardColor: Color
        get() = when (currentTheme) {
            AppTheme.DARK -> Color(0xFF1C1C1E)
            AppTheme.LIGHT -> Color(0xFFF8F9FA)
            AppTheme.AURORA -> Color(0xFF2A2A40)
            AppTheme.ROYAL -> Color(0xFF1A1528)
            AppTheme.CONTRACTOR -> Color(0xFFF8F9FA)
        }

    val cardSecondaryColor: Color
        get() = when (currentTheme) {
            AppTheme.DARK -> Color(0xFF2C2C2E)
            AppTheme.LIGHT -> Color(0xFFE9ECEF)
            AppTheme.AURORA -> Color(0xFF3A3A55)
            AppTheme.ROYAL -> Color(0xFF2A2335)
            AppTheme.CONTRACTOR -> Color(0xFFE9ECEF)
        }

    val primaryGradient: List<Color>
        get() = when (currentTheme) {
            AppTheme.DARK -> listOf(BrandBlue, BrandDarkBlue)
            AppTheme.LIGHT -> listOf(Color(0xFF0077B6), Color(0xFF00B4D8))
            AppTheme.AURORA -> listOf(Color(0xFF38BDD4), Color(0xFF6289C0), Color(0xFF9361AA))
            AppTheme.ROYAL -> listOf(Color(0xFF9B59B6), Color(0xFF8E44AD))
            AppTheme.CONTRACTOR -> listOf(Color(0xFF1E3A5F), Color(0xFF2C5282))
        }

    val secondaryGradient: List<Color>
        get() = when (currentTheme) {
            AppTheme.DARK -> listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E))
            AppTheme.LIGHT -> listOf(Color(0xFF48CAE4), Color(0xFF90E0EF))
            AppTheme.AURORA -> listOf(Color(0xFF6289C0), Color(0xFF9361AA))
            AppTheme.ROYAL -> listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))
            AppTheme.CONTRACTOR -> listOf(Color(0xFF2C5282), Color(0xFF3182CE))
        }

    val accentGradient: List<Color>
        get() = when (currentTheme) {
            AppTheme.DARK -> listOf(BrandGreen, Color(0xFF6AB030))
            AppTheme.LIGHT -> listOf(Color(0xFF00B4D8), Color(0xFF48CAE4))
            AppTheme.AURORA -> listOf(Color(0xFF38BDD4), Color(0xFF6289C0))
            AppTheme.ROYAL -> listOf(Color(0xFFE056FD), Color(0xFFBE2EDD))
            AppTheme.CONTRACTOR -> listOf(Color(0xFFDD6B20), Color(0xFFED8936))
        }

    val accentColor: Color
        get() = when (currentTheme) {
            AppTheme.DARK -> BrandGreen
            AppTheme.LIGHT -> Color(0xFF0077B6)
            AppTheme.AURORA -> Color(0xFF38BDD4)
            AppTheme.ROYAL -> Color(0xFFE056FD)
            AppTheme.CONTRACTOR -> Color(0xFFDD6B20)
        }

    val tabBarBackground: Color
        get() = when (currentTheme) {
            AppTheme.DARK -> Color(0xFF1C1C1E)
            AppTheme.LIGHT -> Color(0xFFFFFFFF)
            AppTheme.AURORA -> Color(0xFF2A2A40)
            AppTheme.ROYAL -> Color(0xFF2D2438)
            AppTheme.CONTRACTOR -> Color(0xFF1E3A5F)
        }

    val headerGradient: List<Color>
        get() = when (currentTheme) {
            AppTheme.DARK -> listOf(Color(0xFF0A0A0A), Color(0xFF1C1C1E))
            AppTheme.LIGHT -> listOf(Color(0xFF0077B6), Color(0xFF00B4D8), Color(0xFF48CAE4))
            AppTheme.AURORA -> listOf(Color(0xFF38BDD4), Color(0xFF6289C0), Color(0xFF9361AA))
            AppTheme.ROYAL -> listOf(Color(0xFF6C5CE7), Color(0xFF9B59B6), Color(0xFFE056FD))
            AppTheme.CONTRACTOR -> listOf(Color(0xFF1E3A5F), Color(0xFF2C5282))
        }

    val buttonGradient: List<Color>
        get() = when (currentTheme) {
            AppTheme.DARK -> listOf(BrandBlue, BrandDarkBlue)
            AppTheme.LIGHT -> listOf(Color(0xFF0077B6), Color(0xFF00B4D8))
            AppTheme.AURORA -> listOf(Color(0xFF38BDD4), Color(0xFF9361AA))
            AppTheme.ROYAL -> listOf(Color(0xFF9B59B6), Color(0xFF6C5CE7))
            AppTheme.CONTRACTOR -> listOf(Color(0xFFDD6B20), Color(0xFFED8936))
        }

    val dangerGradient: List<Color>
        get() = when (currentTheme) {
            AppTheme.DARK -> listOf(Color.Red, Color(0xCCFF0000))
            AppTheme.LIGHT -> listOf(Color(0xFFE63946), Color(0xFFF07167))
            AppTheme.AURORA -> listOf(Color(0xFFFF6B6B), Color(0xFFEE5253))
            AppTheme.ROYAL -> listOf(Color(0xFFFF4757), Color(0xFFFF6B81))
            AppTheme.CONTRACTOR -> listOf(Color(0xFFC53030), Color(0xFFE53E3E))
        }

    // Text colors
    val textPrimary: Color
        get() = when (currentTheme) {
            AppTheme.DARK -> Color.White
            AppTheme.LIGHT -> Color(0xFF111111)
            AppTheme.AURORA -> Color.White
            AppTheme.ROYAL -> Color.White
            AppTheme.CONTRACTOR -> Color(0xFF111111)
        }

    val textSecondary: Color
        get() = when (currentTheme) {
            AppTheme.DARK -> Color(0xFF8E8E93)
            AppTheme.LIGHT -> Color(0xFF374151)
            AppTheme.AURORA -> Color(0xFFA0A0B0)
            AppTheme.ROYAL -> Color(0xFFA29BFE)
            AppTheme.CONTRACTOR -> Color(0xFF374151)
        }

    // Status colors
    val successColor: Color
        get() = when (currentTheme) {
            AppTheme.CONTRACTOR -> Color(0xFF276749)
            AppTheme.LIGHT -> Color(0xFF276749)
            else -> BrandGreen
        }

    val warningColor: Color
        get() = when (currentTheme) {
            AppTheme.CONTRACTOR -> Color(0xFFC05621)
            AppTheme.LIGHT -> Color(0xFFC05621)
            else -> Color(0xFFF59E0B)
        }

    val infoColor: Color
        get() = when (currentTheme) {
            AppTheme.CONTRACTOR -> Color(0xFF2B6CB0)
            AppTheme.LIGHT -> Color(0xFF2B6CB0)
            else -> BrandBlue
        }

    // Menu row gradients
    val menuGradient1: List<Color>
        get() = when (currentTheme) {
            AppTheme.AURORA -> listOf(Color(0xFF38BDD4), Color(0xFF4EA8C9))
            AppTheme.ROYAL -> listOf(Color(0xFF6C5CE7), Color(0xFF7D6EE7))
            AppTheme.CONTRACTOR -> listOf(Color(0xFF1E3A5F), Color(0xFF2C5282))
            else -> listOf(Color(0xFF03045E), Color(0xFF0077B6))
        }

    val menuGradient2: List<Color>
        get() = when (currentTheme) {
            AppTheme.AURORA -> listOf(Color(0xFF5A96BE), Color(0xFF6289C0))
            AppTheme.ROYAL -> listOf(Color(0xFF9B59B6), Color(0xFFA66BBE))
            AppTheme.CONTRACTOR -> listOf(Color(0xFF2C5282), Color(0xFF3182CE))
            else -> listOf(Color(0xFF0077B6), Color(0xFF00B4D8))
        }

    val menuGradient3: List<Color>
        get() = when (currentTheme) {
            AppTheme.AURORA -> listOf(Color(0xFF7578B4), Color(0xFF8A6AAA))
            AppTheme.ROYAL -> listOf(Color(0xFF8E44AD), Color(0xFF9B59B6))
            AppTheme.CONTRACTOR -> listOf(Color(0xFFDD6B20), Color(0xFFED8936))
            else -> listOf(Color(0xFF00B4D8), Color(0xFF48CAE4))
        }

    val menuGradient4: List<Color>
        get() = when (currentTheme) {
            AppTheme.AURORA -> listOf(Color(0xFF9361AA), Color(0xFF8560A0))
            AppTheme.ROYAL -> listOf(Color(0xFFE056FD), Color(0xFFD63CE6))
            AppTheme.CONTRACTOR -> listOf(Color(0xFF38A169), Color(0xFF48BB78))
            else -> listOf(Color(0xFF48CAE4), Color(0xFF90E0EF))
        }

    val menuGradient5: List<Color>
        get() = when (currentTheme) {
            AppTheme.AURORA -> listOf(Color(0xFF7A5D98), Color(0xFF6B5A95))
            AppTheme.ROYAL -> listOf(Color(0xFFBE2EDD), Color(0xFF9B59B6))
            AppTheme.CONTRACTOR -> listOf(Color(0xFF2D3748), Color(0xFF4A5568))
            else -> listOf(Color(0xFF90E0EF), Color(0xFFCAF0F8))
        }

    val fullSpectrumGradient: List<Color>
        get() = when (currentTheme) {
            AppTheme.AURORA -> listOf(
                Color(0xFF38BDD4),  // Cyan
                Color(0xFF4EA8C9),  // Light blue
                Color(0xFF6289C0),  // Blue
                Color(0xFF7578B4),  // Indigo
                Color(0xFF9361AA),  // Purple
                Color(0xFF6B5A95)   // Deep purple
            )
            AppTheme.ROYAL -> listOf(
                Color(0xFF6C5CE7),  // Violet
                Color(0xFFA29BFE),  // Lavender
                Color(0xFF9B59B6),  // Amethyst
                Color(0xFF8E44AD),  // Deep purple
                Color(0xFFE056FD),  // Magenta
                Color(0xFFBE2EDD)   // Hot pink
            )
            AppTheme.CONTRACTOR -> listOf(
                Color(0xFF1E3A5F),  // Navy
                Color(0xFF2C5282),  // Blue
                Color(0xFFDD6B20),  // Orange
                Color(0xFFED8936),  // Light orange
                Color(0xFF38A169),  // Green
                Color(0xFF2D3748)   // Slate
            )
            else -> listOf(
                Color(0xFF03045E),  // Deep navy
                Color(0xFF0077B6),  // Ocean blue
                Color(0xFF00B4D8),  // Cyan
                Color(0xFF48CAE4),  // Aqua
                Color(0xFF90E0EF),  // Light aqua
                Color(0xFFCAF0F8)   // Sea foam
            )
        }
}

