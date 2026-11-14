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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class AuditStatusColors(
    val launched: Color,
    val launchedContainer: Color,
    val onLaunchedContainer: Color,
    val denied: Color,
    val deniedContainer: Color,
    val onDeniedContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val neutral: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color
)

private val DarkAuditStatusColors = AuditStatusColors(
    launched = StatusLaunchedDark,
    launchedContainer = StatusLaunchedContainerDark,
    onLaunchedContainer = StatusLaunchedOnContainerDark,
    denied = StatusDeniedDark,
    deniedContainer = StatusDeniedContainerDark,
    onDeniedContainer = StatusDeniedOnContainerDark,
    warning = StatusWarningDark,
    warningContainer = StatusWarningContainerDark,
    onWarningContainer = StatusWarningOnContainerDark,
    neutral = StatusNeutralDark,
    neutralContainer = StatusNeutralContainerDark,
    onNeutralContainer = StatusNeutralOnContainerDark
)

private val LightAuditStatusColors = AuditStatusColors(
    launched = StatusLaunchedLight,
    launchedContainer = StatusLaunchedContainerLight,
    onLaunchedContainer = StatusLaunchedOnContainerLight,
    denied = StatusDeniedLight,
    deniedContainer = StatusDeniedContainerLight,
    onDeniedContainer = StatusDeniedOnContainerLight,
    warning = StatusWarningLight,
    warningContainer = StatusWarningContainerLight,
    onWarningContainer = StatusWarningOnContainerLight,
    neutral = StatusNeutralLight,
    neutralContainer = StatusNeutralContainerLight,
    onNeutralContainer = StatusNeutralOnContainerLight
)

val LocalAuditStatusColors = staticCompositionLocalOf { DarkAuditStatusColors }

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = CyanOnPrimary,
    primaryContainer = CyanPrimaryContainer,
    onPrimaryContainer = CyanOnPrimaryContainer,
    secondary = SteelSecondary,
    onSecondary = SteelOnSecondary,
    secondaryContainer = SteelSecondaryContainer,
    onSecondaryContainer = SteelOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = StatusDeniedDark,
    errorContainer = StatusDeniedContainerDark,
    onErrorContainer = StatusDeniedOnContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealPrimaryContainerLight,
    onPrimaryContainer = TealOnPrimaryContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateSecondaryContainerLight,
    onSecondaryContainer = SlateOnSecondaryContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = AmberOnTertiaryLight,
    tertiaryContainer = AmberTertiaryContainerLight,
    onTertiaryContainer = AmberOnTertiaryContainerLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = StatusDeniedLight,
    errorContainer = StatusDeniedContainerLight,
    onErrorContainer = StatusDeniedOnContainerLight
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ActiScanTheme(
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
    val statusColors = if (darkTheme) DarkAuditStatusColors else LightAuditStatusColors

    CompositionLocalProvider(LocalAuditStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
