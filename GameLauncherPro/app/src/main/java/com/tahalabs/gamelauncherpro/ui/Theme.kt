package com.tahalabs.gamelauncherpro.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palette: deep ink background, one calm mint accent, quiet greys. */
object Pal {
    val Bg = Color(0xFF0A0B0F)
    val Surface = Color(0xFF111319)
    val Raised = Color(0xFF181B23)
    val Line = Color(0xFF242833)
    val Text = Color(0xFFF2F3F6)
    val Dim = Color(0xFF8C919E)
    val Faint = Color(0xFF575C69)
    val Accent = Color(0xFF3DDC97)
    val Warn = Color(0xFFFFB454)
    val Hot = Color(0xFFFF5D6C)
}

private val Scheme = darkColorScheme(
    primary = Pal.Accent,
    onPrimary = Pal.Bg,
    background = Pal.Bg,
    onBackground = Pal.Text,
    surface = Pal.Surface,
    onSurface = Pal.Text,
    outline = Pal.Line
)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
