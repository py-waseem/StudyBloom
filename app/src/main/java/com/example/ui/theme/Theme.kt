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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val StudyBloomShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = PureWhite,
    primaryContainer = LavenderLight,
    onPrimaryContainer = LavenderDark,
    secondary = PastelBlue,
    onSecondary = PureWhite,
    secondaryContainer = PastelBlueLight,
    onSecondaryContainer = PastelBlueDark,
    tertiary = MintGreen,
    onTertiary = PureWhite,
    tertiaryContainer = MintGreenLight,
    onTertiaryContainer = MintGreenDark,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantWarm,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = LavenderBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkLavenderPrimary,
    onPrimary = DarkBackground,
    primaryContainer = LavenderDark,
    onPrimaryContainer = DarkLavenderPrimary,
    secondary = DarkPastelBlue,
    onSecondary = DarkBackground,
    secondaryContainer = PastelBlueDark,
    onSecondaryContainer = DarkPastelBlue,
    tertiary = DarkMintGreen,
    onTertiary = DarkBackground,
    tertiaryContainer = MintGreenDark,
    onTertiaryContainer = DarkMintGreen,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

@Composable
fun StudyBloomTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    dynamicColor: Boolean = false, // Keep branded palette by default
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

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
        shapes = StudyBloomShapes,
        typography = Typography,
        content = content
    )
}
