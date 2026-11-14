package com.studymate.app.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import com.studymate.app.ui.adaptive.AdaptiveWidth
import com.studymate.app.ui.theme.StudyTheme

/**
 * Phím tắt cho màn hình (tablet gắn bàn phím, Chromebook). Màn nhận focus khi mở để phím
 * hoạt động ngay; [onKey] trả `true` nếu đã xử lý phím.
 */
@Composable
fun Modifier.keyboardShortcuts(onKey: (Key) -> Boolean): Modifier {
    val focusRequester = remember { FocusRequester() }
    val currentOnKey by rememberUpdatedState(onKey)
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    return this
        .onPreviewKeyEvent { event -> event.type == KeyEventType.KeyDown && currentOnKey(event.key) }
        .focusRequester(focusRequester)
        .focusable()
}

val Key.isConfirm: Boolean get() = this == Key.Enter || this == Key.NumPadEnter || this == Key.Spacebar

/** Chỉ số 0..3 cho phím 1–4 hoặc A–D (cả bàn phím số). */
fun Key.choiceIndex(): Int? = when (this) {
    Key.One, Key.NumPad1, Key.A -> 0
    Key.Two, Key.NumPad2, Key.B -> 1
    Key.Three, Key.NumPad3, Key.C -> 2
    Key.Four, Key.NumPad4, Key.D -> 3
    else -> null
}

/** Dòng gợi ý phím tắt, chỉ hiện trên màn hình lớn. */
@Composable
fun ShortcutHint(text: String, modifier: Modifier = Modifier) {
    if (AdaptiveWidth.current.isCompact) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = StudyTheme.colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
