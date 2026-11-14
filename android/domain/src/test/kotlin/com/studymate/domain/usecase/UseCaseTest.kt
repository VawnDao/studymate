package com.studymate.domain.usecase

import app.cash.turbine.test
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode
import com.studymate.domain.common.TimeProvider
import com.studymate.domain.fakes.FakeDocumentRepository
import com.studymate.domain.fakes.FakeStudyRepository
import com.studymate.domain.fakes.FakeTutorRepository
import com.studymate.domain.fakes.card
import com.studymate.domain.model.DocumentInput
import com.studymate.domain.model.ProcessingState
import com.studymate.domain.model.ProcessingStatus
import com.studymate.domain.model.QuizItem
import com.studymate.domain.model.ReviewRating
import com.studymate.domain.model.TutorMessage
import com.studymate.domain.model.TutorRole
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCaseTest {

    private val now = Instant.parse("2026-11-14T08:00:00Z")
    private val clock = TimeProvider { now }

    @Test
    fun `import rejects more than five images without calling the repository`() = runTest {
        val repo = FakeDocumentRepository()
        val result = ImportDocumentUseCase(repo)(DocumentInput.Images(List(6) { "content://img/$it" }))

        assertEquals(AppResult.Failure(DomainError.Validation(ErrorCode.TOO_MANY_IMAGES)), result)
        assertTrue(repo.uploaded.isEmpty())
    }

    @Test
    fun `processing emits progress, syncs the pack when ready, then completes`() = runTest {
        val repo = FakeDocumentRepository().apply {
            statusQueue += AppResult.Success(ProcessingState("d1", ProcessingStatus.EXTRACTING))
            statusQueue += AppResult.Failure(DomainError.Network) // lỗi lẻ: bỏ qua
            statusQueue += AppResult.Success(ProcessingState("d1", ProcessingStatus.GENERATING))
            statusQueue += AppResult.Success(ProcessingState("d1", ProcessingStatus.READY))
        }

        ObserveProcessingUseCase(repo)("d1", pollInterval = 10.milliseconds).test {
            assertEquals(ProcessingStatus.EXTRACTING, (awaitItem() as AppResult.Success).value.status)
            assertEquals(ProcessingStatus.GENERATING, (awaitItem() as AppResult.Success).value.status)
            assertEquals(ProcessingStatus.READY, (awaitItem() as AppResult.Success).value.status)
            awaitComplete()
        }
        assertEquals(1, repo.syncCalls)
    }

    @Test
    fun `processing gives up after consecutive errors`() = runTest {
        val repo = FakeDocumentRepository().apply {
            repeat(3) { statusQueue += AppResult.Failure(DomainError.Timeout) }
        }

        ObserveProcessingUseCase(repo)("d1", pollInterval = 10.milliseconds, maxConsecutiveErrors = 3).test {
            assertEquals(AppResult.Failure(DomainError.Timeout), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `review session falls back to practice mode when nothing is due`() = runTest {
        val study = FakeStudyRepository().apply {
            cards.value = listOf(card("a", "d1", now.plus(Duration.ofDays(3))))
        }

        val session = GetReviewSessionUseCase(study, clock)("d1")

        assertTrue(session.isPractice)
        assertEquals(listOf("a"), session.cards.map { it.id })
    }

    @Test
    fun `reviewing a card persists the next schedule`() = runTest {
        val study = FakeStudyRepository().apply { cards.value = listOf(card("a", "d1", now)) }

        val updated = ReviewCardUseCase(study, clock)(study.cards.value.first(), ReviewRating.GOOD)

        assertEquals(now.plus(Duration.ofDays(1)), updated.schedule.dueAt)
        assertEquals(updated.schedule, study.cards.value.first().schedule)
    }

    @Test
    fun `quiz is scored and the attempt is saved`() = runTest {
        val study = FakeStudyRepository()
        val items = listOf(
            QuizItem("q1", "?", listOf("a", "b", "c", "d"), answerIndex = 0, explanation = "", sourceChunkIds = emptyList()),
            QuizItem("q2", "?", listOf("a", "b", "c", "d"), answerIndex = 2, explanation = "", sourceChunkIds = emptyList()),
        )

        val result = SubmitQuizUseCase(study, clock)("d1", items, mapOf("q1" to 0, "q2" to 1))

        assertEquals(1, result.score)
        assertEquals(listOf("q2"), result.wrongItemIds)
        assertEquals(1, study.attempts.size)
    }

    @Test
    fun `tutor trims history, clamps hint level and requires a user message last`() = runTest {
        val tutor = FakeTutorRepository()
        val useCase = AskTutorUseCase(tutor)
        val history = List(20) { TutorMessage(if (it % 2 == 0) TutorRole.USER else TutorRole.ASSISTANT, "m$it") } +
            TutorMessage(TutorRole.USER, "x".repeat(5_000))

        useCase("d1", history, hintLevel = 9)

        assertEquals(AskTutorUseCase.MAX_MESSAGES, tutor.lastMessages.size)
        assertEquals(AskTutorUseCase.MAX_CHARS, tutor.lastMessages.last().content.length)
        assertEquals(3, tutor.lastHintLevel)

        val invalid = useCase("d1", listOf(TutorMessage(TutorRole.ASSISTANT, "hi")), 1)
        assertEquals(AppResult.Failure(DomainError.Validation(ErrorCode.EMPTY_INPUT)), invalid)
    }
}
