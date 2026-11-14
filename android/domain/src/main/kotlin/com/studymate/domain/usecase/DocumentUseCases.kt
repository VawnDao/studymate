package com.studymate.domain.usecase

import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode
import com.studymate.domain.common.TimeProvider
import com.studymate.domain.common.ticks
import com.studymate.domain.model.DocumentHub
import com.studymate.domain.model.DocumentInput
import com.studymate.domain.model.DocumentWithDue
import com.studymate.domain.model.ProcessingState
import com.studymate.domain.model.ProcessingStatus
import com.studymate.domain.model.SourceChunk
import com.studymate.domain.model.StudyPack
import com.studymate.domain.model.TodayOverview
import com.studymate.domain.repository.DocumentRepository
import com.studymate.domain.repository.StudyRepository
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ImportDocumentUseCase @Inject constructor(
    private val documents: DocumentRepository,
) {
    suspend operator fun invoke(input: DocumentInput): AppResult<ProcessingState> {
        val invalid = when (input) {
            is DocumentInput.Pdf -> if (input.uri.isBlank()) ErrorCode.EMPTY_INPUT else null
            is DocumentInput.Images -> when {
                input.uris.isEmpty() -> ErrorCode.EMPTY_INPUT
                input.uris.size > MAX_IMAGES -> ErrorCode.TOO_MANY_IMAGES
                else -> null
            }
        }
        if (invalid != null) return AppResult.Failure(DomainError.Validation(invalid))
        return documents.upload(input)
    }

    companion object {
        const val MAX_IMAGES = 5
    }
}

/**
 * Theo dõi tiến độ xử lý bằng cách hỏi trạng thái định kỳ. Khi tài liệu `READY`,
 * đồng bộ bộ ôn tập về máy rồi mới phát trạng thái cuối, để màn kế tiếp có dữ liệu ngay.
 * Bỏ qua lỗi mạng lẻ tẻ; chỉ báo lỗi khi thất bại liên tiếp [maxConsecutiveErrors] lần.
 */
class ObserveProcessingUseCase @Inject constructor(
    private val documents: DocumentRepository,
) {
    operator fun invoke(
        documentId: String,
        pollInterval: Duration = 1.seconds,
        maxConsecutiveErrors: Int = 5,
    ): Flow<AppResult<ProcessingState>> = flow {
        var consecutiveErrors = 0
        while (true) {
            when (val result = documents.fetchStatus(documentId)) {
                is AppResult.Success -> {
                    consecutiveErrors = 0
                    val state = result.value
                    when (state.status) {
                        ProcessingStatus.READY -> {
                            when (val sync = documents.syncStudyPack(documentId)) {
                                is AppResult.Success -> emit(result)
                                is AppResult.Failure -> emit(sync)
                            }
                            return@flow
                        }
                        ProcessingStatus.FAILED -> {
                            emit(result)
                            return@flow
                        }
                        else -> emit(result)
                    }
                }
                is AppResult.Failure -> {
                    consecutiveErrors++
                    if (consecutiveErrors >= maxConsecutiveErrors) {
                        emit(result)
                        return@flow
                    }
                }
            }
            delay(pollInterval)
        }
    }
}

class SyncStudyPackUseCase @Inject constructor(
    private val documents: DocumentRepository,
) {
    suspend operator fun invoke(documentId: String): AppResult<StudyPack> = documents.syncStudyPack(documentId)
}

class ObserveStudyPackUseCase @Inject constructor(
    private val documents: DocumentRepository,
) {
    operator fun invoke(documentId: String): Flow<StudyPack?> = documents.observeStudyPack(documentId)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDocumentHubUseCase @Inject constructor(
    private val documents: DocumentRepository,
    private val study: StudyRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(documentId: String, refreshEvery: Duration = 1.minutes): Flow<DocumentHub?> =
        combine(
            documents.observeStudyPack(documentId),
            time.ticks(refreshEvery).flatMapLatest { now -> study.observeDueCounts(now) },
        ) { pack, due ->
            pack?.let { DocumentHub(pack = it, dueCount = due[documentId] ?: 0) }
        }.distinctUntilChanged()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveLibraryUseCase @Inject constructor(
    private val documents: DocumentRepository,
    private val study: StudyRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(refreshEvery: Duration = 1.minutes): Flow<List<DocumentWithDue>> =
        combine(
            documents.observeDocuments(),
            time.ticks(refreshEvery).flatMapLatest { now -> study.observeDueCounts(now) },
        ) { docs, due ->
            docs.map { DocumentWithDue(it, due[it.id] ?: 0) }
        }.distinctUntilChanged()
}

class ObserveTodayUseCase @Inject constructor(
    private val library: ObserveLibraryUseCase,
) {
    operator fun invoke(refreshEvery: Duration = 1.minutes): Flow<TodayOverview> =
        library(refreshEvery).map { items ->
            TodayOverview(
                dueTotal = items.sumOf { it.dueCount },
                focus = items.filter { it.dueCount > 0 }.maxByOrNull { it.dueCount },
                recent = items.take(RECENT_LIMIT),
                documentCount = items.size,
            )
        }

    companion object {
        const val RECENT_LIMIT = 5
    }
}

class GetSourceChunkUseCase @Inject constructor(
    private val documents: DocumentRepository,
) {
    suspend operator fun invoke(documentId: String, chunkId: String): AppResult<SourceChunk> =
        documents.getSourceChunk(documentId, chunkId)
}

class DeleteDocumentUseCase @Inject constructor(
    private val documents: DocumentRepository,
) {
    suspend operator fun invoke(documentId: String): AppResult<Unit> = documents.delete(documentId)
}
