package com.studymate.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studymate.app.ui.theme.Motion
import com.studymate.app.ui.theme.StudyTheme

/** Thu nhỏ còn [pressedScale] khi đang nhấn. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(Motion.SHORT, easing = Motion.Emphasized),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

enum class StudyButtonStyle { Primary, Dark, Light, Outline }

@Composable
fun StudyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: StudyButtonStyle = StudyButtonStyle.Dark,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 56.dp,
) {
    val colors = StudyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val (container, content) = when (style) {
        StudyButtonStyle.Primary -> colors.primary to Color.White
        StudyButtonStyle.Dark -> colors.ink to Color.White
        StudyButtonStyle.Light -> colors.surface to colors.ink
        StudyButtonStyle.Outline -> Color.Transparent to colors.ink
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interaction,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = if (style == StudyButtonStyle.Outline) 0f else 0.5f),
            disabledContentColor = content.copy(alpha = 0.7f),
        ),
        border = if (style == StudyButtonStyle.Outline) BorderStroke(1.5.dp, colors.divider) else null,
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier
            .height(height)
            .pressScale(interaction),
    ) {
        if (loading) {
            CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = StudyTheme.colors.surface,
    tint: Color = StudyTheme.colors.ink,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = tint,
        interactionSource = interaction,
        modifier = modifier
            .size(44.dp)
            .pressScale(interaction),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

/** Thẻ bấm được có hiệu ứng nhấn. */
@Composable
fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = StudyTheme.colors.surface,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = shape,
        color = color,
        contentColor = StudyTheme.colors.ink,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}
