package com.studymate.data.repository

import com.studymate.data.local.dao.CardDao
import com.studymate.data.local.dao.QuizAttemptDao
import com.studymate.data.local.mapper.toDomain
import com.studymate.data.local.mapper.toEntity
import com.studymate.domain.model.Flashcard
import com.studymate.domain.model.QuizAttempt
import com.studymate.domain.model.ReviewSchedule
import com.studymate.domain.repository.StudyRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class StudyRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val quizAttemptDao: QuizAttemptDao,
) : StudyRepository {

    override fun observeDueCounts(now: Instant): Flow<Map<String, Int>> =
        cardDao.observeDueCounts(now.toEpochMilli()).map { rows -> rows.associate { it.documentId to it.count } }

    override suspend fun getDueCards(documentId: String?, now: Instant, limit: Int): List<Flashcard> {
        val entities = if (documentId == null) {
            cardDao.getDue(now.toEpochMilli(), limit)
        } else {
            cardDao.getDueForDocument(documentId, now.toEpochMilli(), limit)
        }
        return entities.map { it.toDomain() }
    }

    override suspend fun getCards(documentId: String): List<Flashcard> =
        cardDao.getByDocument(documentId).map { it.toDomain() }

    override suspend fun updateSchedule(cardId: String, schedule: ReviewSchedule) {
        cardDao.updateSchedule(
            id = cardId,
            ease = schedule.ease,
            intervalDays = schedule.intervalDays,
            reps = schedule.reps,
            lapses = schedule.lapses,
            dueAt = schedule.dueAt.toEpochMilli(),
        )
    }

    override suspend fun saveQuizAttempt(attempt: QuizAttempt) {
        quizAttemptDao.insert(attempt.toEntity())
    }
}
