package com.zhenci.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================
// 深色配色方案 - Dark Color Scheme
// ============================================

private val DarkColorScheme = darkColorScheme(
    primary = DeepBlue,
    onPrimary = White,
    primaryContainer = BrightBlue,
    onPrimaryContainer = White,
    secondary = ProfessionalGreen,
    onSecondary = White,
    secondaryContainer = CalmGreen,
    onSecondaryContainer = White,
    background = DarkGray,
    onBackground = White,
    surface = Color(0xFF1F2937),
    onSurface = White,
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = LightGray,
    error = ZhenciError,
    onError = White,
    outline = MediumGray,
    outlineVariant = PaleGray
)

// ============================================
// 浅色配色方案 - Light Color Scheme (默认)
// ============================================

private val LightColorScheme = lightColorScheme(
    // 主色
    primary = DeepBlue,
    onPrimary = White,
    primaryContainer = BrightBlue,
    onPrimaryContainer = White,

    // 次色
    secondary = ProfessionalGreen,
    onSecondary = White,
    secondaryContainer = CalmGreen,
    onSecondaryContainer = White,

    // 第三色（用于强调）
    tertiary = BrandBlue,
    onTertiary = White,
    tertiaryContainer = BrightBlue,
    onTertiaryContainer = White,

    // 背景
    background = LightGray,
    onBackground = DarkGray,

    // 表面
    surface = White,
    onSurface = DarkGray,
    surfaceVariant = LightGray,
    onSurfaceVariant = MediumGray,

    // 错误
    error = ZhenciError,
    onError = White,

    // 轮廓
    outline = PaleGray,
    outlineVariant = Color(0xFFF3F4F6),

    // 反色表面
    inverseSurface = DarkGray,
    inverseOnSurface = White,
    inversePrimary = BrightBlue
)

@Composable
fun ZhenciTheme(
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏使用主色
            window.statusBarColor = colorScheme.primary.toArgb()
            // 状态栏图标颜色根据主题调整
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
