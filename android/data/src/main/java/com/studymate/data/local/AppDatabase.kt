package com.studymate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.studymate.data.local.dao.CardDao
import com.studymate.data.local.dao.DocumentDao
import com.studymate.data.local.dao.QuizAttemptDao
import com.studymate.data.local.entity.CardEntity
import com.studymate.data.local.entity.DocumentEntity
import com.studymate.data.local.entity.QuizAttemptEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Database(
    entities = [DocumentEntity::class, CardEntity::class, QuizAttemptEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun cardDao(): CardDao
    abstract fun quizAttemptDao(): QuizAttemptDao

    companion object {
        const val NAME = "studymate.db"
    }
}

internal class Converters {
    private val serializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(serializer, value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else Json.decodeFromString(serializer, value)
}
