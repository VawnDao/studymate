package com.studymate.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.studymate.app.R
import java.time.Duration

/** "< 10 phút", "3 giờ", "1 ngày"… cho nhãn dưới nút đánh giá thẻ. */
@Composable
fun formatInterval(duration: Duration): String {
    val minutes = duration.toMinutes().coerceAtLeast(1)
    return when {
        minutes < 60 -> pluralStringResource(R.plurals.interval_minutes, minutes.toInt(), minutes.toInt())
        minutes < 60 * 24 -> (minutes / 60).toInt().let { pluralStringResource(R.plurals.interval_hours, it, it) }
        else -> duration.toDays().toInt().let { pluralStringResource(R.plurals.interval_days, it, it) }
    }
}
