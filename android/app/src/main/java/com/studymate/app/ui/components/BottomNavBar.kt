package com.studymate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.studymate.app.R
import com.studymate.app.ui.theme.StudyTheme

enum class TopLevelTab { TODAY, LIBRARY }

/** Thanh điều hướng nổi: Hôm nay · Thêm · Thư viện (theo prototype). */
@Composable
fun BottomNavBar(
    current: TopLevelTab,
    onSelect: (TopLevelTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StudyTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = colors.ink.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(26.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.divider),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(
                label = stringResource(R.string.nav_today),
                icon = Icons.Outlined.Home,
                selected = current == TopLevelTab.TODAY,
                onClick = { onSelect(TopLevelTab.TODAY) },
                modifier = Modifier.weight(1f),
            )
            AddItem(onClick = onAdd, modifier = Modifier.weight(1f))
            NavItem(
                label = stringResource(R.string.nav_library),
                icon = Icons.Outlined.FolderOpen,
                selected = current == TopLevelTab.LIBRARY,
                onClick = { onSelect(TopLevelTab.LIBRARY) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = StudyTheme.colors
    val content by animateColorAsState(if (selected) colors.primaryStrong else colors.inkMuted, tween(280), label = "navContent")
    val pill by animateColorAsState(if (selected) colors.primaryTint else Color.Transparent, tween(280), label = "navPill")
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab, interactionSource = interaction, indication = null)
            .pressScale(interaction),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 30.dp)
                .clip(CircleShape)
                .background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun AddItem(onClick: () -> Unit, modifier: Modifier) {
    val colors = StudyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val label = stringResource(R.string.nav_add)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = false, onClick = onClick, role = Role.Button, interactionSource = interaction, indication = null)
            .pressScale(interaction),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = colors.primary)
                .size(width = 52.dp, height = 36.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.primaryStrong, modifier = Modifier.padding(top = 4.dp))
    }
}
