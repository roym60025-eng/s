package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeStyle

@Immutable
data class AppThemePalette(
    val style: AppThemeStyle,
    val isDark: Boolean,
    val headerColor: Color,
    val buttonColor: Color,
    val accentColor: Color,
    val accentLightColor: Color,
    val outgoingBubbleColor: Color,
    val incomingBubbleColor: Color,
    val chatWallpaperColor: Color,
    val voiceCallGradientTop: Color,
    val voiceCallGradientMid: Color,
    val voiceCallGradientBottom: Color,
    val waveSoftColor: Color,
    val waveDeepColor: Color,
    val colorScheme: ColorScheme
)

fun themePaletteFor(style: AppThemeStyle, darkTheme: Boolean): AppThemePalette {
    return when (style) {
        AppThemeStyle.EMERALD -> createPalette(
            style = style,
            darkTheme = darkTheme,
            lightPrimary = ForestGreenHeader,
            lightSecondary = BrandGreen,
            lightButton = EmeraldButton,
            lightAccentGlow = BrandGreenLight,
            lightPrimaryContainer = OutgoingGreenLight,
            lightOnPrimaryContainer = Color(0xFF053D26),
            lightBg = PorcelainBackground,
            lightSurfaceVariant = PorcelainSurfaceVariant,
            lightSurfaceContainer = PorcelainSurfaceContainer,
            lightSurfaceHigh = Color(0xFFE7F0EA),
            lightOutline = Color(0xFFD0DDD5),
            lightWallpaper = ChatWallpaperLight,
            darkHeader = ForestGreenDarkHeader,
            darkPrimary = BrandGreenLight,
            darkButton = Color(0xFF0F8D49),
            darkOutgoing = OutgoingGreenDark,
            darkWallpaper = ChatWallpaperDark,
            darkBg = TwilightBackground,
            darkSurface = TwilightSurface,
            darkSurfaceElevated = TwilightSurfaceElevated,
            darkSurfaceHigh = TwilightSurfaceHighest,
            voiceTop = VoiceCallGradientTop,
            voiceMid = VoiceCallGradientMid,
            voiceBottom = VoiceCallGradientBottom
        )

        AppThemeStyle.OCEAN -> createPalette(
            style = style,
            darkTheme = darkTheme,
            lightPrimary = Color(0xFF0D47A1),
            lightSecondary = Color(0xFF1E88E5),
            lightButton = Color(0xFF1565C0),
            lightAccentGlow = Color(0xFF42A5F5),
            lightPrimaryContainer = Color(0xFFD5E9FF),
            lightOnPrimaryContainer = Color(0xFF072D5E),
            lightBg = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFEAF2FC),
            lightSurfaceContainer = Color(0xFFF3F7FD),
            lightSurfaceHigh = Color(0xFFE2ECF9),
            lightOutline = Color(0xFFCBD8EA),
            lightWallpaper = Color(0xFFE9F0F8),
            darkHeader = Color(0xFF13263E),
            darkPrimary = Color(0xFF64B5F6),
            darkButton = Color(0xFF1976D2),
            darkOutgoing = Color(0xFF0B3F72),
            darkWallpaper = Color(0xFF0A121D),
            darkBg = Color(0xFF0A121D),
            darkSurface = Color(0xFF101B29),
            darkSurfaceElevated = Color(0xFF18273A),
            darkSurfaceHigh = Color(0xFF21344D),
            voiceTop = Color(0xFF08254B),
            voiceMid = Color(0xFF0E396E),
            voiceBottom = Color(0xFF041428)
        )

        AppThemeStyle.ROSE -> createPalette(
            style = style,
            darkTheme = darkTheme,
            lightPrimary = Color(0xFF880E4F),
            lightSecondary = Color(0xFFE91E63),
            lightButton = Color(0xFFAD1457),
            lightAccentGlow = Color(0xFFFF4081),
            lightPrimaryContainer = Color(0xFFFFDCE8),
            lightOnPrimaryContainer = Color(0xFF540529),
            lightBg = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFFBEEF3),
            lightSurfaceContainer = Color(0xFFFCF4F7),
            lightSurfaceHigh = Color(0xFFF6E3EB),
            lightOutline = Color(0xFFE8CCD7),
            lightWallpaper = Color(0xFFF7EEF2),
            darkHeader = Color(0xFF2C1624),
            darkPrimary = Color(0xFFF48FB1),
            darkButton = Color(0xFFC2185B),
            darkOutgoing = Color(0xFF631339),
            darkWallpaper = Color(0xFF140B11),
            darkBg = Color(0xFF140B11),
            darkSurface = Color(0xFF1D1019),
            darkSurfaceElevated = Color(0xFF2B1825),
            darkSurfaceHigh = Color(0xFF392031),
            voiceTop = Color(0xFF470828),
            voiceMid = Color(0xFF690E3C),
            voiceBottom = Color(0xFF250415)
        )

        AppThemeStyle.VIOLET -> createPalette(
            style = style,
            darkTheme = darkTheme,
            lightPrimary = Color(0xFF4A148C),
            lightSecondary = Color(0xFF8E24AA),
            lightButton = Color(0xFF6A1B9A),
            lightAccentGlow = Color(0xFFBA68C8),
            lightPrimaryContainer = Color(0xFFEDD9FF),
            lightOnPrimaryContainer = Color(0xFF31085B),
            lightBg = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFF3ECF9),
            lightSurfaceContainer = Color(0xFFF7F3FC),
            lightSurfaceHigh = Color(0xFFECE2F5),
            lightOutline = Color(0xFFD9CBE8),
            lightWallpaper = Color(0xFFF2EDF7),
            darkHeader = Color(0xFF231834),
            darkPrimary = Color(0xFFCE93D8),
            darkButton = Color(0xFF7B1FA2),
            darkOutgoing = Color(0xFF4A1B6D),
            darkWallpaper = Color(0xFF100B19),
            darkBg = Color(0xFF100B19),
            darkSurface = Color(0xFF181124),
            darkSurfaceElevated = Color(0xFF231934),
            darkSurfaceHigh = Color(0xFF302346),
            voiceTop = Color(0xFF2C0D52),
            voiceMid = Color(0xFF441678),
            voiceBottom = Color(0xFF17072B)
        )

        AppThemeStyle.SUNSET -> createPalette(
            style = style,
            darkTheme = darkTheme,
            lightPrimary = Color(0xFF9E3407),
            lightSecondary = Color(0xFFEF6C00),
            lightButton = Color(0xFFC74208),
            lightAccentGlow = Color(0xFFFF9800),
            lightPrimaryContainer = Color(0xFFFFE5CC),
            lightOnPrimaryContainer = Color(0xFF4F1800),
            lightBg = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFFDF1E8),
            lightSurfaceContainer = Color(0xFFFEF6F0),
            lightSurfaceHigh = Color(0xFFF9E6D8),
            lightOutline = Color(0xFFEBD4C3),
            lightWallpaper = Color(0xFFF8EFE8),
            darkHeader = Color(0xFF2D1E16),
            darkPrimary = Color(0xFFFFB74D),
            darkButton = Color(0xFFE65100),
            darkOutgoing = Color(0xFF6A2A08),
            darkWallpaper = Color(0xFF150E0A),
            darkBg = Color(0xFF150E0A),
            darkSurface = Color(0xFF1E140F),
            darkSurfaceElevated = Color(0xFF2C1E17),
            darkSurfaceHigh = Color(0xFF3A281F),
            voiceTop = Color(0xFF4D1A05),
            voiceMid = Color(0xFF6E290A),
            voiceBottom = Color(0xFF280C02)
        )

        AppThemeStyle.TEAL -> createPalette(
            style = style,
            darkTheme = darkTheme,
            lightPrimary = Color(0xFF004D47),
            lightSecondary = Color(0xFF00897B),
            lightButton = Color(0xFF00695C),
            lightAccentGlow = Color(0xFF26A69A),
            lightPrimaryContainer = Color(0xFFCFF5EF),
            lightOnPrimaryContainer = Color(0xFF00332D),
            lightBg = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFE8F5F3),
            lightSurfaceContainer = Color(0xFFF2F9F8),
            lightSurfaceHigh = Color(0xFFDDEEEB),
            lightOutline = Color(0xFFC8DFDC),
            lightWallpaper = Color(0xFFEBF3F2),
            darkHeader = Color(0xFF142A2A),
            darkPrimary = Color(0xFF4DB6AC),
            darkButton = Color(0xFF00796B),
            darkOutgoing = Color(0xFF004D45),
            darkWallpaper = Color(0xFF0A1414),
            darkBg = Color(0xFF0A1414),
            darkSurface = Color(0xFF101D1D),
            darkSurfaceElevated = Color(0xFF182B2B),
            darkSurfaceHigh = Color(0xFF213939),
            voiceTop = Color(0xFF04322E),
            voiceMid = Color(0xFF094B45),
            voiceBottom = Color(0xFF021A18)
        )

        AppThemeStyle.MIDNIGHT -> createPalette(
            style = style,
            darkTheme = darkTheme,
            lightPrimary = Color(0xFF1C2331),
            lightSecondary = Color(0xFFD49B27),
            lightButton = Color(0xFF2C3547),
            lightAccentGlow = Color(0xFFE8B849),
            lightPrimaryContainer = Color(0xFFF4E6C3),
            lightOnPrimaryContainer = Color(0xFF2B2108),
            lightBg = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFF1F2F6),
            lightSurfaceContainer = Color(0xFFF7F8FA),
            lightSurfaceHigh = Color(0xFFE6E8EF),
            lightOutline = Color(0xFFD2D6E0),
            lightWallpaper = Color(0xFFECEEF3),
            darkHeader = Color(0xFF161A23),
            darkPrimary = Color(0xFFEEC15E),
            darkButton = Color(0xFFB37E18),
            darkOutgoing = Color(0xFF282E3D),
            darkWallpaper = Color(0xFF090B10),
            darkBg = Color(0xFF090B10),
            darkSurface = Color(0xFF10131C),
            darkSurfaceElevated = Color(0xFF1A1F2B),
            darkSurfaceHigh = Color(0xFF242A3A),
            voiceTop = Color(0xFF151924),
            voiceMid = Color(0xFF202636),
            voiceBottom = Color(0xFF0A0D13)
        )
    }
}

private fun createPalette(
    style: AppThemeStyle,
    darkTheme: Boolean,
    lightPrimary: Color,
    lightSecondary: Color,
    lightButton: Color,
    lightAccentGlow: Color,
    lightPrimaryContainer: Color,
    lightOnPrimaryContainer: Color,
    lightBg: Color,
    lightSurfaceVariant: Color,
    lightSurfaceContainer: Color,
    lightSurfaceHigh: Color,
    lightOutline: Color,
    lightWallpaper: Color,
    darkHeader: Color,
    darkPrimary: Color,
    darkButton: Color,
    darkOutgoing: Color,
    darkWallpaper: Color,
    darkBg: Color,
    darkSurface: Color,
    darkSurfaceElevated: Color,
    darkSurfaceHigh: Color,
    voiceTop: Color,
    voiceMid: Color,
    voiceBottom: Color
): AppThemePalette {
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = darkPrimary,
            onPrimary = Color(0xFF0B141A),
            primaryContainer = darkOutgoing,
            onPrimaryContainer = Color(0xFFF0F4F8),
            secondary = darkPrimary,
            onSecondary = Color(0xFF0B141A),
            secondaryContainer = darkSurfaceElevated,
            onSecondaryContainer = Color(0xFFE9EDEF),
            tertiary = SoftBlueTertiaryDark,
            onTertiary = Color(0xFF560024),
            tertiaryContainer = Color(0xFF740034),
            onTertiaryContainer = Color(0xFFFFD9E4),
            background = darkBg,
            onBackground = Color(0xFFE9EDEF),
            surface = darkSurface,
            onSurface = Color(0xFFE9EDEF),
            surfaceVariant = darkSurfaceElevated,
            onSurfaceVariant = MutedSlateDark,
            surfaceContainerLow = darkSurface,
            surfaceContainer = darkSurfaceElevated,
            surfaceContainerHigh = darkSurfaceHigh,
            outline = Color(0xFF2D3F4A),
            error = Color(0xFFFF6B6B),
            onError = Color(0xFF4E000A)
        )
    } else {
        lightColorScheme(
            primary = lightPrimary,
            onPrimary = Color.White,
            primaryContainer = lightPrimaryContainer,
            onPrimaryContainer = lightOnPrimaryContainer,
            secondary = lightSecondary,
            onSecondary = Color.White,
            secondaryContainer = lightSurfaceVariant,
            onSecondaryContainer = lightOnPrimaryContainer,
            tertiary = BrandPink,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD9E6),
            onTertiaryContainer = Color(0xFF5D0024),
            background = lightBg,
            onBackground = DeepIndigoInk,
            surface = lightBg,
            onSurface = DeepIndigoInk,
            surfaceVariant = lightSurfaceVariant,
            onSurfaceVariant = MutedSlateLight,
            surfaceContainerLow = Color(0xFFFDFEFE),
            surfaceContainer = lightSurfaceContainer,
            surfaceContainerHigh = lightSurfaceHigh,
            outline = lightOutline,
            error = DangerRose,
            onError = Color.White
        )
    }

    return AppThemePalette(
        style = style,
        isDark = darkTheme,
        headerColor = if (darkTheme) darkHeader else lightPrimary,
        buttonColor = if (darkTheme) darkButton else lightButton,
        accentColor = if (darkTheme) darkPrimary else lightSecondary,
        accentLightColor = if (darkTheme) darkPrimary else lightAccentGlow,
        outgoingBubbleColor = if (darkTheme) darkOutgoing else lightPrimaryContainer,
        incomingBubbleColor = if (darkTheme) darkSurfaceElevated else IncomingBubbleLight,
        chatWallpaperColor = if (darkTheme) darkWallpaper else lightWallpaper,
        voiceCallGradientTop = voiceTop,
        voiceCallGradientMid = voiceMid,
        voiceCallGradientBottom = voiceBottom,
        waveSoftColor = if (darkTheme) darkPrimary else lightAccentGlow,
        waveDeepColor = if (darkTheme) darkButton else lightSecondary,
        colorScheme = scheme
    )
}

val LocalAppThemePalette = staticCompositionLocalOf {
    themePaletteFor(AppThemeStyle.EMERALD, darkTheme = false)
}

object AppThemeTokens {
    val current: AppThemePalette
        @Composable get() = LocalAppThemePalette.current
}

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun ManojSonaliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeStyle: AppThemeStyle = AppThemeStyle.EMERALD,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val basePalette = themePaletteFor(themeStyle, darkTheme)
    val effectiveScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        basePalette.colorScheme
    }

    CompositionLocalProvider(LocalAppThemePalette provides basePalette) {
        MaterialTheme(
            colorScheme = effectiveScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
