package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.viewmodel.PortalViewModel

@Composable
fun PortalApp(viewModel: PortalViewModel) {
    val studentId by viewModel.studentId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // We removed AuthScreen manually earlier, keeping MainAppScreen only.
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

    var activeTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            currentPlayingVideo != null -> {
                PremiumHeaderBar(
                    title = selectedSubjectState ?: "Classroom",
                    onBack = { viewModel.stopPlaying() }
                )
            }
            selectedSubjectState != null -> {
                PremiumHeaderBar(
                    title = selectedSubjectState ?: "Subject",
                    onBack = { viewModel.clearSelectedSubject() }
                )
            }
            selectedBatchState != null -> {
                PremiumHeaderBar(
                    title = selectedBatchState?.name ?: "Subjects",
                    onBack = { viewModel.clearSelectedBatch() }
                )
            }
            else -> {
                PremiumAppTopBar(
                    studentName = studentName ?: "Student",
                    onLeaderboardClick = { viewModel.loadLeaderboard(); activeTab = 1 },
                    onBookmarksClick = { viewModel.loadBookmarksAndWatchMetrics(); activeTab = 2 }
                )
            }
        }

        // Global Notice
        notice?.text?.let { noticeText ->
            if (selectedSubjectState == null && currentPlayingVideo == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Campaign, contentDescription = "Notice", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = noticeText,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).basicMarquee()
                    )
                }
            }
        }

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

        if (selectedBatchState == null && selectedSubjectState == null && currentPlayingVideo == null) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
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
fun PremiumAppTopBar(
    studentName: String,
    onLeaderboardClick: () -> Unit,
    onBookmarksClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome Back,",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = studentName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onLeaderboardClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = "Leaderboard", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = onBookmarksClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Filled.Bookmark, contentDescription = "Bookmarks", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PremiumHeaderBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
