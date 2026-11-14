package com.studymate.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorEnvelope(val error: ApiErrorDto)

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String? = null,
)

@Serializable
data class DocumentStatusDto(
    val id: String,
    val title: String? = null,
    val pages: Int = 0,
    @SerialName("source_type") val sourceType: String? = null,
    val status: String,
    val step: Int = 0,
    @SerialName("steps_total") val stepsTotal: Int = 4,
    val error: ApiErrorDto? = null,
    val cached: Boolean = false,
)

@Serializable
data class StudyPackDto(
    val document: PackDocumentDto,
    val summary: SummaryDto,
    val flashcards: List<FlashcardDto> = emptyList(),
    val quiz: List<QuizItemDto> = emptyList(),
    @SerialName("chunks_index") val chunksIndex: Map<String, ChunkRefDto> = emptyMap(),
)

@Serializable
data class PackDocumentDto(
    val id: String,
    val title: String,
    val pages: Int = 0,
    @SerialName("source_type") val sourceType: String = "pdf",
)

@Serializable
data class SummaryDto(
    val tldr: String = "",
    val outline: List<OutlineSectionDto> = emptyList(),
    val concepts: List<ConceptDto> = emptyList(),
)

@Serializable
data class OutlineSectionDto(
    val title: String,
    val points: List<String> = emptyList(),
    @SerialName("source_chunk_ids") val sourceChunkIds: List<String> = emptyList(),
)

@Serializable
data class ConceptDto(
    val term: String,
    val explain: String,
    @SerialName("source_chunk_ids") val sourceChunkIds: List<String> = emptyList(),
)

@Serializable
data class FlashcardDto(
    val id: String,
    val front: String,
    val back: String,
    @SerialName("source_chunk_ids") val sourceChunkIds: List<String> = emptyList(),
)

@Serializable
data class QuizItemDto(
    val id: String,
    val question: String,
    val options: List<String>,
    @SerialName("answer_index") val answerIndex: Int,
    val explanation: String = "",
    @SerialName("source_chunk_ids") val sourceChunkIds: List<String> = emptyList(),
)

@Serializable
data class ChunkRefDto(val page: Int)

@Serializable
data class ChunkDto(
    val id: String,
    val page: Int,
    val text: String,
)

@Serializable
data class TutorRequestDto(
    val messages: List<TutorMessageDto>,
    @SerialName("hint_level") val hintLevel: Int,
)

@Serializable
data class TutorMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class TutorReplyDto(
    val reply: String,
    val mode: String,
    @SerialName("next_question") val nextQuestion: String? = null,
    val citations: List<CitationDto> = emptyList(),
    @SerialName("in_document") val inDocument: Boolean = true,
)

@Serializable
data class CitationDto(
    @SerialName("chunk_id") val chunkId: String,
    val page: Int,
    val quote: String? = null,
)

@Serializable
data class PodcastStatusDto(
    val status: String,
    val title: String = "",
    val script: String = "",
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("ai_generated") val aiGenerated: Boolean = true,
)
