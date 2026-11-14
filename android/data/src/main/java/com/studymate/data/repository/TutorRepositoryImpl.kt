package com.studymate.data.repository

import com.studymate.data.remote.StudyMateApi
import com.studymate.data.remote.apiCall
import com.studymate.data.remote.dto.TutorRequestDto
import com.studymate.data.remote.mapper.toDomain
import com.studymate.data.remote.mapper.toDto
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.map
import com.studymate.domain.model.TutorMessage
import com.studymate.domain.model.TutorReply
import com.studymate.domain.repository.TutorRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class TutorRepositoryImpl @Inject constructor(
    private val api: StudyMateApi,
    private val json: Json,
) : TutorRepository {

    override suspend fun ask(documentId: String, messages: List<TutorMessage>, hintLevel: Int): AppResult<TutorReply> =
        apiCall(json) {
            api.askTutor(documentId, TutorRequestDto(messages = messages.map { it.toDto() }, hintLevel = hintLevel))
        }.map { it.toDomain() }
}
