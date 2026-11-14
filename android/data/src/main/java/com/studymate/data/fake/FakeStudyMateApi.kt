package com.studymate.data.fake

import android.content.Context
import com.studymate.data.remote.StudyMateApi
import com.studymate.data.remote.dto.ApiErrorDto
import com.studymate.data.remote.dto.ApiErrorEnvelope
import com.studymate.data.remote.dto.ChunkDto
import com.studymate.data.remote.dto.CitationDto
import com.studymate.data.remote.dto.DocumentStatusDto
import com.studymate.data.remote.dto.StudyPackDto
import com.studymate.data.remote.dto.TutorReplyDto
import com.studymate.data.remote.dto.TutorRequestDto
import com.studymate.domain.common.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.HttpException
import retrofit2.Response

/**
 * Backend giả chạy ngay trong app (bật bằng `studymate.useFakeApi=true`), để FE phát triển
 * và demo khi backend Go chưa sẵn sàng. Tuân thủ đúng hợp đồng API v1: cùng DTO, cùng mã lỗi.
 */
@Singleton
internal class FakeStudyMateApi @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val time: TimeProvider,
) : StudyMateApi {

    private data class FakeDocument(val id: String, val title: String, val sourceType: String, val startedAt: Long)

    private val documents = ConcurrentHashMap<String, FakeDocument>()
    private val counter = AtomicInteger(0)

    private val samplePack: StudyPackDto by lazy {
        json.decodeFromString(StudyPackDto.serializer(), readAsset("fake/pack_sample.json"))
    }
    private val sampleChunks: Map<String, ChunkDto> by lazy {
        json.decodeFromString(ListSerializer(ChunkDto.serializer()), readAsset("fake/chunks_sample.json"))
            .associateBy { it.id }
    }

    override suspend fun uploadDocument(parts: List<MultipartBody.Part>): DocumentStatusDto {
        delay(LATENCY_MS * 2)
        val title = parts.formValue("title") ?: samplePack.document.title
        val sourceType = if (parts.any { it.fieldName() == "images" }) "images" else "pdf"
        val id = "demo-${counter.incrementAndGet()}-${time.now().toEpochMilli()}"
        documents[id] = FakeDocument(id, title, sourceType, time.now().toEpochMilli())
        return DocumentStatusDto(id = id, title = title, status = "uploaded", step = 0, sourceType = sourceType)
    }

    override suspend fun getDocumentStatus(documentId: String): DocumentStatusDto {
        delay(LATENCY_MS)
        val doc = documents[documentId] ?: return DocumentStatusDto(id = documentId, status = "ready", step = 4)
        val elapsedSteps = ((time.now().toEpochMilli() - doc.startedAt) / STEP_DURATION_MS).toInt()
        val (status, step) = PIPELINE.getOrElse(elapsedSteps) { PIPELINE.last() }
        return DocumentStatusDto(
            id = doc.id,
            title = doc.title,
            pages = samplePack.document.pages,
            sourceType = doc.sourceType,
            status = status,
            step = step,
        )
    }

    override suspend fun getStudyPack(documentId: String): StudyPackDto {
        delay(LATENCY_MS)
        val doc = documents[documentId]
        return samplePack.copy(
            document = samplePack.document.copy(
                id = documentId,
                title = doc?.title ?: samplePack.document.title,
                sourceType = doc?.sourceType ?: samplePack.document.sourceType,
            ),
        )
    }

    override suspend fun getChunk(documentId: String, chunkId: String): ChunkDto {
        delay(LATENCY_MS)
        return sampleChunks[chunkId] ?: throw httpError(404, "NOT_FOUND", "Không tìm thấy đoạn nguồn")
    }

    override suspend fun askTutor(documentId: String, body: TutorRequestDto): TutorReplyDto {
        delay(TUTOR_LATENCY_MS)
        val question = body.messages.lastOrNull { it.role == "user" }?.content.orEmpty().lowercase()
        if (OUT_OF_SCOPE_HINTS.any(question::contains)) {
            return TutorReplyDto(
                reply = "Tài liệu của bạn chưa nhắc tới nội dung này, nên mình không trả lời để tránh nói sai. " +
                    "Bạn thử hỏi về pha sáng, pha tối hoặc vai trò của diệp lục nhé.",
                mode = "out_of_scope",
                inDocument = false,
            )
        }
        val citation = CitationDto(chunkId = "c004", page = 3, quote = "ATP và NADPH")
        return when (body.hintLevel) {
            1 -> TutorReplyDto(
                reply = "Gợi ý nhé: hãy nghĩ xem pha tối cần \"nguồn năng lượng\" nào để biến CO₂ thành đường. " +
                    "Pha sáng tạo ra những chất gì?",
                mode = "hint",
                nextQuestion = "Bạn thử kể 2 sản phẩm của pha sáng?",
                citations = listOf(citation),
            )
            2 -> TutorReplyDto(
                reply = "Gợi ý thêm: một chất là \"đồng tiền năng lượng\" của tế bào (3 chữ cái), " +
                    "chất còn lại bắt đầu bằng \"NADP…\".",
                mode = "hint",
                nextQuestion = "Hai chất đó là gì?",
                citations = listOf(citation),
            )
            else -> TutorReplyDto(
                reply = "Lời giải: pha tối (chu trình Calvin) dùng ATP và NADPH do pha sáng tạo ra để biến CO₂ thành đường. " +
                    "Không có ánh sáng thì không có ATP, NADPH nên pha tối cũng dừng.",
                mode = "solution",
                citations = listOf(citation, CitationDto(chunkId = "c005", page = 5, quote = null)),
            )
        }
    }

    override suspend fun deleteDocument(documentId: String): Response<Unit> {
        delay(LATENCY_MS)
        documents.remove(documentId)
        return Response.success(204, Unit)
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun httpError(status: Int, code: String, message: String): HttpException {
        val body = json.encodeToString(ApiErrorEnvelope.serializer(), ApiErrorEnvelope(ApiErrorDto(code, message)))
        return HttpException(Response.error<Any>(status, body.toResponseBody("application/json".toMediaType())))
    }

    private fun MultipartBody.Part.fieldName(): String? =
        headers?.get("Content-Disposition")?.let { NAME_REGEX.find(it)?.groupValues?.get(1) }

    private fun List<MultipartBody.Part>.formValue(name: String): String? =
        firstOrNull { it.fieldName() == name }?.let { part ->
            Buffer().also { part.body.writeTo(it) }.readUtf8()
        }

    private companion object {
        const val LATENCY_MS = 250L
        const val TUTOR_LATENCY_MS = 900L
        const val STEP_DURATION_MS = 900L
        val NAME_REGEX = Regex("name=\"([^\"]+)\"")
        val OUT_OF_SCOPE_HINTS = listOf("ai phát hiện", "ai tìm ra", "năm nào", "lịch sử")
        val PIPELINE = listOf(
            "extracting" to 1,
            "ocr" to 2,
            "chunking" to 3,
            "indexing" to 3,
            "generating" to 4,
            "validating" to 4,
            "ready" to 4,
        )
    }
}
