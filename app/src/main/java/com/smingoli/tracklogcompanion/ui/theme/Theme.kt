package com.smingoli.tracklogcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF12100E)
private val Espresso = Color(0xFF1B1714)
private val Card = Color(0xFF28211C)
private val Amber = Color(0xFFF4B860)
private val Cream = Color(0xFFFFF7EA)
private val Muted = Color(0xFFC8BCAE)

private val TrackLogColors = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    secondary = Amber,
    background = Ink,
    onBackground = Cream,
    surface = Espresso,
    onSurface = Cream,
    surfaceVariant = Card,
    onSurfaceVariant = Muted,
    outline = Color(0xFF5A4C40),
)

@Composable
fun TrackLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrackLogColors,
        typography = TrackLogTypography,
        content = content,
    )
}

