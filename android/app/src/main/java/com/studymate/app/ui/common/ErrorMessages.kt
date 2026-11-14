package com.studymate.app.ui.common

import androidx.annotation.StringRes
import com.studymate.app.R
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode

/** Câu tiếng Việt hiển thị cho từng lỗi (FE.md mục 6.3). */
@StringRes
fun DomainError.messageRes(): Int = when (this) {
    DomainError.Network -> R.string.error_network
    DomainError.Timeout -> R.string.error_network
    is DomainError.Api -> code.messageRes()
    is DomainError.Validation -> code.messageRes()
    is DomainError.Unknown -> R.string.error_generic
}

@StringRes
fun ErrorCode.messageRes(): Int = when (this) {
    ErrorCode.FILE_TOO_LARGE -> R.string.error_file_too_large
    ErrorCode.PDF_TOO_LONG -> R.string.error_pdf_too_long
    ErrorCode.UNSUPPORTED_TYPE -> R.string.error_unsupported_type
    ErrorCode.TOO_MANY_IMAGES -> R.string.error_too_many_images
    ErrorCode.NOT_READY -> R.string.error_not_ready
    ErrorCode.NOT_FOUND -> R.string.error_not_found
    ErrorCode.RATE_LIMITED -> R.string.error_rate_limited
    ErrorCode.LLM_UNAVAILABLE -> R.string.error_llm_unavailable
    ErrorCode.EMPTY_INPUT -> R.string.error_empty_input
    ErrorCode.UNAUTHORIZED -> R.string.error_unauthorized
    ErrorCode.NOT_IMPLEMENTED -> R.string.error_not_implemented
    ErrorCode.INTERNAL, ErrorCode.UNKNOWN -> R.string.error_generic
}
