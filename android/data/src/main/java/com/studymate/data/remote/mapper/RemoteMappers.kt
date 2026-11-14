package com.studymate.data.remote.mapper

import com.studymate.data.remote.dto.ChunkDto
import com.studymate.data.remote.dto.DocumentStatusDto
import com.studymate.data.remote.dto.PodcastStatusDto
import com.studymate.data.remote.dto.StudyPackDto
import com.studymate.data.remote.dto.TutorMessageDto
import com.studymate.data.remote.dto.TutorReplyDto
import com.studymate.domain.common.ErrorCode
import com.studymate.domain.model.Citation
import com.studymate.domain.model.Concept
import com.studymate.domain.model.FlashcardContent
import com.studymate.domain.model.OutlineSection
import com.studymate.domain.model.ProcessingState
import com.studymate.domain.model.Podcast
import com.studymate.domain.model.PodcastStatus
import com.studymate.domain.model.ProcessingStatus
import com.studymate.domain.model.QuizItem
import com.studymate.domain.model.SourceChunk
import com.studymate.domain.model.SourceType
import com.studymate.domain.model.StudyPack
import com.studymate.domain.model.Summary
import com.studymate.domain.model.TutorMessage
import com.studymate.domain.model.TutorMode
import com.studymate.domain.model.TutorReply
import com.studymate.domain.model.TutorRole

internal fun DocumentStatusDto.toProcessingState() = ProcessingState(
    documentId = id,
    status = ProcessingStatus.fromWire(status),
    failure = error?.code?.let(ErrorCode::fromWire),
)

internal fun sourceTypeFromWire(value: String?): SourceType =
    if (value.equals("images", ignoreCase = true)) SourceType.IMAGES else SourceType.PDF

internal fun SourceType.toWire(): String = when (this) {
    SourceType.PDF -> "pdf"
    SourceType.IMAGES -> "images"
}

/**
 * Chuyển DTO sang domain. Bỏ các câu quiz sai cấu trúc (không đủ 4 đáp án hoặc chỉ số đáp án
 * ngoài khoảng) để UI không bao giờ phải xử lý dữ liệu hỏng.
 */
internal fun StudyPackDto.toDomain() = StudyPack(
    documentId = document.id,
    title = document.title,
    pages = document.pages,
    sourceType = sourceTypeFromWire(document.sourceType),
    summary = Summary(
        tldr = summary.tldr,
        outline = summary.outline.map { OutlineSection(it.title, it.points, it.sourceChunkIds) },
        concepts = summary.concepts.map { Concept(it.term, it.explain, it.sourceChunkIds) },
    ),
    flashcards = flashcards
        .filter { it.front.isNotBlank() && it.back.isNotBlank() }
        .map { FlashcardContent(it.id, it.front, it.back, it.sourceChunkIds) },
    quiz = quiz
        .filter { it.options.size == QUIZ_OPTIONS && it.answerIndex in it.options.indices }
        .map { QuizItem(it.id, it.question, it.options, it.answerIndex, it.explanation, it.sourceChunkIds) },
    chunkPages = chunksIndex.mapValues { it.value.page },
)

internal fun ChunkDto.toDomain() = SourceChunk(id = id, page = page, text = text)

internal fun TutorMessage.toDto() = TutorMessageDto(
    role = when (role) {
        TutorRole.USER -> "user"
        TutorRole.ASSISTANT -> "assistant"
    },
    content = content,
)

internal fun TutorReplyDto.toDomain() = TutorReply(
    reply = reply,
    mode = TutorMode.fromWire(mode),
    nextQuestion = nextQuestion?.takeIf { it.isNotBlank() },
    citations = citations.map { Citation(it.chunkId, it.page, it.quote?.takeIf(String::isNotBlank)) },
    inDocument = inDocument,
)

internal fun PodcastStatusDto.toDomain(documentId: String) = Podcast(
    documentId = documentId,
    title = title,
    script = script,
    audioUrl = audioUrl,
    status = PodcastStatus.fromWire(status),
    aiGenerated = aiGenerated,
)

private const val QUIZ_OPTIONS = 4
