package com.studymate.app.di

import com.studymate.app.BuildConfig
import com.studymate.data.config.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" },
        useFakeApi = BuildConfig.USE_FAKE_API,
        enableHttpLogging = BuildConfig.DEBUG,
    )
}
