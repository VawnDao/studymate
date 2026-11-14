package com.studymate.domain.model

import java.time.Instant

/** Thẻ đã lưu trên máy, kèm lịch ôn lặp lại ngắt quãng. */
data class Flashcard(
    val id: String,
    val documentId: String,
    val front: String,
    val back: String,
    val sourceChunkIds: List<String>,
    val schedule: ReviewSchedule,
)

data class ReviewSchedule(
    val ease: Double = DEFAULT_EASE,
    val intervalDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val dueAt: Instant,
) {
    companion object {
        const val DEFAULT_EASE = 2.5
        fun newCard(now: Instant) = ReviewSchedule(dueAt = now)
    }
}

/** Mức tự đánh giá sau khi lật thẻ, theo thang chất lượng của SM-2. */
enum class ReviewRating(val quality: Int) {
    AGAIN(0),
    HARD(3),
    GOOD(4),
    EASY(5),
}

data class ReviewSession(
    val cards: List<Flashcard>,
    /** `true` khi không còn thẻ đến hạn và người dùng đang ôn toàn bộ thẻ của tài liệu. */
    val isPractice: Boolean,
)

data class QuizAttempt(
    val documentId: String,
    val score: Int,
    val total: Int,
    val wrongItemIds: List<String>,
    val createdAt: Instant,
)

data class QuizResult(
    val score: Int,
    val total: Int,
    val wrongItemIds: List<String>,
)
