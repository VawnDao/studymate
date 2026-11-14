package com.studymate.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.studymate.app.R
import com.studymate.app.ui.theme.StudyTheme
import com.studymate.domain.model.DocumentWithDue
import com.studymate.domain.model.ProcessingStatus
import com.studymate.domain.model.SourceType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentRow(
    item: DocumentWithDue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = StudyTheme.colors
    val document = item.document
    val interaction = remember { MutableInteractionSource() }
    val longClickLabel = stringResource(R.string.library_delete)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = if (onLongClick != null) longClickLabel else null,
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubjectBadge(document.title)
        Column(Modifier.weight(1f)) {
            Text(document.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                text = subtitle(item),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                maxLines = 1,
            )
        }
        when (document.status) {
            ProcessingStatus.READY -> if (item.dueCount > 0) {
                Tag(
                    text = pluralStringResource(R.plurals.due_cards_short, item.dueCount, item.dueCount),
                    container = colors.primaryTint,
                    content = colors.primaryStrong,
                )
            }
            ProcessingStatus.FAILED -> Tag(stringResource(R.string.document_failed), colors.errorTint, colors.errorText)
            else -> Tag(stringResource(R.string.document_processing), colors.warningTint, colors.warningText)
        }
    }
}

@Composable
private fun subtitle(item: DocumentWithDue): String {
    val source = when (item.document.sourceType) {
        SourceType.PDF -> stringResource(R.string.source_type_pdf)
        SourceType.IMAGES -> stringResource(R.string.source_type_images)
    }
    return if (item.document.pages > 0) {
        stringResource(R.string.document_meta, source, pluralStringResource(R.plurals.pages, item.document.pages, item.document.pages))
    } else {
        source
    }
}
