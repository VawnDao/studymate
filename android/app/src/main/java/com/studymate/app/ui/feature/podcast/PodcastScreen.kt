package com.studymate.app.ui.feature.podcast

import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.studymate.app.R
import com.studymate.app.ui.common.messageRes
import com.studymate.app.ui.components.BackHeader
import com.studymate.app.ui.components.LoadingView
import com.studymate.app.ui.components.MessageView
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.PodcastStatus

@Composable
fun PodcastRoute(onBack: () -> Unit, viewModel: PodcastViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PodcastScreen(state, onBack, viewModel::create, viewModel::retry)
}

@OptIn(UnstableApi::class)
@Composable
private fun PodcastScreen(state: PodcastUiState, onBack: () -> Unit, onCreate: () -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.podcast_title), onBack, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        when {
            state.error != null -> MessageView(
                title = stringResource(R.string.podcast_error_title),
                body = stringResource(state.error.messageRes()),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry,
            )
            state.podcast == null && !state.isStarting -> {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Outlined.Headphones, contentDescription = null, tint = StudyTheme.colors.primary)
                    Text(stringResource(R.string.podcast_intro), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.podcast_ai_badge), color = StudyTheme.colors.primaryStrong)
                    Button(onClick = onCreate) { Text(stringResource(R.string.podcast_create)) }
                }
            }
            state.isStarting || state.isLoading -> LoadingView()
            state.podcast?.status == PodcastStatus.READY -> PodcastReady(state)
            else -> MessageView(
                title = stringResource(R.string.podcast_error_title),
                body = stringResource(R.string.podcast_failed),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PodcastReady(state: PodcastUiState) {
    val podcast = state.podcast ?: return
    val context = LocalContext.current
    val player = remember(podcast.audioUrl, state.accessToken) {
        podcast.audioUrl?.let { url ->
            val http = DefaultHttpDataSource.Factory().setDefaultRequestProperties(
                mapOf("Authorization" to "Bearer ${state.accessToken.orEmpty()}"),
            )
            ExoPlayer.Builder(context).setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(http)).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
            }
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(podcast.title.ifBlank { stringResource(R.string.podcast_title) }, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.podcast_ai_badge), color = StudyTheme.colors.primaryStrong)
        player?.let { exo ->
            AndroidView(
                factory = { PlayerView(it).apply { this.player = exo; layoutParams = ViewGroup.LayoutParams(-1, 220) } },
                modifier = Modifier.fillMaxWidth(),
            )
        } ?: Text(stringResource(R.string.podcast_no_audio), color = StudyTheme.colors.inkMuted)
        Surface(color = StudyTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
            Text(podcast.script, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
