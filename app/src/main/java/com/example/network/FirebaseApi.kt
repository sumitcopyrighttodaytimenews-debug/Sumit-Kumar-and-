package com.example.network

import com.example.model.*
import retrofit2.http.*

interface FirebaseApi {

    @GET("noticeBoard.json")
    suspend fun getNoticeBoard(): NoticeBoard?

    @GET("users.json")
    suspend fun getUserByMobile(
        @Query("orderBy") orderBy: String = "\"mobile\"",
        @Query("equalTo") equalTo: String
    ): Map<String, User>?

    @GET("users.json")
    suspend fun getUserByLoginKey(
        @Query("orderBy") orderBy: String = "\"loginKey\"",
        @Query("equalTo") equalTo: String
    ): Map<String, User>?

    @GET("users.json")
    suspend fun getTopUsers(
        @Query("orderBy") orderBy: String = "\"watchTimeSeconds\"",
        @Query("limitToLast") limitToLast: Int = 10
    ): Map<String, User>?

    @POST("users.json")
    suspend fun registerUser(@Body user: User): Map<String, String>?

    @GET("batches.json")
    suspend fun getBatches(): Map<String, Any>? // Parsed dynamically to avoid nested type issues with Moshi

    @GET("users/{userId}/bookmarks.json")
    suspend fun getUserBookmarks(@Path("userId") userId: String): Map<String, Bookmark>?

    @PUT("users/{userId}/bookmarks/{lectureId}.json")
    suspend fun addBookmark(
        @Path("userId") userId: String,
        @Path("lectureId") lectureId: String,
        @Body bookmark: Bookmark
    ): Bookmark

    @DELETE("users/{userId}/bookmarks/{lectureId}.json")
    suspend fun deleteBookmark(
        @Path("userId") userId: String,
        @Path("lectureId") lectureId: String
    ): Any

    @PUT("users/{userId}/progress/{batchId}/{subject}/{lectureId}.json")
    suspend fun saveProgress(
        @Path("userId") userId: String,
        @Path("batchId") batchId: String,
        @Path("subject") subject: String,
        @Path("lectureId") lectureId: String,
        @Body progress: UserProgress
    ): UserProgress

    @GET("users/{userId}/watchTimeSeconds.json")
    suspend fun getWatchTime(@Path("userId") userId: String): Long?

    @PUT("users/{userId}/watchTimeSeconds.json")
    suspend fun setWatchTime(
        @Path("userId") userId: String,
        @Body value: Long
    ): Long
}
