package com.fitlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = VoltDeep,
    onPrimary = VoltInk,
    primaryContainer = Color(0xFFE7F7B0),
    onPrimaryContainer = Color(0xFF3A4517),
    secondary = Ink2Light,
    onSecondary = Color.White,
    background = BgLight,
    onBackground = InkLight,
    surface = CardLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Ink2Light,
    outline = Color(0xFFC9CEDA),
    outlineVariant = LineLight,
    error = Red,
    tertiary = VoltDeep,
    onTertiary = VoltInk
)

private val DarkColors = darkColorScheme(
    primary = Volt,
    onPrimary = VoltInk,
    primaryContainer = Color(0xFF3A4517),
    onPrimaryContainer = Color(0xFFDFF59A),
    secondary = Color(0xFF8B93A5),
    onSecondary = Color(0xFF0C0E13),
    background = BgDark,
    onBackground = Color(0xFFF2F4F8),
    surface = CardDark,
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF9BA3B0),
    outline = Color(0xFF3A4152),
    outlineVariant = LineDark,
    error = Red,
    tertiary = Volt,
    onTertiary = VoltInk
)

@Composable
fun FitLogTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}

/** 强调文字色：暗色底用 Volt，浅色底用深橄榄（保证对比度） */
@Composable
fun accentText(): Color = if (isSystemInDarkTheme()) VoltTextDark else VoltTextLight
