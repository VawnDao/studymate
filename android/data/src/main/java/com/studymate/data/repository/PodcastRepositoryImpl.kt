package com.studymate.data.repository

import com.studymate.data.remote.SessionTokenStore
import com.studymate.data.remote.StudyMateApi
import com.studymate.data.remote.apiCall
import com.studymate.data.remote.mapper.toDomain
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.map
import com.studymate.domain.model.Podcast
import com.studymate.domain.repository.PodcastRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class PodcastRepositoryImpl @Inject constructor(
    private val api: StudyMateApi,
    private val json: Json,
    private val tokenStore: SessionTokenStore,
) : PodcastRepository {
    override suspend fun start(documentId: String): AppResult<Podcast> =
        apiCall(json) { api.createPodcast(documentId) }.map { it.toDomain(documentId) }

    override suspend fun status(documentId: String): AppResult<Podcast> =
        apiCall(json) { api.getPodcast(documentId) }.map { it.toDomain(documentId) }

    override suspend fun accessToken(): String? = tokenStore.get()?.accessToken
}
