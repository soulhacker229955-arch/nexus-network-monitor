package com.asad.nexus.networkmonitor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NexusBg = Color(0xFF080D18)
val NexusSurface = Color(0xFF111827)
val NexusCyan = Color(0xFF00D9FF)
val NexusMuted = Color(0xFF64748B)
val NexusText = Color(0xFFF8FAFC)
val NexusSuccess = Color(0xFF22C55E)
val NexusWarning = Color(0xFFF59E0B)
val NexusError = Color(0xFFEF4444)

private val Scheme = darkColorScheme(
    primary = NexusCyan,
    onPrimary = Color(0xFF00222B),
    secondary = NexusMuted,
    background = NexusBg,
    onBackground = NexusText,
    surface = NexusSurface,
    onSurface = NexusText,
    error = NexusError,
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
