package com.sahalos.sboard.model

import kotlinx.serialization.Serializable

@Serializable
data class SboardTheme(
    val id: String = "nord_dark",
    val name: String = "Nord Dark",
    val backgroundColor: Long = 0xFF2E3440,
    val surfaceColor: Long = 0xFF3B4252,
    val keyBackgroundColor: Long = 0xFF434C5E,
    val keyActionColor: Long = 0xFF88C0D0,
    val keyTextColor: Long = 0xFFECEFF4,
    val keySecondaryTextColor: Long = 0xFFD8DEE9,
    val accentColor: Long = 0xFF81A1C1,
    val keyCornerRadiusDp: Int = 8,
    val keyElevationDp: Int = 2,
    val isDark: Boolean = true
) {
    companion object {
        val OledBlack = SboardTheme(
            id = "oled_black",
            name = "OLED Black",
            backgroundColor = 0xFF000000,
            surfaceColor = 0xFF121212,
            keyBackgroundColor = 0xFF1E1E1E,
            keyActionColor = 0xFF3B82F6,
            keyTextColor = 0xFFFFFFFF,
            keySecondaryTextColor = 0xFF9CA3AF,
            accentColor = 0xFF60A5FA,
            keyCornerRadiusDp = 8,
            isDark = true
        )

        val MaterialLight = SboardTheme(
            id = "material_light",
            name = "Material Light",
            backgroundColor = 0xFFF1F3F4,
            surfaceColor = 0xFFE8EAED,
            keyBackgroundColor = 0xFFFFFFFF,
            keyActionColor = 0xFF1A73E8,
            keyTextColor = 0xFF202124,
            keySecondaryTextColor = 0xFF5F6368,
            accentColor = 0xFF1A73E8,
            keyCornerRadiusDp = 8,
            isDark = false
        )

        val AvailableThemes = listOf(OledBlack, MaterialLight)
    }
}
