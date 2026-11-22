package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = DeepIndigoInk,
    primaryContainer = Color(0xFF31278A),
    onPrimaryContainer = Color(0xFFE6E2FF),
    secondary = VioletSecondaryDark,
    onSecondary = Color(0xFF281259),
    secondaryContainer = Color(0xFF42268B),
    onSecondaryContainer = Color(0xFFECE3FF),
    tertiary = SoftBlueTertiaryDark,
    onTertiary = Color(0xFF002F54),
    tertiaryContainer = Color(0xFF15497B),
    onTertiaryContainer = Color(0xFFD6EAFF),
    background = TwilightBackground,
    onBackground = Color(0xFFF3F1FF),
    surface = TwilightSurface,
    onSurface = Color(0xFFF3F1FF),
    surfaceVariant = TwilightSurfaceElevated,
    onSurfaceVariant = MutedSlateDark,
    surfaceContainerLow = TwilightSurface,
    surfaceContainer = TwilightSurfaceElevated,
    surfaceContainerHigh = TwilightSurfaceHighest,
    outline = Color(0xFF3E3868),
    error = Color(0xFFFF7A85),
    onError = Color(0xFF4E0010)
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E3FF),
    onPrimaryContainer = Color(0xFF1B126B),
    secondary = VioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEE6FF),
    onSecondaryContainer = Color(0xFF2A0C70),
    tertiary = SoftBlueTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCEFFF),
    onTertiaryContainer = Color(0xFF002D56),
    background = PorcelainBackground,
    onBackground = DeepIndigoInk,
    surface = PorcelainSurface,
    onSurface = DeepIndigoInk,
    surfaceVariant = PorcelainSurfaceVariant,
    onSurfaceVariant = MutedSlateLight,
    surfaceContainerLow = PorcelainSurface,
    surfaceContainer = PorcelainSurfaceContainer,
    surfaceContainerHigh = PorcelainSurfaceVariant,
    outline = Color(0xFFD3CCE9),
    error = DangerRose,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun ManojSonaliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
