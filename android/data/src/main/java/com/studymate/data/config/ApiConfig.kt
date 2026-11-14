package com.studymate.data.config

/** Cấu hình kết nối backend, do module app cung cấp từ BuildConfig. */
data class ApiConfig(
    val baseUrl: String,
    val useFakeApi: Boolean,
    val enableHttpLogging: Boolean,
)
