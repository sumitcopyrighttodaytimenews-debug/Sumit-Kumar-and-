package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NoticeBoard(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class User(
    val name: String? = null,
    val dob: String? = null,
    val fatherName: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    val loginKey: String? = null,
    val watchTimeSeconds: Long? = 0L,
    val joinedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class Lecture(
    val id: String = "",
    val lectureName: String? = null,
    val topicName: String? = null,
    val videoUrl: String? = null,
    val notesUrl: String? = null,
    val timestamp: Long? = null,
    val uploadTime: Long? = null,
    val classNum: Int? = null
)

@JsonClass(generateAdapter = true)
data class Batch(
    val id: String = "",
    val name: String? = null,
    val poster: String? = null,
    val subjects: Any? = null, // Custom decoded in standard parsing
    val lectures: Map<String, Map<String, Lecture>>? = null
)

@JsonClass(generateAdapter = true)
data class UserProgress(
    val currentTime: Double? = 0.0,
    val percentage: Double? = 0.0,
    val lastUpdated: Long? = null
)

@JsonClass(generateAdapter = true)
data class Bookmark(
    val id: String = "",
    val title: String? = null,
    val date: String? = null,
    val videoUrl: String? = null,
    val posterUrl: String? = null,
    val batchId: String? = null,
    val subject: String? = null,
    val savedAt: Long? = null
)
