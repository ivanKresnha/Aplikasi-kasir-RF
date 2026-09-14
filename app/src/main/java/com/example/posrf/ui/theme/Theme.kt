package com.example.posrf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),         // Emerald Green
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFF34D399),
    
    background = Color(0xFF0F172A),       // Charcoal Dark
    onBackground = Color(0xFFF8FAFC),
    
    surface = Color(0xFF1E293B),          // Slate card background
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),   // Slightly lighter slate
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun POSRFTheme(
    darkTheme: Boolean = true, // Force Dark Mode
    dynamicColor: Boolean = false, // Disable system dynamic tinting
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
