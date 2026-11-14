package com.studymate.domain.model

import com.studymate.domain.common.ErrorCode
import java.time.Instant

data class Document(
    val id: String,
    val title: String,
    val pages: Int,
    val sourceType: SourceType,
    val status: ProcessingStatus,
    val createdAt: Instant,
    val subject: String = "",
)

enum class SourceType { PDF, IMAGES }

/** Trạng thái xử lý tài liệu phía backend, theo đúng thứ tự pipeline. */
enum class ProcessingStatus {
    UPLOADED,
    EXTRACTING,
    OCR,
    CHUNKING,
    INDEXING,
    GENERATING,
    VALIDATING,
    READY,
    FAILED;

    val isTerminal: Boolean get() = this == READY || this == FAILED

    companion object {
        fun fromWire(value: String?): ProcessingStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UPLOADED
    }
}

data class ProcessingState(
    val documentId: String,
    val status: ProcessingStatus,
    val failure: ErrorCode? = null,
)

/** Tài liệu người dùng chọn để tải lên. `uri` là chuỗi URI nội dung của hệ điều hành. */
sealed interface DocumentInput {
    val title: String?

    data class Pdf(val uri: String, override val title: String? = null) : DocumentInput

    data class Images(val uris: List<String>, override val title: String? = null) : DocumentInput
}

data class SourceChunk(
    val id: String,
    val page: Int,
    val text: String,
)
