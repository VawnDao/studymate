package com.studymate.data.remote

import com.studymate.data.remote.dto.ChunkDto
import com.studymate.data.remote.dto.DocumentStatusDto
import com.studymate.data.remote.dto.StudyPackDto
import com.studymate.data.remote.dto.TutorReplyDto
import com.studymate.data.remote.dto.TutorRequestDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** Hợp đồng StudyMate API v1; MobileSessionAuthenticator handles anonymous bearer sessions. */
interface StudyMateApi {

    /** multipart: `file` (1 PDF) hoặc `images` (1–5 JPEG/PNG), kèm `title`. */
    @Multipart
    @POST("v1/documents")
    suspend fun uploadDocument(@Part parts: List<MultipartBody.Part>): DocumentStatusDto

    @GET("v1/documents/{id}")
    suspend fun getDocumentStatus(@Path("id") documentId: String): DocumentStatusDto

    @GET("v1/documents/{id}/pack")
    suspend fun getStudyPack(@Path("id") documentId: String): StudyPackDto

    @GET("v1/documents/{id}/chunks/{chunkId}")
    suspend fun getChunk(@Path("id") documentId: String, @Path("chunkId") chunkId: String): ChunkDto

    @POST("v1/documents/{id}/tutor")
    suspend fun askTutor(@Path("id") documentId: String, @Body body: TutorRequestDto): TutorReplyDto

    @DELETE("v1/documents/{id}")
    suspend fun deleteDocument(@Path("id") documentId: String): Response<Unit>
}
