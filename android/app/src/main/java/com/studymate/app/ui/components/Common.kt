package com.studymate.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studymate.app.R
import com.studymate.app.ui.theme.Motion
import com.studymate.app.ui.theme.StudyTheme
import kotlinx.coroutines.delay

@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack,
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.inkMuted, maxLines = 1)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SourceChip(
    page: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = StudyTheme.colors.background,
    content: Color = StudyTheme.colors.inkSecondary,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = modifier.heightIn(min = 32.dp),
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (page != null) stringResource(R.string.source_chip_page, page) else stringResource(R.string.source_chip),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun Tag(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
fun AnimatedProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Int = 8) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600, easing = Motion.Emphasized),
        label = "progress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(StudyTheme.colors.divider),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = StudyTheme.colors.primary)
    }
}

@Composable
fun MessageView(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = StudyTheme.colors.inkSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            StudyButton(text = actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth())
        }
        if (secondaryLabel != null && onSecondary != null) {
            StudyButton(text = secondaryLabel, onClick = onSecondary, style = StudyButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Hiện dần lần lượt theo [index] (trễ 60 ms mỗi phần tử): mờ → rõ, trượt lên 12dp. */
@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * Motion.STAGGER_MS)
        progress.animateTo(1f, tween(Motion.LONG, easing = Motion.Emphasized))
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 12.dp.toPx()
        val scale = 0.98f + 0.02f * p
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .clip(CircleShape)
                    .background(StudyTheme.colors.inkMuted),
            )
        }
    }
}

@Composable
fun SubjectBadge(title: String, modifier: Modifier = Modifier) {
    val palette = listOf(
        StudyTheme.colors.mint to StudyTheme.colors.success,
        StudyTheme.colors.peach to StudyTheme.colors.orangeText,
        StudyTheme.colors.sky to StudyTheme.colors.blueText,
        StudyTheme.colors.primaryTint to StudyTheme.colors.primaryStrong,
    )
    val (bg, fg) = palette[(title.hashCode() and Int.MAX_VALUE) % palette.size]
    val label = title.trim().split(' ', '·', '-').firstOrNull { it.isNotBlank() }?.take(4)?.uppercase() ?: "?"
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
}
