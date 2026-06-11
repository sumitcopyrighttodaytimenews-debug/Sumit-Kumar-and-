package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.PortalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PortalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PortalRepository()
    private val sharedPrefs = application.getSharedPreferences("student_portal_prefs", Context.MODE_PRIVATE)

    // Current logged-in student info
    private val _studentId = MutableStateFlow<String?>(sharedPrefs.getString("studentId", null))
    val studentId: StateFlow<String?> = _studentId.asStateFlow()

    private val _studentName = MutableStateFlow<String?>(sharedPrefs.getString("studentName", null))
    val studentName: StateFlow<String?> = _studentName.asStateFlow()

    // Screen states
    private val _notice = MutableStateFlow<NoticeBoard?>(null)
    val notice: StateFlow<NoticeBoard?> = _notice.asStateFlow()

    private val _batches = MutableStateFlow<List<Batch>>(emptyList())
    val batches: StateFlow<List<Batch>> = _batches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _totalWatchTime = MutableStateFlow<Long>(0L)
    val totalWatchTime: StateFlow<Long> = _totalWatchTime.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<User>>(emptyList())
    val leaderboard: StateFlow<List<User>> = _leaderboard.asStateFlow()

    // Navigation and Current state
    private val _selectedBatch = MutableStateFlow<Batch?>(null)
    val selectedBatch: StateFlow<Batch?> = _selectedBatch.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    // Current playing video details
    private val _currentPlayingVideo = MutableStateFlow<Lecture?>(null)
    val currentPlayingVideo: StateFlow<Lecture?> = _currentPlayingVideo.asStateFlow()

    // Auth screen mode: True = Login, False = Registration
    private val _isLoginMode = MutableStateFlow(true)
    val isLoginMode: StateFlow<Boolean> = _isLoginMode.asStateFlow()

    private var watchTimeJob: Job? = null

    init {
        fetchGlobalNotice()
        if (_studentId.value != null) {
            loadBatches()
            loadBookmarksAndWatchMetrics()
            loadLeaderboard()
        }
    }

    fun toggleAuthMode() {
        _isLoginMode.value = !_isLoginMode.value
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun fetchGlobalNotice() {
        viewModelScope.launch {
            _notice.value = repository.getNoticeBoard()
        }
    }

    fun loadBatches() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _batches.value = repository.getBatches()
            } catch (e: Exception) {
                showToast("Connection failed. Check connectivity.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectBatch(batch: Batch) {
        _selectedBatch.value = batch
        _selectedSubject.value = null
    }

    fun clearSelectedBatch() {
        _selectedBatch.value = null
        _selectedSubject.value = null
        _currentPlayingVideo.value = null
        stopWatchTimer()
    }

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
    }

    fun clearSelectedSubject() {
        _selectedSubject.value = null
        _currentPlayingVideo.value = null
        stopWatchTimer()
    }

    fun register(name: String, dob: String, fatherName: String, mobile: String, email: String) {
        if (name.isEmpty() || dob.isEmpty() || fatherName.isEmpty() || mobile.isEmpty() || email.isEmpty()) {
            showToast("Please fill all fields")
            return
        }
        if (mobile.length != 10) {
            showToast("Mobile number must be 10 digits")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val existing = repository.getUserByMobile(mobile)
            if (existing != null && existing.isNotEmpty()) {
                showToast("Mobile already registered! Please Login")
                _isLoginMode.value = true
                _isLoading.value = false
                return@launch
            }

            val allowedChars = ('A'..'Z') + ('0'..'9')
            val secretKey = (1..16).map { allowedChars.random() }.joinToString("")

            val user = User(
                name = name,
                dob = dob,
                fatherName = fatherName,
                mobile = mobile,
                email = email,
                loginKey = secretKey,
                watchTimeSeconds = 0L,
                joinedAt = System.currentTimeMillis()
            )

            val newId = repository.registerUser(user)
            if (newId != null) {
                showToast("Register OK! Key: $secretKey. Copy to LOGIN.")
                _isLoginMode.value = true
            } else {
                showToast("Failed to register. Please retry.")
            }
            _isLoading.value = false
        }
    }

    fun login(secretKey: String) {
        val trimmed = secretKey.trim().uppercase()
        if (trimmed.length != 16) {
            showToast("Please enter 16-digit Secret Key")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val results = repository.getUserByLoginKey(trimmed)
            if (results != null && results.isNotEmpty()) {
                val userId = results.keys.first()
                val userData = results.values.first()

                sharedPrefs.edit()
                    .putString("studentId", userId)
                    .putString("studentName", userData.name)
                    .apply()

                _studentId.value = userId
                _studentName.value = userData.name

                showToast("Welcome ${userData.name}!")
                loadBatches()
                loadBookmarksAndWatchMetrics()
                loadLeaderboard()
            } else {
                showToast("Invalid key. Please check your invoice.")
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        sharedPrefs.edit().clear().apply()
        _studentId.value = null
        _studentName.value = null
        _selectedBatch.value = null
        _selectedSubject.value = null
        _currentPlayingVideo.value = null
        stopWatchTimer()
    }

    fun loadBookmarksAndWatchMetrics() {
        val uid = _studentId.value ?: return
        viewModelScope.launch {
            _bookmarks.value = repository.getUserBookmarks(uid)
            _totalWatchTime.value = repository.getWatchTime(uid)
        }
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _leaderboard.value = repository.getTopUsers()
        }
    }

    fun toggleBookmark(lecture: Lecture, date: String, posterUrl: String) {
        val uid = _studentId.value ?: return
        val batch = _selectedBatch.value ?: return
        val subjectName = _selectedSubject.value ?: "General"
        
        viewModelScope.launch {
            val existing = _bookmarks.value.find { it.id == lecture.id }
            if (existing != null) {
                repository.deleteBookmark(uid, lecture.id)
                showToast("Removed from bookmarks")
            } else {
                val b = Bookmark(
                    id = lecture.id,
                    title = lecture.lectureName ?: "Lecture",
                    date = date,
                    videoUrl = lecture.videoUrl,
                    posterUrl = posterUrl,
                    batchId = batch.id,
                    subject = subjectName,
                    savedAt = System.currentTimeMillis()
                )
                repository.addBookmark(uid, lecture.id, b)
                showToast("Saved to bookmarks")
            }
            _bookmarks.value = repository.getUserBookmarks(uid)
        }
    }

    fun toggleBookmarkGlobal(bookmark: Bookmark) {
        val uid = _studentId.value ?: return
        viewModelScope.launch {
            repository.deleteBookmark(uid, bookmark.id)
            showToast("Removed from bookmarks")
            _bookmarks.value = repository.getUserBookmarks(uid)
        }
    }

    fun playVideo(lecture: Lecture) {
        _currentPlayingVideo.value = lecture
        startWatchTimer(lecture.id)
    }

    fun stopPlaying() {
        _currentPlayingVideo.value = null
        stopWatchTimer()
    }

    fun saveProgressLocal(currentTimeSecs: Double, percentage: Double) {
        val uid = _studentId.value ?: return
        val batch = _selectedBatch.value ?: return
        val subject = _selectedSubject.value ?: "General"
        val lecture = _currentPlayingVideo.value ?: return

        viewModelScope.launch {
            repository.saveProgress(
                userId = uid,
                batchId = batch.id,
                subject = subject,
                lectureId = lecture.id,
                progress = UserProgress(
                    currentTime = currentTimeSecs,
                    percentage = percentage,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    private fun startWatchTimer(lectureId: String) {
        stopWatchTimer()
        val uid = _studentId.value ?: return
        watchTimeJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                repository.incrementWatchTime(uid, 5L)
                _totalWatchTime.value += 5L
            }
        }
    }

    private fun stopWatchTimer() {
        watchTimeJob?.cancel()
        watchTimeJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopWatchTimer()
    }
}
