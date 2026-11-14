package com.studymate.data.file

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.studymate.data.di.IoDispatcher
import com.studymate.domain.common.ErrorCode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class AndroidLocalFileSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val compressor: ImageCompressor,
) : LocalFileSource {

    private val resolver get() = context.contentResolver

    override suspend fun readPdf(uri: String): FilePayload = withContext(io) {
        val parsed = Uri.parse(uri)
        val bytes = readLimited(parsed, LocalFileSource.MAX_UPLOAD_BYTES)
        if (!bytes.startsWith(PDF_MAGIC)) throw LocalFileException(ErrorCode.UNSUPPORTED_TYPE)
        val pages = countPages(parsed)
        if (pages > LocalFileSource.MAX_PDF_PAGES) throw LocalFileException(ErrorCode.PDF_TOO_LONG)
        FilePayload(
            fileName = queryName(parsed) ?: "document.pdf",
            mimeType = MIME_PDF,
            bytes = bytes,
            pageCount = pages,
        )
    }

    override suspend fun readImages(uris: List<String>): List<FilePayload> = withContext(io) {
        val payloads = uris.mapIndexed { index, uri ->
            val parsed = Uri.parse(uri)
            FilePayload(
                fileName = "page-${index + 1}.jpg",
                mimeType = MIME_JPEG,
                bytes = compressor.compress(open = { open(parsed) }),
                pageCount = 1,
            )
        }
        if (payloads.sumOf { it.bytes.size.toLong() } > LocalFileSource.MAX_UPLOAD_BYTES) {
            throw LocalFileException(ErrorCode.FILE_TOO_LARGE)
        }
        payloads
    }

    override suspend fun displayName(uri: String): String? = withContext(io) {
        queryName(Uri.parse(uri))?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
    }

    private fun open(uri: Uri): InputStream =
        resolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())

    private fun readLimited(uri: Uri, maxBytes: Long): ByteArray = open(uri).use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) throw LocalFileException(ErrorCode.FILE_TOO_LARGE)
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    }

    /** PDF có mật khẩu hoặc lỗi cấu trúc: trả 0 và để backend quyết định. */
    private fun countPages(uri: Uri): Int = runCatching {
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { it.pageCount }
        } ?: 0
    }.getOrDefault(0)

    private fun queryName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_JPEG = "image/jpeg"
        val PDF_MAGIC = "%PDF-".toByteArray(Charsets.US_ASCII)
    }
}
