package com.studymate.data.remote

import com.studymate.data.remote.dto.StudyPackDto
import com.studymate.data.remote.dto.TutorRequestDto
import com.studymate.data.remote.mapper.toDomain
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode
import com.studymate.domain.model.SourceType
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RemoteContractTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
    private lateinit var server: MockWebServer
    private lateinit var api: StudyMateApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        MobileBearerInterceptor(
                            InMemorySessionTokenStore(
                                MobileSessionToken("test-token", Long.MAX_VALUE),
                            ),
                        ),
                    )
                    .readTimeout(1, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(StudyMateApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sample pack parses and maps to the domain`() {
        val raw = requireNotNull(javaClass.classLoader?.getResource("pack_sample.json")).readText()

        val pack = json.decodeFromString(StudyPackDto.serializer(), raw).toDomain()

        assertEquals(SourceType.PDF, pack.sourceType)
        assertEquals(6, pack.flashcards.size)
        assertEquals(4, pack.quiz.size)
        assertEquals(3, pack.pageOf("c004"))
        assertTrue(pack.flashcards.all { it.sourceChunkIds.isNotEmpty() })
    }

    @Test
    fun `malformed quiz items are dropped by the mapper`() {
        val raw = """
            {"document":{"id":"d","title":"t"},"summary":{},
             "quiz":[{"id":"q1","question":"?","options":["a","b"],"answer_index":0},
                     {"id":"q2","question":"?","options":["a","b","c","d"],"answer_index":7},
                     {"id":"q3","question":"?","options":["a","b","c","d"],"answer_index":1}]}
        """.trimIndent()

        val pack = json.decodeFromString(StudyPackDto.serializer(), raw).toDomain()

        assertEquals(listOf("q3"), pack.quiz.map { it.id })
    }

    @Test
    fun `api error envelope maps to a domain error code and bearer session is sent`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody("""{"error":{"code":"PDF_TOO_LONG","message":"Tài liệu vượt 20 trang"}}"""),
        )

        val result = apiCall(json) { api.getDocumentStatus("d1") }

        assertEquals(AppResult.Failure(DomainError.Api(ErrorCode.PDF_TOO_LONG, "Tài liệu vượt 20 trang")), result)
        assertEquals("Bearer test-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `http status is used when the body has no error envelope`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("slow down"))

        val result = apiCall(json) { api.getDocumentStatus("d1") }

        assertEquals(AppResult.Failure(DomainError.Api(ErrorCode.RATE_LIMITED, null)), result)
    }

    @Test
    fun `backend scaffold 501 NOT_IMPLEMENTED maps to a dedicated code`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(501)
                .setBody("""{"error":{"code":"NOT_IMPLEMENTED","message":"Not implemented"}}"""),
        )

        val result = apiCall(json) { api.getDocumentStatus("d_1") }

        assertEquals(AppResult.Failure(DomainError.Api(ErrorCode.NOT_IMPLEMENTED, "Not implemented")), result)
    }

    @Test
    fun `upload response without title or pages still parses`() = runTest {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"id":"d_abc","status":"uploaded","step":0,"steps_total":4}"""))

        val result = apiCall(json) {
            api.uploadDocument(listOf(okhttp3.MultipartBody.Part.createFormData("title", "Quang hợp")))
        }

        assertEquals("d_abc", (result as AppResult.Success).value.id)
    }

    @Test
    fun `read timeout maps to Timeout`() = runTest {
        server.enqueue(MockResponse().setBody("{}").setHeadersDelay(3, TimeUnit.SECONDS))

        val result = apiCall(json) { api.getDocumentStatus("d1") }

        assertEquals(AppResult.Failure(DomainError.Timeout), result)
    }

    @Test
    fun `tutor request is serialized with snake_case fields`() = runTest {
        server.enqueue(MockResponse().setBody("""{"reply":"ok","mode":"hint","citations":[],"in_document":true}"""))

        apiCall(json) { api.askTutor("d1", TutorRequestDto(messages = emptyList(), hintLevel = 2)) }

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body, body.contains("\"hint_level\":2"))
    }
}

private class InMemorySessionTokenStore(
    private var session: MobileSessionToken?,
) : SessionTokenStore {
    override fun get(): MobileSessionToken? = session
    override fun save(session: MobileSessionToken) { this.session = session }
    override fun clear() { session = null }
}
