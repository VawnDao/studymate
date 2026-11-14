package com.studymate.domain.fakes

import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.model.Document
import com.studymate.domain.model.DocumentInput
import com.studymate.domain.model.Flashcard
import com.studymate.domain.model.ProcessingState
import com.studymate.domain.model.QuizAttempt
import com.studymate.domain.model.ReviewSchedule
import com.studymate.domain.model.SourceChunk
import com.studymate.domain.model.SourceType
import com.studymate.domain.model.StudyPack
import com.studymate.domain.model.Summary
import com.studymate.domain.model.TutorMessage
import com.studymate.domain.model.TutorReply
import com.studymate.domain.repository.DocumentRepository
import com.studymate.domain.repository.StudyRepository
import com.studymate.domain.repository.TutorRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeDocumentRepository : DocumentRepository {
    /** Lần lượt trả các kết quả này cho mỗi lần gọi [fetchStatus]. */
    val statusQueue = ArrayDeque<AppResult<ProcessingState>>()
    var syncResult: AppResult<StudyPack> = AppResult.Success(samplePack("doc"))
    var uploadResult: AppResult<ProcessingState>? = null
    val uploaded = mutableListOf<DocumentInput>()
    var syncCalls = 0

    override suspend fun upload(input: DocumentInput): AppResult<ProcessingState> {
        uploaded += input
        return uploadResult ?: AppResult.Failure(DomainError.Unknown())
    }

    override suspend fun fetchStatus(documentId: String): AppResult<ProcessingState> =
        statusQueue.removeFirstOrNull() ?: error("statusQueue is empty")

    override suspend fun syncStudyPack(documentId: String): AppResult<StudyPack> {
        syncCalls++
        return syncResult
    }

    override fun observeStudyPack(documentId: String): Flow<StudyPack?> = flowOf(null)

    override fun observeDocuments(): Flow<List<Document>> = flowOf(emptyList())

    override suspend fun getSourceChunk(documentId: String, chunkId: String): AppResult<SourceChunk> =
        AppResult.Success(SourceChunk(chunkId, 1, "text"))

    override suspend fun delete(documentId: String): AppResult<Unit> = AppResult.Success(Unit)
}

class FakeStudyRepository : StudyRepository {
    val cards = MutableStateFlow<List<Flashcard>>(emptyList())
    val attempts = mutableListOf<QuizAttempt>()

    override fun observeDueCounts(now: Instant): Flow<Map<String, Int>> = cards.map { list ->
        list.filter { !it.schedule.dueAt.isAfter(now) }.groupingBy { it.documentId }.eachCount()
    }

    override suspend fun getDueCards(documentId: String?, now: Instant, limit: Int): List<Flashcard> =
        cards.value.filter { (documentId == null || it.documentId == documentId) && !it.schedule.dueAt.isAfter(now) }
            .take(limit)

    override suspend fun getCards(documentId: String): List<Flashcard> = cards.value.filter { it.documentId == documentId }

    override suspend fun updateSchedule(cardId: String, schedule: ReviewSchedule) {
        cards.value = cards.value.map { if (it.id == cardId) it.copy(schedule = schedule) else it }
    }

    override suspend fun saveQuizAttempt(attempt: QuizAttempt) {
        attempts += attempt
    }
}

class FakeTutorRepository : TutorRepository {
    var lastMessages: List<TutorMessage> = emptyList()
    var lastHintLevel: Int = 0
    var result: AppResult<TutorReply> = AppResult.Failure(DomainError.Unknown())

    override suspend fun ask(documentId: String, messages: List<TutorMessage>, hintLevel: Int): AppResult<TutorReply> {
        lastMessages = messages
        lastHintLevel = hintLevel
        return result
    }
}

fun samplePack(documentId: String) = StudyPack(
    documentId = documentId,
    title = "Quang hợp",
    pages = 6,
    sourceType = SourceType.PDF,
    summary = Summary(tldr = "tldr", outline = emptyList(), concepts = emptyList()),
    flashcards = emptyList(),
    quiz = emptyList(),
    chunkPages = emptyMap(),
)

fun card(id: String, documentId: String, dueAt: Instant) = Flashcard(
    id = id,
    documentId = documentId,
    front = "front $id",
    back = "back $id",
    sourceChunkIds = listOf("c001"),
    schedule = ReviewSchedule.newCard(dueAt),
)
