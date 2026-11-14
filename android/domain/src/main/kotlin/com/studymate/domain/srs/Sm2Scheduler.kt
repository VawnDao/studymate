package com.studymate.domain.srs

import com.studymate.domain.model.ReviewRating
import com.studymate.domain.model.ReviewSchedule
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Lịch ôn lặp lại ngắt quãng theo SM-2 rút gọn.
 *
 * - Quên: ôn lại sau 10 phút, đặt lại chuỗi nhớ.
 * - Khó / Nhớ: 1 ngày → 3 ngày → khoảng trước × hệ số dễ.
 * - Dễ: như Nhớ nhưng giãn thêm ít nhất 1 ngày (×1.3).
 */
object Sm2Scheduler {
    val RELEARN_DELAY: Duration = Duration.ofMinutes(10)
    const val MIN_EASE = 1.3
    private const val EASY_BONUS = 1.3

    fun next(current: ReviewSchedule, rating: ReviewRating, now: Instant): ReviewSchedule {
        val quality = rating.quality
        val penalty = 5 - quality
        val ease = max(MIN_EASE, current.ease + 0.1 - penalty * (0.08 + penalty * 0.02))

        if (rating == ReviewRating.AGAIN) {
            return current.copy(
                ease = ease,
                intervalDays = 0,
                reps = 0,
                lapses = current.lapses + 1,
                dueAt = now.plus(RELEARN_DELAY),
            )
        }

        val reps = current.reps + 1
        var interval = when (reps) {
            1 -> 1
            2 -> 3
            else -> max(1, (current.intervalDays * current.ease).roundToInt())
        }
        if (rating == ReviewRating.EASY) {
            interval = max(interval + 1, (interval * EASY_BONUS).roundToInt())
        }
        return ReviewSchedule(
            ease = ease,
            intervalDays = interval,
            reps = reps,
            lapses = current.lapses,
            dueAt = now.plus(Duration.ofDays(interval.toLong())),
        )
    }

    /** Khoảng thời gian tới lần ôn kế tiếp cho từng lựa chọn, để hiện dưới các nút đánh giá. */
    fun preview(current: ReviewSchedule, now: Instant): Map<ReviewRating, Duration> =
        ReviewRating.entries.associateWith { rating ->
            Duration.between(now, next(current, rating, now).dueAt)
        }
}
