package com.studymate.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studymate.app.R

/** Bảng màu của app (khớp prototype và FE.md mục 10). Mọi cặp chữ/nền đạt tương phản ≥ 4.5:1. */
@Immutable
data class StudyColors(
    val background: Color = Color(0xFFF3F4F8),
    val surface: Color = Color(0xFFFFFFFF),
    val surfaceMuted: Color = Color(0xFFF7F7FA),
    val ink: Color = Color(0xFF15161C),
    val inkSecondary: Color = Color(0xFF4A4E5C),
    val inkMuted: Color = Color(0xFF626777),
    val divider: Color = Color(0xFFE3E5EC),
    val primary: Color = Color(0xFF5B47F5),
    val primaryStrong: Color = Color(0xFF4733D6),
    val primaryTint: Color = Color(0xFFECE9FF),
    val mint: Color = Color(0xFFDDF3E6),
    val peach: Color = Color(0xFFFFE6D6),
    val sky: Color = Color(0xFFDDEBFF),
    val lemon: Color = Color(0xFFFFF3C4),
    val onLemon: Color = Color(0xFF4A3B00),
    val success: Color = Color(0xFF17804F),
    val error: Color = Color(0xFFD64545),
    val errorText: Color = Color(0xFFB23434),
    val errorTint: Color = Color(0xFFFDE3E3),
    val warningText: Color = Color(0xFF8A4E00),
    val warningTint: Color = Color(0xFFFFF0D6),
    val orangeText: Color = Color(0xFFA14A00),
    val orange: Color = Color(0xFFF07A2B),
    val blueText: Color = Color(0xFF1F56B8),
    val highlight: Color = Color(0xFFFFE58A),
    val streak: Color = Color(0xFFA14A00),
)

private val LocalStudyColors = staticCompositionLocalOf { StudyColors() }

private val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
    Font(R.font.be_vietnam_pro_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = BeVietnamPro,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

private val StudyTypography = Typography(
    displayLarge = style(56, FontWeight.ExtraBold, 60, -2f),
    displayMedium = style(44, FontWeight.ExtraBold, 50, -1f),
    headlineLarge = style(28, FontWeight.ExtraBold, 34, -0.5f),
    headlineMedium = style(26, FontWeight.ExtraBold, 32, -0.5f),
    headlineSmall = style(23, FontWeight.ExtraBold, 30, -0.3f),
    titleLarge = style(20, FontWeight.ExtraBold, 26),
    titleMedium = style(17, FontWeight.Bold, 24),
    titleSmall = style(15, FontWeight.Bold, 21),
    bodyLarge = style(16, FontWeight.Normal, 25),
    bodyMedium = style(15, FontWeight.Normal, 22),
    bodySmall = style(13, FontWeight.Normal, 19),
    labelLarge = style(16, FontWeight.Bold, 22),
    labelMedium = style(13, FontWeight.SemiBold, 18),
    labelSmall = style(11, FontWeight.Bold, 14, 0.4f),
)

private val StudyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun StudyMateTheme(content: @Composable () -> Unit) {
    val colors = StudyColors()
    val scheme = lightColorScheme(
        primary = colors.primary,
        onPrimary = Color.White,
        primaryContainer = colors.primaryTint,
        onPrimaryContainer = colors.primaryStrong,
        secondary = colors.ink,
        onSecondary = Color.White,
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        surfaceVariant = colors.surfaceMuted,
        onSurfaceVariant = colors.inkSecondary,
        surfaceContainerLow = colors.surface,
        error = colors.error,
        onError = Color.White,
        outline = colors.divider,
        outlineVariant = colors.divider,
    )
    CompositionLocalProvider(LocalStudyColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = StudyTypography,
            shapes = StudyShapes,
            content = content,
        )
    }
}

object StudyTheme {
    val colors: StudyColors
        @Composable @ReadOnlyComposable get() = LocalStudyColors.current
}

/** Thời lượng và easing dùng chung (FE.md mục 10.1). */
object Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val Overshoot = CubicBezierEasing(0.2f, 1.4f, 0.4f, 1f)
    const val SHORT = 160
    const val MEDIUM = 320
    const val LONG = 450
    const val STAGGER_MS = 60L
}
