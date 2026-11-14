package com.studymate.data.di

import android.content.Context
import androidx.room.Room
import com.studymate.data.config.ApiConfig
import com.studymate.data.fake.FakeStudyMateApi
import com.studymate.data.file.AndroidLocalFileSource
import com.studymate.data.file.LocalFileSource
import com.studymate.data.local.AppDatabase
import com.studymate.data.local.dao.CardDao
import com.studymate.data.local.dao.DocumentDao
import com.studymate.data.local.dao.QuizAttemptDao
import com.studymate.data.remote.AndroidKeystoreSessionTokenStore
import com.studymate.data.remote.MobileBearerInterceptor
import com.studymate.data.remote.MobileSessionAuthenticator
import com.studymate.data.remote.SessionTokenStore
import com.studymate.data.remote.StudyMateApi
import com.studymate.data.repository.DocumentRepositoryImpl
import com.studymate.data.repository.StudyRepositoryImpl
import com.studymate.data.repository.TutorRepositoryImpl
import com.studymate.data.repository.PodcastRepositoryImpl
import com.studymate.data.time.SystemTimeProvider
import com.studymate.domain.common.TimeProvider
import com.studymate.domain.repository.DocumentRepository
import com.studymate.domain.repository.StudyRepository
import com.studymate.domain.repository.TutorRepository
import com.studymate.domain.repository.PodcastRepository
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideSessionTokenStore(@ApplicationContext context: Context): SessionTokenStore =
        AndroidKeystoreSessionTokenStore(context)

    @Provides
    @Singleton
    @SessionHttpClient
    fun provideSessionHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        config: ApiConfig,
        tokenStore: SessionTokenStore,
        json: Json,
        @SessionHttpClient sessionClient: OkHttpClient,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(MobileBearerInterceptor(tokenStore))
            .authenticator(
                MobileSessionAuthenticator(
                    baseUrl = config.baseUrl.toHttpUrl(),
                    sessionClient = sessionClient,
                    tokenStore = tokenStore,
                    json = json,
                ),
            )
            .apply {
                if (config.enableHttpLogging) {
                    // BASIC: chỉ log dòng request/response, không log nội dung tài liệu.
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    @Provides
    @Singleton
    fun provideStudyMateApi(
        config: ApiConfig,
        json: Json,
        client: Lazy<OkHttpClient>,
        fake: Lazy<FakeStudyMateApi>,
    ): StudyMateApi =
        if (config.useFakeApi) {
            fake.get()
        } else {
            Retrofit.Builder()
                .baseUrl(config.baseUrl)
                .client(client.get())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(StudyMateApi::class.java)
        }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideDocumentDao(database: AppDatabase): DocumentDao = database.documentDao()

    @Provides
    fun provideCardDao(database: AppDatabase): CardDao = database.cardDao()

    @Provides
    fun provideQuizAttemptDao(database: AppDatabase): QuizAttemptDao = database.quizAttemptDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BindingModule {
    @Binds
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository

    @Binds
    abstract fun bindStudyRepository(impl: StudyRepositoryImpl): StudyRepository

    @Binds
    abstract fun bindTutorRepository(impl: TutorRepositoryImpl): TutorRepository

    @Binds
    abstract fun bindPodcastRepository(impl: PodcastRepositoryImpl): PodcastRepository

    @Binds
    abstract fun bindLocalFileSource(impl: AndroidLocalFileSource): LocalFileSource

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
