package com.studymate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.unit.dp
import com.studymate.app.R
import com.studymate.app.ui.theme.StudyTheme

/**
 * Navigation Rail cho tablet (MEDIUM/EXPANDED): nút Thêm ở trên cùng, rồi Hôm nay, Thư viện.
 * Cùng ngôn ngữ thiết kế với [BottomNavBar] trên điện thoại.
 */
@Composable
fun StudyNavRail(
    current: TopLevelTab?,
    onSelect: (TopLevelTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StudyTheme.colors
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(96.dp)
            .background(colors.surface)
            .systemBarsPadding()
            .padding(vertical = 20.dp)
            .semantics { selectableGroup() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RailAddButton(onAdd)
        Box(Modifier.size(8.dp))
        RailItem(
            label = stringResource(R.string.nav_today),
            icon = Icons.Outlined.Home,
            selected = current == TopLevelTab.TODAY,
            onClick = { onSelect(TopLevelTab.TODAY) },
        )
        RailItem(
            label = stringResource(R.string.nav_library),
            icon = Icons.Outlined.FolderOpen,
            selected = current == TopLevelTab.LIBRARY,
            onClick = { onSelect(TopLevelTab.LIBRARY) },
        )
    }
}

@Composable
private fun RailAddButton(onClick: () -> Unit) {
    val colors = StudyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = false, onClick = onClick, role = Role.Button, interactionSource = interaction, indication = null)
            .pressScale(interaction)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = colors.primary)
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Text(
            stringResource(R.string.nav_add),
            style = MaterialTheme.typography.labelMedium,
            color = colors.primaryStrong,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun RailItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val colors = StudyTheme.colors
    val content by animateColorAsState(if (selected) colors.primaryStrong else colors.inkMuted, tween(280), label = "railContent")
    val pill by animateColorAsState(if (selected) colors.primaryTint else Color.Transparent, tween(280), label = "railPill")
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab, interactionSource = interaction, indication = null)
            .pressScale(interaction)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(CircleShape)
                .background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, modifier = Modifier.padding(top = 4.dp))
    }
}
