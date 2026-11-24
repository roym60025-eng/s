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
    primary = BrandGreenLight,
    onPrimary = Color(0xFF00391B),
    primaryContainer = OutgoingGreenDark,
    onPrimaryContainer = Color(0xFFD9FDD3),
    secondary = VioletSecondaryDark,
    onSecondary = Color(0xFF00391B),
    secondaryContainer = Color(0xFF154236),
    onSecondaryContainer = Color(0xFFC8F7DB),
    tertiary = SoftBlueTertiaryDark,
    onTertiary = Color(0xFF560024),
    tertiaryContainer = Color(0xFF740034),
    onTertiaryContainer = Color(0xFFFFD9E4),
    background = TwilightBackground,
    onBackground = Color(0xFFE9EDEF),
    surface = TwilightSurface,
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = TwilightSurfaceElevated,
    onSurfaceVariant = MutedSlateDark,
    surfaceContainerLow = TwilightSurface,
    surfaceContainer = TwilightSurfaceElevated,
    surfaceContainerHigh = TwilightSurfaceHighest,
    outline = Color(0xFF2D3F4A),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF4E000A)
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenHeader,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9FDD3),
    onPrimaryContainer = Color(0xFF053D26),
    secondary = BrandGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F6EA),
    onSecondaryContainer = Color(0xFF064724),
    tertiary = BrandPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E6),
    onTertiaryContainer = Color(0xFF5D0024),
    background = PorcelainBackground,
    onBackground = DeepIndigoInk,
    surface = PorcelainSurface,
    onSurface = DeepIndigoInk,
    surfaceVariant = PorcelainSurfaceVariant,
    onSurfaceVariant = MutedSlateLight,
    surfaceContainerLow = Color(0xFFFDFEFE),
    surfaceContainer = PorcelainSurfaceContainer,
    surfaceContainerHigh = Color(0xFFE7F0EA),
    outline = Color(0xFFD0DDD5),
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
