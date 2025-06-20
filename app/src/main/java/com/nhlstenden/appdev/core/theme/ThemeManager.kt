package com.nhlstenden.appdev.core.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.nhlstenden.appdev.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    private val THEME_COLOR_KEY = "custom_theme_color"

    fun getCustomThemeColor(): Int? {
        val colorValue = sharedPreferences.getString(THEME_COLOR_KEY, null)
        return colorValue?.let { parseColor(it) }
    }

    fun hasCustomTheme(): Boolean {
        return sharedPreferences.contains(THEME_COLOR_KEY)
    }

    fun clearCustomTheme() {
        sharedPreferences.edit().remove(THEME_COLOR_KEY).apply()
    }

    fun getPrimaryColor(): Int {
        return getCustomThemeColor() ?: ContextCompat.getColor(context, R.color.colorPrimary)
    }

    fun getPrimaryDarkColor(): Int {
        val customColor = getCustomThemeColor()
        return if (customColor != null) {
            // Darken the custom color by 20%
            darkenColor(customColor, 0.2f)
        } else {
            ContextCompat.getColor(context, R.color.primary_dark)
        }
    }

    fun getAccentColor(): Int {
        val customColor = getCustomThemeColor()
        return if (customColor != null) {
            // Lighten the custom color by 30% for accent
            lightenColor(customColor, 0.3f)
        } else {
            ContextCompat.getColor(context, R.color.accent)
        }
    }

    private fun parseColor(colorValue: String): Int {
        val trimmed = colorValue.trim()
        
        return when {
            trimmed.startsWith("#") -> {
                Color.parseColor(trimmed)
            }
            trimmed.startsWith("rgb(") && trimmed.endsWith(")") -> {
                val rgbValues = trimmed.substring(4, trimmed.length - 1)
                    .split(",")
                    .map { it.trim().toInt() }
                Color.rgb(rgbValues[0], rgbValues[1], rgbValues[2])
            }
            else -> {
                Color.parseColor(trimmed)
            }
        }
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= (1 - factor) // Reduce value (brightness)
        return Color.HSVToColor(hsv)
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] + factor).coerceAtMost(1f) // Increase value (brightness)
        return Color.HSVToColor(hsv)
    }
} 