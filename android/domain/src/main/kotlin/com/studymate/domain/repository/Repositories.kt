package com.studymate.domain.repository

import com.studymate.domain.common.AppResult
import com.studymate.domain.model.Document
import com.studymate.domain.model.DocumentInput
import com.studymate.domain.model.Flashcard
import com.studymate.domain.model.ProcessingState
import com.studymate.domain.model.QuizAttempt
import com.studymate.domain.model.ReviewSchedule
import com.studymate.domain.model.SourceChunk
import com.studymate.domain.model.StudyPack
import com.studymate.domain.model.TutorMessage
import com.studymate.domain.model.TutorReply
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    /** Đọc file trên máy, kiểm tra giới hạn, tải lên backend và lưu bản ghi tài liệu. */
    suspend fun upload(input: DocumentInput): AppResult<ProcessingState>

    suspend fun fetchStatus(documentId: String): AppResult<ProcessingState>

    /** Tải bộ ôn tập từ backend, lưu lại để học offline và tạo thẻ mới (giữ lịch ôn thẻ cũ). */
    suspend fun syncStudyPack(documentId: String): AppResult<StudyPack>

    fun observeStudyPack(documentId: String): Flow<StudyPack?>

    fun observeDocuments(): Flow<List<Document>>

    suspend fun getSourceChunk(documentId: String, chunkId: String): AppResult<SourceChunk>

    suspend fun delete(documentId: String): AppResult<Unit>
}

interface StudyRepository {
    /** Số thẻ đến hạn theo từng tài liệu tại thời điểm [now]. */
    fun observeDueCounts(now: Instant): Flow<Map<String, Int>>

    suspend fun getDueCards(documentId: String?, now: Instant, limit: Int): List<Flashcard>

    suspend fun getCards(documentId: String): List<Flashcard>

    suspend fun updateSchedule(cardId: String, schedule: ReviewSchedule)

    suspend fun saveQuizAttempt(attempt: QuizAttempt)
}

interface TutorRepository {
    suspend fun ask(documentId: String, messages: List<TutorMessage>, hintLevel: Int): AppResult<TutorReply>
}
