package com.fitplan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Green = Color(0xFF66BB6A)
val Amber = Color(0xFFFFCA28)
val Blue = Color(0xFF42A5F5)

private val colors = darkColorScheme(
    primary = Green,
    onPrimary = Color(0xFF0B1F0C),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Blue,
    tertiary = Amber,
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceContainer = Color(0xFF1C1C1C),
    error = Color(0xFFEF5350),
)

private val base = Typography()
private val typography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun FitPlanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
