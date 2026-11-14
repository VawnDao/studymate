package com.studymate.data.local.mapper

import com.studymate.data.local.entity.CardEntity
import com.studymate.data.local.entity.DocumentEntity
import com.studymate.data.local.entity.QuizAttemptEntity
import com.studymate.data.remote.mapper.sourceTypeFromWire
import com.studymate.domain.model.Document
import com.studymate.domain.model.Flashcard
import com.studymate.domain.model.FlashcardContent
import com.studymate.domain.model.ProcessingStatus
import com.studymate.domain.model.QuizAttempt
import com.studymate.domain.model.ReviewSchedule
import java.time.Instant

internal fun DocumentEntity.toDomain() = Document(
    id = id,
    title = title,
    pages = pages,
    sourceType = sourceTypeFromWire(sourceType),
    status = ProcessingStatus.fromWire(status),
    createdAt = Instant.ofEpochMilli(createdAt),
    subject = subject,
)

internal fun CardEntity.toDomain() = Flashcard(
    id = id,
    documentId = documentId,
    front = front,
    back = back,
    sourceChunkIds = sourceChunkIds,
    schedule = ReviewSchedule(
        ease = ease,
        intervalDays = intervalDays,
        reps = reps,
        lapses = lapses,
        dueAt = Instant.ofEpochMilli(dueAt),
    ),
)

internal fun cardEntityId(documentId: String, cardId: String) = "$documentId::$cardId"

internal fun FlashcardContent.toNewEntity(documentId: String, now: Instant): CardEntity {
    val schedule = ReviewSchedule.newCard(now)
    return CardEntity(
        id = cardEntityId(documentId, id),
        documentId = documentId,
        front = front,
        back = back,
        sourceChunkIds = sourceChunkIds,
        ease = schedule.ease,
        intervalDays = schedule.intervalDays,
        reps = schedule.reps,
        lapses = schedule.lapses,
        dueAt = schedule.dueAt.toEpochMilli(),
    )
}

internal fun QuizAttempt.toEntity() = QuizAttemptEntity(
    documentId = documentId,
    score = score,
    total = total,
    wrongIds = wrongItemIds,
    createdAt = createdAt.toEpochMilli(),
)
