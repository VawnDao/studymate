package com.studymate.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Serializable
data object LibraryRoute

@Serializable
data class ProcessingRoute(val documentId: String)

@Serializable
data class HubRoute(val documentId: String)

@Serializable
data class SummaryRoute(val documentId: String)

/** `documentId == null`: ôn mọi thẻ đến hạn của tất cả tài liệu. */
@Serializable
data class CardsRoute(val documentId: String? = null)

@Serializable
data class QuizRoute(val documentId: String)

@Serializable
data class TutorRoute(val documentId: String)
