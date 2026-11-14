package com.studymate.domain.usecase

import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode
import com.studymate.domain.common.TimeProvider
import com.studymate.domain.model.Flashcard
import com.studymate.domain.model.HintLevel
import com.studymate.domain.model.QuizAttempt
import com.studymate.domain.model.QuizItem
import com.studymate.domain.model.QuizResult
import com.studymate.domain.model.ReviewRating
import com.studymate.domain.model.ReviewSession
import com.studymate.domain.model.TutorMessage
import com.studymate.domain.model.TutorReply
import com.studymate.domain.model.TutorRole
import com.studymate.domain.repository.StudyRepository
import com.studymate.domain.repository.TutorRepository
import com.studymate.domain.srs.Sm2Scheduler
import java.time.Duration
import javax.inject.Inject

class GetReviewSessionUseCase @Inject constructor(
    private val study: StudyRepository,
    private val time: TimeProvider,
) {
    /**
     * Lấy các thẻ đến hạn. Nếu một tài liệu cụ thể không còn thẻ đến hạn, trả toàn bộ thẻ
     * của tài liệu đó ở chế độ luyện tập để người dùng vẫn ôn được.
     */
    suspend operator fun invoke(documentId: String?): ReviewSession {
        val due = study.getDueCards(documentId, time.now(), SESSION_LIMIT)
        if (due.isNotEmpty() || documentId == null) return ReviewSession(due, isPractice = false)
        return ReviewSession(study.getCards(documentId).take(SESSION_LIMIT), isPractice = true)
    }

    companion object {
        const val SESSION_LIMIT = 50
    }
}

class ReviewCardUseCase @Inject constructor(
    private val study: StudyRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(card: Flashcard, rating: ReviewRating): Flashcard {
        val next = Sm2Scheduler.next(card.schedule, rating, time.now())
        study.updateSchedule(card.id, next)
        return card.copy(schedule = next)
    }

    fun previews(card: Flashcard): Map<ReviewRating, Duration> = Sm2Scheduler.preview(card.schedule, time.now())
}

class SubmitQuizUseCase @Inject constructor(
    private val study: StudyRepository,
    private val time: TimeProvider,
) {
    /** Chấm điểm theo đáp án đã chọn (`itemId -> optionIndex`) và lưu lượt làm bài. */
    suspend operator fun invoke(documentId: String, items: List<QuizItem>, answers: Map<String, Int>): QuizResult {
        val wrong = items.filter { item -> answers[item.id]?.let(item::isCorrect) != true }.map { it.id }
        val result = QuizResult(score = items.size - wrong.size, total = items.size, wrongItemIds = wrong)
        study.saveQuizAttempt(
            QuizAttempt(
                documentId = documentId,
                score = result.score,
                total = result.total,
                wrongItemIds = wrong,
                createdAt = time.now(),
            ),
        )
        return result
    }
}

class AskTutorUseCase @Inject constructor(
    private val tutor: TutorRepository,
) {
    /** Cắt lịch sử còn [MAX_MESSAGES] tin gần nhất, mỗi tin tối đa [MAX_CHARS] ký tự (giới hạn của API). */
    suspend operator fun invoke(documentId: String, history: List<TutorMessage>, hintLevel: Int): AppResult<TutorReply> {
        val messages = history
            .filter { it.content.isNotBlank() }
            .takeLast(MAX_MESSAGES)
            .map { it.copy(content = it.content.trim().take(MAX_CHARS)) }
        if (messages.lastOrNull()?.role != TutorRole.USER) {
            return AppResult.Failure(DomainError.Validation(ErrorCode.EMPTY_INPUT))
        }
        return tutor.ask(documentId, messages, hintLevel.coerceIn(HintLevel.MIN, HintLevel.MAX))
    }

    companion object {
        const val MAX_MESSAGES = 12
        const val MAX_CHARS = 1_000
    }
}
