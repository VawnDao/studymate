package com.studymate.domain.model

/** Bộ ôn tập AI sinh ra từ một tài liệu. Mọi mục đều trỏ về đoạn nguồn (`sourceChunkIds`). */
data class StudyPack(
    val documentId: String,
    val title: String,
    val pages: Int,
    val sourceType: SourceType,
    val summary: Summary,
    val flashcards: List<FlashcardContent>,
    val quiz: List<QuizItem>,
    val chunkPages: Map<String, Int>,
    val subject: String = "",
) {
    fun pageOf(chunkId: String): Int? = chunkPages[chunkId]

    /** Trang của nguồn đầu tiên, dùng cho chip "Nguồn · tr. N". */
    fun firstSourceOf(chunkIds: List<String>): SourceRef? =
        chunkIds.firstNotNullOfOrNull { id -> pageOf(id)?.let { SourceRef(id, it) } }
}

data class SourceRef(val chunkId: String, val page: Int)

data class Summary(
    val tldr: String,
    val outline: List<OutlineSection>,
    val concepts: List<Concept>,
)

data class OutlineSection(
    val title: String,
    val points: List<String>,
    val sourceChunkIds: List<String>,
)

data class Concept(
    val term: String,
    val explain: String,
    val sourceChunkIds: List<String>,
)

data class FlashcardContent(
    val id: String,
    val front: String,
    val back: String,
    val sourceChunkIds: List<String>,
)

data class QuizItem(
    val id: String,
    val question: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val sourceChunkIds: List<String>,
) {
    fun isCorrect(optionIndex: Int): Boolean = optionIndex == answerIndex
}
