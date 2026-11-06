package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val VaultFont = FontFamily(
    Font(R.font.vault_regular, FontWeight.Normal),
    Font(R.font.vault_medium, FontWeight.Medium),
    Font(R.font.vault_semibold, FontWeight.SemiBold),
    Font(R.font.vault_semibold, FontWeight.Bold)
)
private fun text(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, spacing: Float = 0f) =
    TextStyle(fontFamily = VaultFont, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight, letterSpacing = spacing.sp)

val Typography = Typography(
    displayLarge = text(48, 58, FontWeight.SemiBold, -1.5f),
    displayMedium = text(40, 50, FontWeight.SemiBold, -1.2f),
    displaySmall = text(34, 44, FontWeight.SemiBold, -1f),
    headlineLarge = text(28, 38, FontWeight.SemiBold, -0.8f),
    headlineMedium = text(25, 34, FontWeight.SemiBold, -0.6f),
    headlineSmall = text(22, 30, FontWeight.SemiBold, -0.4f),
    titleLarge = text(20, 28, FontWeight.SemiBold, -0.3f),
    titleMedium = text(15, 23, FontWeight.SemiBold, -0.15f),
    titleSmall = text(13, 20, FontWeight.SemiBold),
    bodyLarge = text(15, 24), bodyMedium = text(13, 21), bodySmall = text(12, 19),
    labelLarge = text(14, 21, FontWeight.SemiBold), labelMedium = text(12, 18, FontWeight.Medium),
    labelSmall = text(10, 16, FontWeight.SemiBold, 0.7f)
)
