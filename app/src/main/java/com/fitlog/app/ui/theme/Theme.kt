package com.fitlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = Color(0xFF1A4BCC),
    secondary = Color(0xFF7C5CFF),
    background = Color(0xFFF5F6F8),
    onBackground = InkDark,
    surface = Color.White,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFF1F3F7),
    onSurfaceVariant = Ink2Dark,
    outline = Color(0xFFD7DCE5),
    outlineVariant = Color(0xFFEBEDF2),
    error = Red,
    tertiary = Green
)

private val DarkColors = darkColorScheme(
    primary = BlueDark,
    onPrimary = Color(0xFF0B1B3D),
    primaryContainer = Color(0xFF23304F),
    onPrimaryContainer = Color(0xFFB9CDFF),
    secondary = Color(0xFF9B82FF),
    background = BgDark,
    onBackground = Color(0xFFF0F2F7),
    surface = CardDark,
    onSurface = Color(0xFFF0F2F7),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFA8B0BF),
    outline = Color(0xFF3A4152),
    outlineVariant = LineDark,
    error = Red,
    tertiary = Green
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
