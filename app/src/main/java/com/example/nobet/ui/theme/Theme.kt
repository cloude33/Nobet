package com.example.nobet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MedicalBlueDark,
    onPrimary = Color.White,
    primaryContainer = MedicalBlueDark.copy(alpha = 0.30f),
    onPrimaryContainer = MedicalBlueLight,
    secondary = MedicalTealDark,
    onSecondary = Color.White,
    secondaryContainer = MedicalTealDark.copy(alpha = 0.30f),
    onSecondaryContainer = MedicalTeal,
    tertiary = MedicalGreenDark,
    onTertiary = Color.White,
    tertiaryContainer = MedicalGreenDark.copy(alpha = 0.30f),
    onTertiaryContainer = MedicalGreen,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE1E1E1),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = Color.White,
    outline = Color(0xFF8D8D8D),
    outlineVariant = Color(0xFF666666),
    inverseOnSurface = Color(0xFF1E1E1E),
    inverseSurface = Color(0xFFE1E1E1),
    inversePrimary = MedicalBlueLight,
    scrim = Color(0x66000000)
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalBlue,
    onPrimary = Color.White,
    primaryContainer = MedicalBlueLight.copy(alpha = 0.22f),
    onPrimaryContainer = MedicalBlueDark,
    secondary = MedicalTeal,
    onSecondary = Color.White,
    secondaryContainer = MedicalTeal.copy(alpha = 0.16f),
    onSecondaryContainer = MedicalTealDark,
    tertiary = MedicalGreen,
    onTertiary = Color.White,
    tertiaryContainer = MedicalGreen.copy(alpha = 0.16f),
    onTertiaryContainer = MedicalGreenDark,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1C1C),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = LightGray,
    onSurfaceVariant = NeutralGray,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF8A0A0A),
    outline = NeutralGray.copy(alpha = 0.50f),
    outlineVariant = NeutralGray.copy(alpha = 0.30f),
    inverseOnSurface = Color(0xFFE1E1E1),
    inverseSurface = Color(0xFF1E1E1E),
    inversePrimary = MedicalBlueDark,
    scrim = Color(0x66000000)
)

@Composable
fun NobetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
        content = content
    )
}
