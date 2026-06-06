package com.dulce.play.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    activeTheme: VisualTheme = VisualTheme.CYBER_NEON,
    overrideAccent: Color? = null,
    isExtremeSaver: Boolean = false,
    content: @Composable () -> Unit
) {
    val basePrimary = when(activeTheme) {
        VisualTheme.CYBER_NEON -> ElectricBlue
        VisualTheme.ELECTRIC_BLUE -> ElectricBlue
        VisualTheme.CLASSIC_DARK -> Color.White
        VisualTheme.NATURE_GREEN -> PremiumGold
    }
    
    val primaryColor = if (isExtremeSaver) Color.White else (overrideAccent ?: basePrimary)
    val background = if (isExtremeSaver) Color.Black else ElegantBlack

    val customColorScheme = darkColorScheme(
        primary = animateColorAsState(primaryColor, animationSpec = tween(600)).value,
        secondary = DeepPurple,
        tertiary = PremiumGold,
        background = background,
        surface = if (isExtremeSaver) Color.Black else DeepSurface,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        surfaceVariant = if (isExtremeSaver) Color.Black else CardDark
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = background.toArgb()
            window.navigationBarColor = background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = Typography,
        content = content
    )
}
