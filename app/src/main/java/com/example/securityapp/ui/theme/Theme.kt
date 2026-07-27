package com.example.securityapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KineticFortressColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = Primary, // Used for gradients/containers
    onPrimaryContainer = OnPrimary,
    
    secondary        = Secondary,
    onSecondary      = OnSecondary,
    secondaryContainer = SurfaceHigh,
    
    tertiary         = Success,
    onTertiary       = Color(0xFF003825),
    
    background       = SurfaceBase,
    onBackground     = OnSurface,
    
    surface          = SurfaceBase,
    onSurface        = OnSurface,
    
    surfaceVariant   = SurfaceLow,
    onSurfaceVariant = OnSurfaceVariant,
    
    error            = Danger,
    onError          = Color(0xFF690005),
    
    outline          = Color(0xFF404752), // outline-variant fallback
    outlineVariant   = Color(0xFF404752)
)

@Composable
fun SecurityAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KineticFortressColorScheme,
        typography = Typography,
        content = content
    )
}
