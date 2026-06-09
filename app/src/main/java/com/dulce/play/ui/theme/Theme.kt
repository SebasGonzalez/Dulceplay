package com.dulce.play.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dulce.play.ui.player.PlayerViewModel.VisualTheme


@Composable
fun MyApplicationTheme(
    activeTheme: VisualTheme = VisualTheme.CYBER_NEON,
    content: @Composable () -> Unit
) {
    // Resolve dynamic colors based on activeTheme
    val resolvedPrimary = when(activeTheme) {
        VisualTheme.CYBER_NEON -> PrimaryNeon
        VisualTheme.CLASSIC_DARK -> Color(0xFFF1F5F9) // Slate-White
        VisualTheme.ELECTRIC_BLUE -> Color(0xFF06B6D4) // Neon Blue-Cyan
        VisualTheme.NATURE_GREEN -> Color(0xFF10B981) // Neon Emerald
    }

    val resolvedSecondary = when(activeTheme) {
        VisualTheme.CYBER_NEON -> ElectricViolet
        VisualTheme.CLASSIC_DARK -> Color(0xFF475569) // Charcoal Steel
        VisualTheme.ELECTRIC_BLUE -> Color(0xFF3B82F6) // Deep Cobalt
        VisualTheme.NATURE_GREEN -> Color(0xFF059669) // Forest Green
    }

    val resolvedTertiary = when(activeTheme) {
        VisualTheme.CYBER_NEON -> AccentCyan
        VisualTheme.CLASSIC_DARK -> Color(0xFF94A3B8) // Bright Silver
        VisualTheme.ELECTRIC_BLUE -> Color(0xFF22D3EE) // Vibrant Ice-Cyan
        VisualTheme.NATURE_GREEN -> Color(0xFF34D399) // Mint Accent
    }

    val resolvedBackground = when(activeTheme) {
        VisualTheme.CYBER_NEON -> MidnightNavy
        VisualTheme.CLASSIC_DARK -> Color(0xFF0F172A) // Sleek Slate-Black
        VisualTheme.ELECTRIC_BLUE -> Color(0xFF0B132B) // Oceanic Tech Navy-Black
        VisualTheme.NATURE_GREEN -> Color(0xFF06140F) // Moss-Forest Black
    }

    val resolvedSurface = when(activeTheme) {
        VisualTheme.CYBER_NEON -> CosmicBlack
        VisualTheme.CLASSIC_DARK -> Color(0xFF020617)
        VisualTheme.ELECTRIC_BLUE -> Color(0xFF01081A)
        VisualTheme.NATURE_GREEN -> Color(0xFF010806)
    }

    val customColorScheme = darkColorScheme(
        primary = resolvedPrimary,
        onPrimary = TextPrimary,
        primaryContainer = resolvedSecondary.copy(alpha = 0.3f),
        secondary = resolvedSecondary,
        background = resolvedBackground,
        surface = resolvedSurface,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        tertiary = resolvedTertiary
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = resolvedBackground.toArgb()
            window.navigationBarColor = resolvedBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = Typography,
        content = content
    )
}

