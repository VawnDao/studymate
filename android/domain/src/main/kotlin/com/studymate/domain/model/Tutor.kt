package com.studymate.domain.model

enum class TutorRole { USER, ASSISTANT }

data class TutorMessage(
    val role: TutorRole,
    val content: String,
)

enum class TutorMode {
    /** Gợi ý hướng suy nghĩ, chưa đưa đáp án. */
    HINT,

    /** Nhận xét câu trả lời của học sinh. */
    CHECK,

    /** Lời giải đầy đủ. */
    SOLUTION,

    /** Tài liệu không có thông tin để trả lời. */
    OUT_OF_SCOPE;

    companion object {
        fun fromWire(value: String?): TutorMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HINT
    }
}

data class Citation(
    val chunkId: String,
    val page: Int,
    val quote: String?,
)

data class TutorReply(
    val reply: String,
    val mode: TutorMode,
    val nextQuestion: String?,
    val citations: List<Citation>,
    val inDocument: Boolean,
)

object HintLevel {
    const val MIN = 1
    const val MAX = 3
    const val SOLUTION = MAX
}
