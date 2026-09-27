package com.ayush.aspect.core.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AspectDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF35265F),
    primaryContainer = Color(0xFF4A3D70),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    background = Color(0xFF09090D),
    onBackground = Color(0xFFF2EFF5),
    surface = Color(0xFF0B0A0F),
    onSurface = Color(0xFFF2EFF5),
    surfaceContainer = Color(0xFF14121A),
    surfaceContainerHigh = Color(0xFF1C1922),
    surfaceVariant = Color(0xFF24212A),
    onSurfaceVariant = Color(0xFFC9C2D0),
    outline = Color(0xFF77717E)
)

private val AspectLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    background = Color(0xFFFAF8FC),
    surface = Color(0xFFFAF8FC),
    surfaceContainer = Color(0xFFF0ECF4),
    surfaceContainerHigh = Color(0xFFE9E3EE)
)

private val AspectTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.7).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.35).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp)
    )
}

@Composable
fun AspectTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context is Activity && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> AspectDarkColors
        else -> AspectLightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = AspectTypography, content = content)
}
