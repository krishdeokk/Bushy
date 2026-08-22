package com.bushy.health.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bushy.health.AvatarType
import com.bushy.health.ThemeMode
import com.bushy.health.VisualStyle

private val MaleColorScheme = lightColorScheme(
    primary = MalePrimary,
    onPrimary = MaleOnPrimary,
    primaryContainer = MalePrimaryContainer,
    onPrimaryContainer = MaleOnPrimaryContainer,
    secondary = MaleSecondary,
    onSecondary = MaleOnSecondary,
    secondaryContainer = MaleSecondaryContainer,
    onSecondaryContainer = MaleOnSecondaryContainer,
    tertiary = MaleTertiary,
    onTertiary = MaleOnTertiary,
    tertiaryContainer = MaleTertiaryContainer,
    onTertiaryContainer = MaleOnTertiaryContainer,
    background = MaleBackground,
    onBackground = MaleOnBackground,
    surface = MaleSurface,
    onSurface = MaleOnSurface,
    surfaceVariant = MaleSurfaceVariant,
    onSurfaceVariant = MaleOnSurfaceVariant,
    outline = MaleOutline
)

private val MaleDarkColorScheme = darkColorScheme(
    primary = MalePrimaryDark,
    onPrimary = MaleOnPrimaryDark,
    primaryContainer = MalePrimaryContainerDark,
    onPrimaryContainer = MaleOnPrimaryContainerDark,
    secondary = MaleSecondaryDark,
    onSecondary = MaleOnSecondaryDark,
    secondaryContainer = MaleSecondaryContainerDark,
    onSecondaryContainer = MaleOnSecondaryContainerDark,
    tertiary = MaleTertiaryDark,
    onTertiary = MaleOnTertiaryDark,
    tertiaryContainer = MaleTertiaryContainerDark,
    onTertiaryContainer = MaleOnTertiaryContainerDark,
    background = MaleBackgroundDark,
    onBackground = MaleOnBackgroundDark,
    surface = MaleSurfaceDark,
    onSurface = MaleOnSurfaceDark,
    surfaceVariant = MaleSurfaceVariantDark,
    onSurfaceVariant = MaleOnSurfaceVariantDark,
    outline = MaleOutlineDark
)

private val FemaleColorScheme = lightColorScheme(
    primary = FemalePrimary,
    onPrimary = FemaleOnPrimary,
    primaryContainer = FemalePrimaryContainer,
    onPrimaryContainer = FemaleOnPrimaryContainer,
    secondary = FemaleSecondary,
    onSecondary = FemaleOnSecondary,
    secondaryContainer = FemaleSecondaryContainer,
    onSecondaryContainer = FemaleOnSecondaryContainer,
    tertiary = FemaleTertiary,
    onTertiary = FemaleOnTertiary,
    tertiaryContainer = FemaleTertiaryContainer,
    onTertiaryContainer = FemaleOnTertiaryContainer,
    background = FemaleBackground,
    onBackground = FemaleOnBackground,
    surface = FemaleSurface,
    onSurface = FemaleOnSurface,
    surfaceVariant = FemaleSurfaceVariant,
    onSurfaceVariant = FemaleOnSurfaceVariant,
    outline = FemaleOutline
)

private val FemaleDarkColorScheme = darkColorScheme(
    primary = FemalePrimaryDark,
    onPrimary = FemaleOnPrimaryDark,
    primaryContainer = FemalePrimaryContainerDark,
    onPrimaryContainer = FemaleOnPrimaryContainerDark,
    secondary = FemaleSecondaryDark,
    onSecondary = FemaleOnSecondaryDark,
    secondaryContainer = FemaleSecondaryContainerDark,
    onSecondaryContainer = FemaleOnSecondaryContainerDark,
    tertiary = FemaleTertiaryDark,
    onTertiary = FemaleOnTertiaryDark,
    tertiaryContainer = FemaleTertiaryContainerDark,
    onTertiaryContainer = FemaleOnTertiaryContainerDark,
    background = FemaleBackgroundDark,
    onBackground = FemaleOnBackgroundDark,
    surface = FemaleSurfaceDark,
    onSurface = FemaleOnSurfaceDark,
    surfaceVariant = FemaleSurfaceVariantDark,
    onSurfaceVariant = FemaleOnSurfaceVariantDark,
    outline = FemaleOutlineDark
)

private val MonoColorScheme = lightColorScheme(
    primary = MonoPrimary,
    onPrimary = MonoOnPrimary,
    primaryContainer = MonoPrimaryContainer,
    onPrimaryContainer = MonoOnPrimaryContainer,
    secondary = MonoSecondary,
    onSecondary = MonoOnSecondary,
    secondaryContainer = MonoSecondaryContainer,
    onSecondaryContainer = MonoOnSecondaryContainer,
    tertiary = MonoTertiary,
    onTertiary = MonoOnTertiary,
    tertiaryContainer = MonoTertiaryContainer,
    onTertiaryContainer = MonoOnTertiaryContainer,
    background = MonoBackground,
    onBackground = MonoOnBackground,
    surface = MonoSurface,
    onSurface = MonoOnSurface,
    surfaceVariant = MonoSurfaceVariant,
    onSurfaceVariant = MonoOnSurfaceVariant,
    outline = MonoOutline,
    surfaceContainerHigh = MonoSecondaryContainer,
    surfaceContainerHighest = MonoPrimaryContainer
)

@Composable
fun BushyTheme(
    avatarType: AvatarType = AvatarType.MALE,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    visualStyle: VisualStyle = VisualStyle.MATERIAL3,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = if (visualStyle == VisualStyle.MONOCHROME) {
        false // Always White/Light as requested
    } else {
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    }

    val colorScheme = when {
        visualStyle == VisualStyle.MONOCHROME -> MonoColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        avatarType == AvatarType.MALE && darkTheme -> MaleDarkColorScheme
        avatarType == AvatarType.MALE && !darkTheme -> MaleColorScheme
        avatarType == AvatarType.FEMALE && darkTheme -> FemaleDarkColorScheme
        else -> FemaleColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
