package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.MediaPlayer
import android.os.Build
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.*
import com.example.viewmodel.PortalViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PortalApp(viewModel: PortalViewModel) {
    val studentId by viewModel.studentId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    // Trigger local Android toasts on state updates
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            MainAppScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainAppScreen(viewModel: PortalViewModel) {
    val studentName by viewModel.studentName.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val selectedBatchState by viewModel.selectedBatch.collectAsState()
    val selectedSubjectState by viewModel.selectedSubject.collectAsState()
    val currentPlayingVideo by viewModel.currentPlayingVideo.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Batches, 1 = Leaderboard, 2 = Bookmarks

    Column(modifier = Modifier.fillMaxSize()) {
        // App Headers based on routing
        when {
            currentPlayingVideo != null -> {
                HeaderBar(
                    title = selectedSubjectState ?: "Classroom",
                    onBack = { viewModel.stopPlaying() }
                )
            }
            selectedSubjectState != null -> {
                HeaderBar(
                    title = selectedSubjectState ?: "Subject",
                    onBack = { viewModel.clearSelectedSubject() }
                )
            }
            selectedBatchState != null -> {
                HeaderBar(
                    title = selectedBatchState?.name ?: "Subjects",
                    onBack = { viewModel.clearSelectedBatch() }
                )
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📚 My Classes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Agya seekhne? Badhiya! ${studentName ?: "Student"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(onClick = { viewModel.loadLeaderboard(); activeTab = 1 }) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = "Leaderboard", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.loadBookmarksAndWatchMetrics(); activeTab = 2 }) {
                            Icon(Icons.Filled.Bookmark, contentDescription = "Bookmarks", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Global Notice Board
        notice?.text?.let { noticeText ->
            if (selectedSubjectState == null && currentPlayingVideo == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Campaign,
                        contentDescription = "Notice",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = noticeText,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee()
                    )
                }
            }
        }

        // Screen routing content
        Box(modifier = Modifier.weight(1f)) {
            when {
                selectedSubjectState != null -> {
                    LecturesScreen(viewModel)
                }
                selectedBatchState != null -> {
                    SubjectsScreen(viewModel)
                }
                else -> {
                    when (activeTab) {
                        0 -> BatchesScreen(viewModel)
                        1 -> LeaderboardScreen(viewModel)
                        2 -> BookmarksScreen(viewModel)
                    }
                }
            }
        }

        // Footer Bottom bar navigation (on root screens only)
        if (selectedBatchState == null && selectedSubjectState == null && currentPlayingVideo == null) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0; viewModel.loadBatches() },
                    icon = { Icon(Icons.Filled.PlayArrow, "Classes") },
                    label = { Text("Batches") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1; viewModel.loadLeaderboard() },
                    icon = { Icon(Icons.Filled.EmojiEvents, "Leaderboard") },
                    label = { Text("Leaderboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2; viewModel.loadBookmarksAndWatchMetrics() },
                    icon = { Icon(Icons.Filled.Bookmark, "Saved") },
                    label = { Text("Bookmarks") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun HeaderBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun AuthScreen(viewModel: PortalViewModel) {
    val isLoginMode by viewModel.isLoginMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var mobile by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var isKeyVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Classes Portal",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(45.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isLoginMode) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "Enter your 16-digit billing Secret Key to access your video classes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = key,
                    onValueChange = { if (it.length <= 16) key = it.uppercase() },
                    placeholder = { Text("e.g. 80C3H7I9WAM4TS7P", color = Color.Gray, fontSize = 14.sp) },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                if (isKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle key visible",
                                tint = Color.Gray
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("secret_key_input")
                )

                Button(
                    onClick = { viewModel.login(key) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Login to Classes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New Student? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(
                        text = "Create Account",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { viewModel.toggleAuthMode() }
                    )
                }

            } else {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = "Fill profile detail to buy details and obtain portal entrance card.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Full Name", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                )

                TextField(
                    value = dob,
                    onValueChange = { dob = it },
                    placeholder = { Text("Date of Birth (DD-MM-YYYY)", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                )

                TextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    placeholder = { Text("Father's Name", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                )

                TextField(
                    value = mobile,
                    onValueChange = { if (it.length <= 10) mobile = it },
                    placeholder = { Text("10-Digit Mobile Number", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                )

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email Address", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { viewModel.register(name, dob, fatherName, mobile, email) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Register Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already registered? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(
                        text = "Login Here",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { viewModel.toggleAuthMode() }
                    )
                }
            }
        }
    }
}

@Composable
fun BatchesScreen(viewModel: PortalViewModel) {
    val batches by viewModel.batches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (batches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "None", tint = Color.Gray, modifier = Modifier.size(48.dp))
                Text("No available batches at this time.", color = Color.Gray, fontSize = 14.sp)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(batches) { batch ->
                Card(
                    onClick = { viewModel.selectBatch(batch) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (batch.poster != null) {
                            AsyncImage(
                                model = batch.poster,
                                contentDescription = "Poster",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .background(Color.DarkGray)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Online Class portal", color = Color.Gray)
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = batch.name ?: "Course Batch",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val subjectsCount = when (val s = batch.subjects) {
                                is List<*> -> s.size
                                is Map<*, *> -> s.size
                                else -> 0
                            }
                            Text(
                                text = "$subjectsCount Subject(s) Available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun SubjectsScreen(viewModel: PortalViewModel) {
    val batchNullable by viewModel.selectedBatch.collectAsState()
    val batch = batchNullable ?: return

    val subjectsList = remember(batch) {
        when (val s = batch.subjects) {
            is List<*> -> s.filterIsInstance<String>()
            is Map<*, *> -> s.values.filterIsInstance<String>()
            else -> emptyList()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(subjectsList) { subject ->
            Card(
                onClick = { viewModel.selectSubject(subject) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LecturesScreen(viewModel: PortalViewModel) {
    val batchNullable by viewModel.selectedBatch.collectAsState()
    val batch = batchNullable ?: return

    val subjectNullable by viewModel.selectedSubject.collectAsState()
    val subject = subjectNullable ?: return

    val currentPlayingVideo by viewModel.currentPlayingVideo.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = LECTURES, 1 = NOTES, 2 = PNG, 3 = DPP

    val safeSubject = remember(subject) { subject.replace("[.#$\\[\\]]".toRegex(), "") }

    val lectures = remember(batch, safeSubject) {
        val rawLectures = batch.lectures?.get(safeSubject) ?: emptyMap()
        rawLectures.values.toList().sortedBy { it.uploadTime ?: 0L }
    }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = currentPlayingVideo != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            currentPlayingVideo?.let { activeVideo ->
                Column {
                    CustomVideoPlayer(
                        videoUrl = activeVideo.videoUrl ?: "",
                        title = activeVideo.lectureName ?: "Video Class",
                        onProgressSave = { currentSecs, percentage ->
                            viewModel.saveProgressLocal(currentSecs, percentage)
                        }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = activeVideo.lectureName ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        activeVideo.topicName?.let {
                            Text(
                                text = "Topic: $it",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = Color.White,
            edgePadding = 16.dp
        ) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                Text("LECTURES", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                Text("NOTES", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Tab(selected = activeSubTab == 2, onClick = { activeSubTab = 2 }) {
                Text("PNGS", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Tab(selected = activeSubTab == 3, onClick = { activeSubTab = 3 }) {
                Text("DPP PDFS", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                0 -> {
                    if (lectures.isEmpty()) {
                        EmptyPlaceholder(message = "No lecture streams loaded yet.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(lectures) { lecture ->
                                val isBookmarked = bookmarks.any { it.id == lecture.id }
                                val formattedDate = remember(lecture) {
                                    lecture.timestamp?.let {
                                        val d = Date(it)
                                        val f = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                        f.format(d)
                                    } ?: "Live Session"
                                }

                                Card(
                                    onClick = { viewModel.playVideo(lecture) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (currentPlayingVideo?.id == lecture.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (currentPlayingVideo?.id == lecture.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(110.dp)
                                                .aspectRatio(16f / 9f)
                                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Class ${lecture.classNum ?: ""}: ${lecture.lectureName ?: ""}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formattedDate,
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (lecture.notesUrl != null) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lecture.notesUrl))
                                                            context.startActivity(intent)
                                                        }
                                                    ) {
                                                        Icon(Icons.Filled.Description, "PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                        Text(" Notes", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.width(1.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.toggleBookmark(lecture, formattedDate, "")
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Bookmark,
                                                        contentDescription = "Save Bookmarks",
                                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.Gray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val lecturesWithNotes = lectures.filter { it.notesUrl != null }
                    if (lecturesWithNotes.isEmpty()) {
                        EmptyPlaceholder(message = "Notes PDF study files will appear here.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(lecturesWithNotes) { lecture ->
                                InteractiveAttachmentCard(
                                    title = "Class Notes: ${lecture.lectureName}",
                                    type = "PDF Study Sheet",
                                    icon = Icons.Filled.Description,
                                    onOpen = {
                                        lecture.notesUrl?.let {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    EmptyPlaceholder(message = "PNG illustrations and whiteboard diagrams will be loaded here.")
                }
                3 -> {
                    EmptyPlaceholder(message = "Daily Practice Problem (DPP) assignments will look here.")
                }
            }
        }
    }
}

@Composable
fun InteractiveAttachmentCard(title: String, type: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = "attachment", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(type, color = Color.Gray, fontSize = 11.sp)
                }
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "View", tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun EmptyPlaceholder(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun LeaderboardScreen(viewModel: PortalViewModel) {
    val totalSeconds by viewModel.totalWatchTime.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val studentId by viewModel.studentId.collectAsState()

    val formattedTotalTime = remember(totalSeconds) {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        "${h}h ${m}m"
    }

    val sortedLeaderboard = remember(leaderboard) {
        leaderboard.sortedByDescending { it.watchTimeSeconds ?: 0L }.take(10)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1F1F22), Color(0xFF151518))
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formattedTotalTime,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Your Total Active Study Hours",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Text(
            text = "Leaderboard Champions This Week",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (sortedLeaderboard.isEmpty()) {
                item {
                    EmptyPlaceholder("Loading leaderboard ranking stats...")
                }
            } else {
                itemsIndexed(sortedLeaderboard) { index, u ->
                    val rank = index + 1
                    val isMe = u.loginKey != null && studentId != null

                    val rankColor = when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> Color.Gray
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "#$rank",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = rankColor,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )

                                Column {
                                    Text(
                                        text = u.name ?: "Unknown Hero",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) MaterialTheme.colorScheme.primary else Color.White,
                                        fontSize = 13.sp
                                    )
                                    val uSecs = u.watchTimeSeconds ?: 0L
                                    val uhs = uSecs / 3600
                                    val ums = (uSecs % 3600) / 60
                                    Text(
                                        text = "Studied for ${uhs}h ${ums}m",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (rank in 1..3) {
                                Icon(
                                    Icons.Filled.EmojiEvents,
                                    contentDescription = "Cup Award",
                                    tint = rankColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarksScreen(viewModel: PortalViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (bookmarks.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Bookmark, "Blank Bookmarks", tint = Color.DarkGray, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No items saved yet", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Click bookmark key on video lectures timeline to keep track.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
        } else {
            items(bookmarks) { bookmark ->
                Card(
                    onClick = {
                        val l = Lecture(
                            id = bookmark.id,
                            lectureName = bookmark.title,
                            videoUrl = bookmark.videoUrl
                        )
                        viewModel.playVideo(l)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .aspectRatio(16f / 9f)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                bookmark.title ?: "",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Subject: " + (bookmark.subject ?: "General"),
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                "Saved on: " + (bookmark.date ?: ""),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.toggleBookmarkGlobal(bookmark) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Remove Bookmark", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomVideoPlayer(
    videoUrl: String,
    title: String,
    onProgressSave: (currentSecs: Double, percentage: Double) -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    var customSpeed by remember { mutableFloatStateOf(1f) }
    var isControlsVisible by remember { mutableStateOf(true) }

    var mediaRefs by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRefs by remember { mutableStateOf<VideoView?>(null) }

    // Toggle overlay visibility loop
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3500)
            isControlsVisible = false
        }
    }

    // High performance updates ticker to trigger callback saves
    LaunchedEffect(isPlaying, videoUrl) {
        while (isPlaying) {
            delay(1000)
            videoViewRefs?.let { vv ->
                val curr = vv.currentPosition.toLong()
                val d = vv.duration.toLong()
                currentPosition = curr
                if (d > 0) {
                    duration = d
                    onProgressSave(curr / 1000.0, (curr.toDouble() / d.toDouble()) * 100.0)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .pointerInput(isPlaying) {
                detectTapGestures(
                    onTap = {
                        isControlsVisible = !isControlsVisible
                    },
                    onLongPress = {
                        mediaRefs?.let { mp ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    mp.playbackParams = mp.playbackParams.setSpeed(2.0f)
                                    customSpeed = 2.0f
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    },
                    onDoubleTap = { offset ->
                        val rectWidth = size.width
                        val clickX = offset.x
                        videoViewRefs?.let { vv ->
                            if (clickX < rectWidth * 0.4f) {
                                vv.seekTo(maxOf(0, vv.currentPosition - 10000))
                                currentPosition = vv.currentPosition.toLong()
                            } else if (clickX > rectWidth * 0.6f) {
                                vv.seekTo(minOf(vv.duration, vv.currentPosition + 10000))
                                currentPosition = vv.currentPosition.toLong()
                            }
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    val videoView = VideoView(ctx).apply {
                        setVideoPath(videoUrl)
                        setOnPreparedListener { mp ->
                            mediaRefs = mp
                            duration = mp.duration.toLong()
                            mp.isLooping = false
                            mp.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                        }
                        setOnCompletionListener {
                            isPlaying = false
                        }
                    }
                    videoViewRefs = videoView
                    addView(videoView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                }
            },
            update = { _ -> },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            videoViewRefs?.let { vv ->
                                vv.seekTo(maxOf(0, vv.currentPosition - 10000))
                                currentPosition = vv.currentPosition.toLong()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            videoViewRefs?.let { vv ->
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            videoViewRefs?.let { vv ->
                                vv.seekTo(minOf(vv.duration, vv.currentPosition + 10000))
                                currentPosition = vv.currentPosition.toLong()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    val progressFraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                    Slider(
                        value = progressFraction,
                        onValueChange = { newVal ->
                            val target = (duration * newVal).toLong()
                            videoViewRefs?.seekTo(target.toInt())
                            currentPosition = target
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Red,
                            activeTrackColor = Color.Red,
                            inactiveTrackColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentStr = remember(currentPosition) {
                            val totalSecs = currentPosition / 1000
                            val m = totalSecs / 60
                            val s = totalSecs % 60
                            String.format(Locale.getDefault(), "%d:%02d", m, s)
                        }

                        val durationStr = remember(duration) {
                            val totalSecs = duration / 1000
                            val m = totalSecs / 60
                            val s = totalSecs % 60
                            String.format(Locale.getDefault(), "%d:%02d", m, s)
                        }

                        Text(
                            text = "$currentStr / $durationStr",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    mediaRefs?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    contentDescription = "sound",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "${customSpeed}x",
                                color = if (customSpeed > 1f) MaterialTheme.colorScheme.primary else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)
                                        val nextIdx = (speeds.indexOf(customSpeed) + 1) % speeds.size
                                        val newSpeed = speeds[nextIdx]
                                        mediaRefs?.let { mp ->
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                try {
                                                    mp.playbackParams = mp.playbackParams.setSpeed(newSpeed)
                                                } catch (e: Exception) { e.printStackTrace() }
                                            }
                                        }
                                        customSpeed = newSpeed
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (customSpeed == 2.0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Holding 2X Speed >>",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
