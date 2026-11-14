package com.studymate.data.file

import com.studymate.domain.common.ErrorCode

/** Nội dung file đã đọc và kiểm tra, sẵn sàng để upload. */
class FilePayload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    /** Số trang của PDF; 0 nếu không đọc được (backend sẽ kiểm tra lại). */
    val pageCount: Int,
)

/** File không hợp lệ theo giới hạn của app (kích thước, định dạng, số trang). */
class LocalFileException(val code: ErrorCode) : Exception(code.name)

interface LocalFileSource {
    /** @throws LocalFileException khi file vượt giới hạn hoặc không phải PDF. */
    suspend fun readPdf(uri: String): FilePayload

    /** Nén mỗi ảnh về JPEG cạnh dài tối đa 1600 px, xoay đúng chiều theo EXIF. */
    suspend fun readImages(uris: List<String>): List<FilePayload>

    /** Tên hiển thị của file (không đuôi), để làm tiêu đề mặc định. */
    suspend fun displayName(uri: String): String?

    companion object {
        const val MAX_UPLOAD_BYTES = 10L * 1024 * 1024
        const val MAX_PDF_PAGES = 20
    }
}
