package com.studymate.domain.model

data class Podcast(
    val documentId: String,
    val title: String = "",
    val script: String = "",
    val audioUrl: String? = null,
    val status: PodcastStatus,
    val aiGenerated: Boolean = true,
)

enum class PodcastStatus {
    GENERATING, READY, FAILED;

    companion object {
        fun fromWire(value: String): PodcastStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FAILED
    }
}
