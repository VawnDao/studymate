package com.studymate.domain.common

sealed interface DomainError {
    /** Không có kết nối hoặc kết nối bị ngắt. */
    data object Network : DomainError

    /** Máy chủ phản hồi quá lâu. */
    data object Timeout : DomainError

    /** Máy chủ trả lỗi theo hợp đồng API (`{"error":{"code","message"}}`). */
    data class Api(val code: ErrorCode, val message: String? = null) : DomainError

    /** Dữ liệu đầu vào không hợp lệ, phát hiện ngay trên máy trước khi gọi API. */
    data class Validation(val code: ErrorCode) : DomainError

    /** Lỗi không lường trước (ví dụ phản hồi sai định dạng). */
    data class Unknown(val cause: Throwable? = null) : DomainError
}

/** Mã lỗi dùng chung giữa app và backend (xem `api/openapi.yaml`). */
enum class ErrorCode {
    UNAUTHORIZED,
    RATE_LIMITED,
    FILE_TOO_LARGE,
    PDF_TOO_LONG,
    UNSUPPORTED_TYPE,
    TOO_MANY_IMAGES,
    NOT_READY,
    NOT_FOUND,
    LLM_UNAVAILABLE,

    /** Backend đã khai báo endpoint nhưng chưa triển khai (HTTP 501). */
    NOT_IMPLEMENTED,
    INTERNAL,
    EMPTY_INPUT,
    UNKNOWN;

    companion object {
        fun fromWire(value: String?): ErrorCode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}
