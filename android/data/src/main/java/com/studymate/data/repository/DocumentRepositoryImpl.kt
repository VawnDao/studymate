package com.studymate.data.repository

import androidx.room.withTransaction
import com.studymate.data.di.IoDispatcher
import com.studymate.data.file.FilePayload
import com.studymate.data.file.LocalFileException
import com.studymate.data.file.LocalFileSource
import com.studymate.data.local.AppDatabase
import com.studymate.data.local.entity.DocumentEntity
import com.studymate.data.local.mapper.cardEntityId
import com.studymate.data.local.mapper.toDomain
import com.studymate.data.local.mapper.toNewEntity
import com.studymate.data.remote.StudyMateApi
import com.studymate.data.remote.apiCall
import com.studymate.data.remote.dto.DocumentStatusDto
import com.studymate.data.remote.dto.StudyPackDto
import com.studymate.data.remote.mapper.toDomain
import com.studymate.data.remote.mapper.toProcessingState
import com.studymate.data.remote.mapper.toWire
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode
import com.studymate.domain.common.TimeProvider
import com.studymate.domain.common.map
import com.studymate.domain.model.Document
import com.studymate.domain.model.DocumentInput
import com.studymate.domain.model.ProcessingState
import com.studymate.domain.model.ProcessingStatus
import com.studymate.domain.model.SourceChunk
import com.studymate.domain.model.SourceType
import com.studymate.domain.model.StudyPack
import com.studymate.domain.repository.DocumentRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
internal class DocumentRepositoryImpl @Inject constructor(
    private val api: StudyMateApi,
    private val database: AppDatabase,
    private val files: LocalFileSource,
    private val json: Json,
    private val time: TimeProvider,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DocumentRepository {

    private val documentDao get() = database.documentDao()
    private val cardDao get() = database.cardDao()

    /** Đoạn nguồn không đổi sau khi xử lý xong, nên giữ trong bộ nhớ cho lần mở sau. */
    private val chunkCache = ConcurrentHashMap<String, SourceChunk>()

    override suspend fun upload(input: DocumentInput): AppResult<ProcessingState> {
        val payloads = try {
            when (input) {
                is DocumentInput.Pdf -> listOf(files.readPdf(input.uri))
                is DocumentInput.Images -> files.readImages(input.uris)
            }
        } catch (e: LocalFileException) {
            return AppResult.Failure(DomainError.Validation(e.code))
        } catch (e: IOException) {
            return AppResult.Failure(DomainError.Validation(ErrorCode.UNSUPPORTED_TYPE))
        } catch (e: SecurityException) {
            return AppResult.Failure(DomainError.Validation(ErrorCode.UNSUPPORTED_TYPE))
        }

        val sourceType = if (input is DocumentInput.Images) SourceType.IMAGES else SourceType.PDF
        val title = input.title?.trim()?.takeIf { it.isNotEmpty() } ?: defaultTitle(input)
        val parts = buildParts(sourceType, payloads, title)

        return apiCall(json) { api.uploadDocument(parts) }.map { dto ->
            saveUploaded(dto, title, sourceType, payloads.sumOf { it.pageCount })
            dto.toProcessingState()
        }
    }

    override suspend fun fetchStatus(documentId: String): AppResult<ProcessingState> =
        apiCall(json) { api.getDocumentStatus(documentId) }.map { dto ->
            documentDao.updateStatus(documentId, dto.status.lowercase(), time.now().toEpochMilli())
            dto.toProcessingState()
        }

    override suspend fun syncStudyPack(documentId: String): AppResult<StudyPack> =
        apiCall(json) { api.getStudyPack(documentId) }.map { dto ->
            savePack(documentId, dto)
            dto.toDomain().copy(documentId = documentId)
        }

    override fun observeStudyPack(documentId: String): Flow<StudyPack?> =
        documentDao.observeById(documentId)
            .map { it?.packJson }
            .distinctUntilChanged()
            .map { packJson ->
                packJson?.let { decodePack(it)?.toDomain()?.copy(documentId = documentId) }
            }
            .flowOn(io)

    override fun observeDocuments(): Flow<List<Document>> =
        documentDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getSourceChunk(documentId: String, chunkId: String): AppResult<SourceChunk> {
        val key = "$documentId/$chunkId"
        chunkCache[key]?.let { return AppResult.Success(it) }
        return apiCall(json) { api.getChunk(documentId, chunkId) }.map { dto ->
            dto.toDomain().also { chunkCache[key] = it }
        }
    }

    override suspend fun delete(documentId: String): AppResult<Unit> {
        val remote = apiCall(json) { api.deleteDocument(documentId) }
        val alreadyGone = remote is AppResult.Failure &&
            (remote.error as? DomainError.Api)?.code == ErrorCode.NOT_FOUND
        if (remote is AppResult.Failure && !alreadyGone) return remote
        documentDao.deleteById(documentId) // thẻ và lượt quiz xoá theo khoá ngoại CASCADE
        chunkCache.keys.removeAll { it.startsWith("$documentId/") }
        return AppResult.Success(Unit)
    }

    private suspend fun defaultTitle(input: DocumentInput): String = when (input) {
        is DocumentInput.Pdf -> files.displayName(input.uri) ?: DEFAULT_TITLE
        is DocumentInput.Images -> DEFAULT_TITLE
    }

    private fun buildParts(sourceType: SourceType, payloads: List<FilePayload>, title: String): List<MultipartBody.Part> {
        val fieldName = if (sourceType == SourceType.PDF) FIELD_FILE else FIELD_IMAGES
        return payloads.map { payload ->
            MultipartBody.Part.createFormData(
                fieldName,
                payload.fileName,
                payload.bytes.toRequestBody(payload.mimeType.toMediaType()),
            )
        } + MultipartBody.Part.createFormData(FIELD_TITLE, title)
    }

    private suspend fun saveUploaded(dto: DocumentStatusDto, title: String, sourceType: SourceType, pages: Int) {
        val now = time.now().toEpochMilli()
        val existing = documentDao.getById(dto.id)
        documentDao.upsert(
            DocumentEntity(
                id = dto.id,
                title = dto.title?.takeIf { it.isNotBlank() } ?: existing?.title ?: title,
                pages = dto.pages.takeIf { it > 0 } ?: pages,
                sourceType = sourceType.toWire(),
                status = dto.status.lowercase(),
                packJson = existing?.packJson,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    private suspend fun savePack(documentId: String, dto: StudyPackDto) {
        val now = time.now()
        val pack = dto.toDomain()
        database.withTransaction {
            val existing = documentDao.getById(documentId)
            documentDao.upsert(
                DocumentEntity(
                    id = documentId,
                    title = pack.title.ifBlank { existing?.title ?: DEFAULT_TITLE },
                    pages = pack.pages.takeIf { it > 0 } ?: existing?.pages ?: 0,
                    sourceType = pack.sourceType.toWire(),
                    status = ProcessingStatus.READY.name.lowercase(),
                    packJson = json.encodeToString(StudyPackDto.serializer(), dto),
                    createdAt = existing?.createdAt ?: now.toEpochMilli(),
                    updatedAt = now.toEpochMilli(),
                ),
            )
            cardDao.insertIgnoringExisting(pack.flashcards.map { it.toNewEntity(documentId, now) })
            pack.flashcards.forEach { card ->
                cardDao.updateContent(cardEntityId(documentId, card.id), card.front, card.back, card.sourceChunkIds)
            }
        }
    }

    private fun decodePack(packJson: String): StudyPackDto? =
        runCatching { json.decodeFromString(StudyPackDto.serializer(), packJson) }.getOrNull()

    private companion object {
        const val FIELD_FILE = "file"
        const val FIELD_IMAGES = "images"
        const val FIELD_TITLE = "title"
        const val DEFAULT_TITLE = "Tài liệu mới"
    }
}
