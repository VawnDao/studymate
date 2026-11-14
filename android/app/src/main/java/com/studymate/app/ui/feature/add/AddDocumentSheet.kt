package com.studymate.app.ui.feature.add

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studymate.app.R
import com.studymate.app.ui.common.ObserveAsEvents
import com.studymate.app.ui.common.messageRes
import com.studymate.app.ui.components.PressableCard
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.DocumentInput
import com.studymate.domain.usecase.ImportDocumentUseCase
import java.io.File

/**
 * Sheet "Thêm tài liệu". [initialAction] (từ lối tắt ở Hôm nay) mở thẳng trình chọn tương ứng.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentSheet(
    initialAction: AddAction?,
    onDismiss: () -> Unit,
    onUploaded: (documentId: String) -> Unit,
    viewModel: AddDocumentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var initialHandled by rememberSaveable { mutableStateOf(false) }

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is AddDocumentEffect.Uploaded -> onUploaded(effect.documentId)
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.import(DocumentInput.Pdf(it.toString())) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = ImportDocumentUseCase.MAX_IMAGES),
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.import(DocumentInput.Images(uris.map(Uri::toString)))
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) viewModel.import(DocumentInput.Images(listOf(uri.toString())))
        pendingCameraUri = null
    }

    fun launch(action: AddAction) {
        if (state.isUploading) return
        viewModel.clearError()
        when (action) {
            AddAction.PDF -> pdfLauncher.launch(arrayOf(MIME_PDF))
            AddAction.GALLERY -> galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            AddAction.CAMERA -> createCameraUri(context).also {
                pendingCameraUri = it
                cameraLauncher.launch(it)
            }
        }
    }

    LaunchedEffect(initialAction) {
        if (initialAction != null && !initialHandled) {
            initialHandled = true
            launch(initialAction)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!state.isUploading) onDismiss() },
        sheetState = sheetState,
        containerColor = StudyTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.add_document), style = MaterialTheme.typography.titleLarge)
            AddOption(
                title = stringResource(R.string.add_pdf),
                subtitle = stringResource(R.string.add_pdf_sub),
                icon = Icons.Outlined.PictureAsPdf,
                container = StudyTheme.colors.sky,
                tint = StudyTheme.colors.blueText,
                enabled = !state.isUploading,
                onClick = { launch(AddAction.PDF) },
            )
            AddOption(
                title = stringResource(R.string.add_camera),
                subtitle = stringResource(R.string.add_camera_sub),
                icon = Icons.Outlined.CameraAlt,
                container = StudyTheme.colors.peach,
                tint = StudyTheme.colors.orangeText,
                enabled = !state.isUploading,
                onClick = { launch(AddAction.CAMERA) },
            )
            AddOption(
                title = stringResource(R.string.add_gallery),
                subtitle = stringResource(R.string.add_gallery_sub),
                icon = Icons.Outlined.Image,
                container = StudyTheme.colors.mint,
                tint = StudyTheme.colors.success,
                enabled = !state.isUploading,
                onClick = { launch(AddAction.GALLERY) },
            )
            AnimatedVisibility(visible = state.isUploading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = StudyTheme.colors.primary)
                    Text(stringResource(R.string.add_uploading), style = MaterialTheme.typography.bodyMedium)
                }
            }
            AnimatedVisibility(visible = state.error != null) {
                state.error?.let {
                    Text(
                        stringResource(it.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudyTheme.colors.errorText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudyTheme.colors.errorTint)
                            .padding(12.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.add_limits),
                style = MaterialTheme.typography.bodySmall,
                color = StudyTheme.colors.inkMuted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AddOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    container: Color,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PressableCard(
        onClick = { if (enabled) onClick() },
        color = container,
        shape = RoundedCornerShape(20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = StudyTheme.colors.surface) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.inkSecondary)
            }
        }
    }
}

/** Ảnh chụp lưu tạm trong cache của app, chia sẻ cho app camera qua FileProvider. */
private fun createCameraUri(context: Context): Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("capture_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private const val MIME_PDF = "application/pdf"
