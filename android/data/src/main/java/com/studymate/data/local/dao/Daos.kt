package com.studymate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.studymate.data.local.entity.CardEntity
import com.studymate.data.local.entity.DocumentEntity
import com.studymate.data.local.entity.DueCountRow
import com.studymate.data.local.entity.QuizAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Upsert
    suspend fun upsert(document: DocumentEntity)

    @Query("UPDATE documents SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CardDao {
    /** Thêm thẻ mới; thẻ đã tồn tại được giữ nguyên (không mất lịch ôn). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(cards: List<CardEntity>)

    @Query("UPDATE cards SET front = :front, back = :back, source_chunk_ids = :sourceChunkIds WHERE id = :id")
    suspend fun updateContent(id: String, front: String, back: String, sourceChunkIds: List<String>)

    @Query("SELECT * FROM cards WHERE due_at <= :now ORDER BY due_at ASC LIMIT :limit")
    suspend fun getDue(now: Long, limit: Int): List<CardEntity>

    @Query("SELECT * FROM cards WHERE document_id = :documentId AND due_at <= :now ORDER BY due_at ASC LIMIT :limit")
    suspend fun getDueForDocument(documentId: String, now: Long, limit: Int): List<CardEntity>

    @Query("SELECT * FROM cards WHERE document_id = :documentId ORDER BY id ASC")
    suspend fun getByDocument(documentId: String): List<CardEntity>

    @Query(
        "UPDATE cards SET ease = :ease, interval_days = :intervalDays, reps = :reps, lapses = :lapses, " +
            "due_at = :dueAt WHERE id = :id",
    )
    suspend fun updateSchedule(id: String, ease: Double, intervalDays: Int, reps: Int, lapses: Int, dueAt: Long)

    @Query("SELECT document_id, COUNT(*) AS count FROM cards WHERE due_at <= :now GROUP BY document_id")
    fun observeDueCounts(now: Long): Flow<List<DueCountRow>>
}

@Dao
interface QuizAttemptDao {
    @Insert
    suspend fun insert(attempt: QuizAttemptEntity)
}
