package com.studymate.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studymate.app.ui.adaptive.AdaptiveWidth
import com.studymate.app.ui.components.BottomNavBar
import com.studymate.app.ui.components.StudyNavRail
import com.studymate.app.ui.components.TopLevelTab
import com.studymate.app.ui.feature.add.AddAction
import com.studymate.app.ui.feature.add.AddDocumentSheet
import com.studymate.app.ui.navigation.LibraryRoute
import com.studymate.app.ui.navigation.ProcessingRoute
import com.studymate.app.ui.navigation.StudyMateNavHost
import com.studymate.app.ui.navigation.TodayRoute
import com.studymate.app.ui.theme.Motion
import com.studymate.app.ui.theme.StudyTheme

/**
 * Khung app: NavHost + điều hướng + sheet "Thêm tài liệu" dùng chung.
 * Điện thoại: thanh điều hướng nổi ở dưới, chỉ hiện ở 2 tab gốc.
 * Tablet / máy gập / điện thoại ngang: Navigation Rail bên trái, luôn hiện.
 */
@Composable
fun StudyMateApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination.topLevelTab()
    val useRail = !AdaptiveWidth.current.isCompact
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var addAction by rememberSaveable { mutableStateOf<AddAction?>(null) }
    val openAdd: (AddAction?) -> Unit = { action ->
        addAction = action
        showAddSheet = true
    }

    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val topLevelPadding = PaddingValues(
        start = if (useRail) 32.dp else 20.dp,
        end = if (useRail) 32.dp else 20.dp,
        top = systemBars.calculateTopPadding() + if (useRail) 28.dp else 16.dp,
        bottom = systemBars.calculateBottomPadding() + if (useRail) 32.dp else 120.dp,
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(StudyTheme.colors.background),
    ) {
        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                StudyNavRail(
                    current = currentTab,
                    onSelect = { navController.navigateToTab(it) },
                    onAdd = { openAdd(null) },
                )
                VerticalDivider(Modifier.fillMaxHeight().width(1.dp), color = StudyTheme.colors.divider)
                Box(Modifier.weight(1f)) {
                    StudyMateNavHost(navController, topLevelPadding, snackbarHostState, openAdd)
                }
            }
        } else {
            StudyMateNavHost(navController, topLevelPadding, snackbarHostState, openAdd)
            AnimatedVisibility(
                visible = currentTab != null,
                enter = slideInVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it } + fadeIn(),
                exit = slideOutVertically(tween(Motion.SHORT)) { it } + fadeOut(tween(Motion.SHORT)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomNavBar(
                    current = currentTab ?: TopLevelTab.TODAY,
                    onSelect = { navController.navigateToTab(it) },
                    onAdd = { openAdd(null) },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (useRail) 24.dp else 96.dp),
        )
    }

    if (showAddSheet) {
        AddDocumentSheet(
            initialAction = addAction,
            onDismiss = {
                showAddSheet = false
                addAction = null
            },
            onUploaded = { documentId ->
                showAddSheet = false
                addAction = null
                navController.navigate(ProcessingRoute(documentId))
            },
        )
    }
}

private fun NavDestination?.topLevelTab(): TopLevelTab? = when {
    this == null -> null
    hierarchy.any { it.hasRoute(TodayRoute::class) } -> TopLevelTab.TODAY
    hierarchy.any { it.hasRoute(LibraryRoute::class) } -> TopLevelTab.LIBRARY
    else -> null
}

private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    val route: Any = when (tab) {
        TopLevelTab.TODAY -> TodayRoute
        TopLevelTab.LIBRARY -> LibraryRoute
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
