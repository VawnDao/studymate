package com.studymate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "documents", indices = [Index("created_at")])
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val pages: Int,
    @ColumnInfo(name = "source_type") val sourceType: String,
    val status: String,
    @ColumnInfo(name = "subject", defaultValue = "") val subject: String = "",
    /** JSON bộ ôn tập (đúng định dạng API) để học offline; null khi chưa đồng bộ. */
    @ColumnInfo(name = "pack_json") val packJson: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("document_id"), Index("due_at")],
)
data class CardEntity(
    /** `<documentId>::<cardId>`: id của thẻ chỉ duy nhất trong một tài liệu. */
    @PrimaryKey val id: String,
    @ColumnInfo(name = "document_id") val documentId: String,
    val front: String,
    val back: String,
    @ColumnInfo(name = "source_chunk_ids") val sourceChunkIds: List<String>,
    val ease: Double,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    val reps: Int,
    val lapses: Int,
    @ColumnInfo(name = "due_at") val dueAt: Long,
)

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("document_id")],
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "document_id") val documentId: String,
    val score: Int,
    val total: Int,
    @ColumnInfo(name = "wrong_ids") val wrongIds: List<String>,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

data class DueCountRow(
    @ColumnInfo(name = "document_id") val documentId: String,
    val count: Int,
)
