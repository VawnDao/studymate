package com.studymate.domain.usecase

import com.studymate.domain.common.AppResult
import com.studymate.domain.model.Podcast
import com.studymate.domain.model.PodcastStatus
import com.studymate.domain.repository.PodcastRepository
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StartPodcastUseCase @Inject constructor(private val podcasts: PodcastRepository) {
    suspend operator fun invoke(documentId: String): AppResult<Podcast> = podcasts.start(documentId)
}

class ObservePodcastUseCase @Inject constructor(private val podcasts: PodcastRepository) {
    operator fun invoke(documentId: String, pollInterval: Duration = 5.seconds): Flow<AppResult<Podcast>> = flow {
        var delayDuration = pollInterval
        while (true) {
            val result = podcasts.status(documentId)
            emit(result)
            val podcast = (result as? AppResult.Success)?.value
            if (podcast?.status == PodcastStatus.READY || podcast?.status == PodcastStatus.FAILED) return@flow
            delay(delayDuration)
            delayDuration = (delayDuration * 2).coerceAtMost(20.seconds)
        }
    }
}

class GetPodcastAccessTokenUseCase @Inject constructor(private val podcasts: PodcastRepository) {
    suspend operator fun invoke(): String? = podcasts.accessToken()
}
