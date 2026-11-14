package com.studymate.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.studymate.app.ui.feature.add.AddAction
import com.studymate.app.ui.feature.cards.CardsRoute as CardsScreenRoute
import com.studymate.app.ui.feature.hub.HubRoute as HubScreenRoute
import com.studymate.app.ui.feature.library.LibraryRoute as LibraryScreenRoute
import com.studymate.app.ui.feature.processing.ProcessingRoute as ProcessingScreenRoute
import com.studymate.app.ui.feature.quiz.QuizRoute as QuizScreenRoute
import com.studymate.app.ui.feature.summary.SummaryRoute as SummaryScreenRoute
import com.studymate.app.ui.feature.today.TodayRoute as TodayScreenRoute
import com.studymate.app.ui.feature.tutor.TutorRoute as TutorScreenRoute
import com.studymate.app.ui.theme.Motion
import com.studymate.domain.model.DocumentWithDue
import com.studymate.domain.model.ProcessingStatus

@Composable
fun StudyMateNavHost(
    navController: NavHostController,
    topLevelPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onAdd: (AddAction?) -> Unit,
) {
    val openDocument: (DocumentWithDue) -> Unit = { item ->
        val id = item.document.id
        if (item.document.status == ProcessingStatus.READY) {
            navController.navigate(HubRoute(id))
        } else {
            navController.navigate(ProcessingRoute(id))
        }
    }

    NavHost(
        navController = navController,
        startDestination = TodayRoute,
        // FE.md 10.1: mờ dần + trượt lên 14dp, 380 ms.
        enterTransition = {
            fadeIn(tween(380, easing = Motion.Emphasized)) +
                slideInVertically(tween(380, easing = Motion.Emphasized)) { it / 60 }
        },
        exitTransition = { fadeOut(tween(Motion.SHORT)) },
        popEnterTransition = { fadeIn(tween(Motion.MEDIUM, easing = Motion.Emphasized)) },
        popExitTransition = { fadeOut(tween(Motion.SHORT)) },
    ) {
        composable<TodayRoute> {
            TodayScreenRoute(
                contentPadding = topLevelPadding,
                onReviewDue = { navController.navigate(CardsRoute(documentId = null)) },
                onOpenDocument = openDocument,
                onAdd = onAdd,
            )
        }
        composable<LibraryRoute> {
            LibraryScreenRoute(
                contentPadding = topLevelPadding,
                snackbarHostState = snackbarHostState,
                onOpenDocument = openDocument,
                onAdd = { onAdd(null) },
            )
        }
        composable<ProcessingRoute> {
            ProcessingScreenRoute(
                onBack = { navController.popBackStack() },
                onCompleted = { documentId ->
                    navController.navigate(HubRoute(documentId)) {
                        popUpTo<ProcessingRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<HubRoute> { entry ->
            val documentId = entry.toRoute<HubRoute>().documentId
            HubScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenSummary = { navController.navigate(SummaryRoute(documentId)) },
                onOpenCards = { navController.navigate(CardsRoute(documentId)) },
                onOpenQuiz = { navController.navigate(QuizRoute(documentId)) },
                onOpenTutor = { navController.navigate(TutorRoute(documentId)) },
            )
        }
        composable<SummaryRoute> { entry ->
            val documentId = entry.toRoute<SummaryRoute>().documentId
            SummaryScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenCards = { navController.navigate(CardsRoute(documentId)) },
            )
        }
        composable<CardsRoute> {
            CardsScreenRoute(
                onClose = { navController.popBackStack() },
                onOpenQuiz = { documentId ->
                    navController.navigate(QuizRoute(documentId)) { popUpTo<CardsRoute> { inclusive = true } }
                },
            )
        }
        composable<QuizRoute> { entry ->
            val documentId = entry.toRoute<QuizRoute>().documentId
            QuizScreenRoute(
                onClose = { navController.popBackStack() },
                onOpenTutor = { navController.navigate(TutorRoute(documentId)) },
            )
        }
        composable<TutorRoute> {
            TutorScreenRoute(onBack = { navController.popBackStack() })
        }
    }
}
