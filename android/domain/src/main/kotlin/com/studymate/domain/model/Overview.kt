package com.studymate.domain.model

data class DocumentWithDue(
    val document: Document,
    val dueCount: Int,
)

data class TodayOverview(
    val dueTotal: Int,
    /** Tài liệu có nhiều thẻ đến hạn nhất, gợi ý ôn trước. */
    val focus: DocumentWithDue?,
    val recent: List<DocumentWithDue>,
    val documentCount: Int,
)

data class DocumentHub(
    val pack: StudyPack,
    val dueCount: Int,
)
