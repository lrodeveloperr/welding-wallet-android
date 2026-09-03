package com.goodusestudios.weldinggaswallet.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WeldingWalletColors = lightColorScheme(
    primary = Color(0xFF1758B7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9F1FF),
    onPrimaryContainer = Color(0xFF15202F),
    secondary = Color(0xFF3F5F88),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF15202F),
    surface = Color.White,
    onSurface = Color(0xFF15202F),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF687383),
    outline = Color(0xFFDFE3E8),
    error = Color(0xFFBB3434),
)

@Composable
fun ShellTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WeldingWalletColors, typography = MaterialTheme.typography, content = content)
}
