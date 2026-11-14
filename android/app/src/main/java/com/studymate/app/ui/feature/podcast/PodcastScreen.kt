package com.studymate.app.ui.feature.podcast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
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
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
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
            state.isStarting || (state.isLoading && state.podcast?.status == PodcastStatus.GENERATING) -> {
                var elapsedSeconds by remember { mutableIntStateOf(0) }
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(1_000)
                        elapsedSeconds++
                    }
                }
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = StudyTheme.colors.primary)
                    Text(stringResource(R.string.podcast_generating_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.podcast_generating_eta, elapsedSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyTheme.colors.inkMuted,
                    )
                }
            }
            state.isLoading -> LoadingView()
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
            PodcastAudioPlayer(exo)
        } ?: Text(stringResource(R.string.podcast_no_audio), color = StudyTheme.colors.inkMuted)
        Surface(
            color = StudyTheme.colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(podcast.script, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PodcastAudioPlayer(exo: ExoPlayer) {
    val colors = StudyTheme.colors
    var isPlaying by remember(exo) { mutableStateOf(exo.isPlaying) }
    var positionMs by remember(exo) { mutableLongStateOf(0L) }
    var durationMs by remember(exo) { mutableLongStateOf(0L) }
    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (exo.duration > 0) durationMs = exo.duration
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener) }
    }
    LaunchedEffect(exo, isPlaying) {
        while (true) {
            positionMs = exo.currentPosition.coerceAtLeast(0L)
            if (exo.duration > 0) durationMs = exo.duration
            if (!isPlaying) break
            kotlinx.coroutines.delay(250)
        }
    }
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.divider),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledIconButton(
                onClick = { if (exo.isPlaying) exo.pause() else exo.play() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White,
                ),
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                )
            }
            Column(Modifier.weight(1f)) {
                Slider(
                    value = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction ->
                        if (durationMs > 0) {
                            val target = (fraction * durationMs).toLong()
                            positionMs = target
                            exo.seekTo(target)
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.divider,
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatAudioTime(positionMs), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                    Text(formatAudioTime(durationMs), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                }
            }
        }
    }
}

private fun formatAudioTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
