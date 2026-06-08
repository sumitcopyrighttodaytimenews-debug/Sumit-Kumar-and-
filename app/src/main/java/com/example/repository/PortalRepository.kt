package com.example.repository

import com.example.model.*
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PortalRepository {
    private val api = RetrofitClient.api

    suspend fun getNoticeBoard(): NoticeBoard? = withContext(Dispatchers.IO) {
        try {
            api.getNoticeBoard()
        } catch (e: Exception) {
            e.printStackTrace()
            NoticeBoard(text = "Welcome to the premium learning portal! Good luck with your studies.")
        }
    }

    suspend fun getUserByMobile(mobile: String): Map<String, User>? = withContext(Dispatchers.IO) {
        try {
            val formatted = "\"$mobile\""
            api.getUserByMobile(equalTo = formatted)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getUserByLoginKey(key: String): Map<String, User>? = withContext(Dispatchers.IO) {
        try {
            val formatted = "\"${key.trim().uppercase()}\""
            api.getUserByLoginKey(equalTo = formatted)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun registerUser(user: User): String? = withContext(Dispatchers.IO) {
        try {
            val res = api.registerUser(user)
            res?.get("name")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getUserBookmarks(userId: String): List<Bookmark> = withContext(Dispatchers.IO) {
        try {
            val res = api.getUserBookmarks(userId)
            res?.entries?.map { entry ->
                entry.value.copy(id = entry.key)
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addBookmark(userId: String, lectureId: String, bookmark: Bookmark) = withContext(Dispatchers.IO) {
        try {
            api.addBookmark(userId, lectureId, bookmark)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteBookmark(userId: String, lectureId: String) = withContext(Dispatchers.IO) {
        try {
            api.deleteBookmark(userId, lectureId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveProgress(userId: String, batchId: String, subject: String, lectureId: String, progress: UserProgress) = withContext(Dispatchers.IO) {
        try {
            api.saveProgress(userId, batchId, subject, lectureId, progress)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getWatchTime(userId: String): Long = withContext(Dispatchers.IO) {
        try {
            api.getWatchTime(userId) ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    suspend fun incrementWatchTime(userId: String, increment: Long) = withContext(Dispatchers.IO) {
        try {
            val current = getWatchTime(userId)
            api.setWatchTime(userId, current + increment)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getTopUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val res = api.getTopUsers()
            res?.values?.filter { it.name != null }?.sortedByDescending { it.watchTimeSeconds ?: 0L } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getBatches(): List<Batch> = withContext(Dispatchers.IO) {
        try {
            val raw = api.getBatches() ?: return@withContext emptyList()
            val list = mutableListOf<Batch>()
            for ((batchId, batchDataRaw) in raw) {
                if (batchDataRaw is Map<*, *>) {
                    val batchMap = batchDataRaw as Map<String, Any>
                    val name = batchMap["name"] as? String
                    val poster = batchMap["poster"] as? String
                    
                    // Parse subjects dynamically
                    val subjectsRaw = batchMap["subjects"]
                    val subjectsList = mutableListOf<String>()
                    if (subjectsRaw is List<*>) {
                        subjectsRaw.forEach { s ->
                            if (s is String) subjectsList.add(s)
                        }
                    } else if (subjectsRaw is Map<*, *>) {
                        subjectsRaw.values.forEach { s ->
                            if (s is String) subjectsList.add(s)
                        }
                    }

                    // Parse lectures dynamically
                    val lecturesRaw = batchMap["lectures"] as? Map<String, Any>
                    val lecturesParsed = mutableMapOf<String, Map<String, Lecture>>()
                    if (lecturesRaw != null) {
                        for ((subjectKey, subjectLecturesRaw) in lecturesRaw) {
                            if (subjectLecturesRaw is Map<*, *>) {
                                val lecMap = mutableMapOf<String, Lecture>()
                                for ((lecId, lecDataRaw) in subjectLecturesRaw as Map<String, Any>) {
                                    if (lecDataRaw is Map<*, *>) {
                                        val l = lecDataRaw as Map<String, Any>
                                        val lecture = Lecture(
                                            id = lecId,
                                            lectureName = l["lectureName"] as? String,
                                            topicName = l["topicName"] as? String,
                                            videoUrl = l["videoUrl"] as? String,
                                            notesUrl = l["notesUrl"] as? String,
                                            timestamp = (l["timestamp"] as? Number)?.toLong(),
                                            uploadTime = (l["uploadTime"] as? Number)?.toLong(),
                                            classNum = (l["classNum"] as? Number)?.toInt()
                                        )
                                        lecMap[lecId] = lecture
                                    }
                                }
                                lecturesParsed[subjectKey] = lecMap
                            }
                        }
                    }

                    list.add(Batch(
                        id = batchId,
                        name = name,
                        poster = poster,
                        subjects = subjectsList,
                        lectures = lecturesParsed
                    ))
                }
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
