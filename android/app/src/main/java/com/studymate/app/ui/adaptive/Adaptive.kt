package com.studymate.app.ui.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nhóm bề rộng cửa sổ theo Material 3:
 * - COMPACT (< 600dp): điện thoại dọc → thanh điều hướng dưới, 1 cột.
 * - MEDIUM (600–839dp): tablet dọc, điện thoại ngang, máy gập mở → Navigation Rail, nội dung căn giữa.
 * - EXPANDED (≥ 840dp): tablet ngang → Navigation Rail, bố cục 2 cột / lưới.
 */
enum class WidthClass {
    COMPACT, MEDIUM, EXPANDED;

    val isCompact: Boolean get() = this == COMPACT
    val isExpanded: Boolean get() = this == EXPANDED

    companion object {
        fun from(sizeClass: WindowWidthSizeClass): WidthClass = when (sizeClass) {
            WindowWidthSizeClass.Expanded -> EXPANDED
            WindowWidthSizeClass.Medium -> MEDIUM
            else -> COMPACT
        }
    }
}

val LocalWidthClass = staticCompositionLocalOf { WidthClass.COMPACT }

object AdaptiveWidth {
    val current: WidthClass
        @Composable @ReadOnlyComposable get() = LocalWidthClass.current
}

/** Độ rộng tối đa của nội dung để dòng chữ không quá dài trên màn hình lớn. */
object ContentWidth {
    /** Màn tập trung một việc: xử lý, flashcard. */
    val Focus = 680.dp

    /** Màn đọc/làm bài: quiz, gia sư, danh sách 1 cột. */
    val Reading = 760.dp

    /** Màn tổng quan nhiều khối: hub, tóm tắt 2 cột, lưới thư viện. */
    val Wide = 1120.dp
}

/** Căn giữa theo chiều ngang và giới hạn bề rộng tối đa; trên điện thoại không đổi gì. */
fun Modifier.centeredContent(maxWidth: Dp): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)
        .fillMaxWidth()
