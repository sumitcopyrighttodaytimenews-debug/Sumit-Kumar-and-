package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Lecture
import com.example.ui.components.CustomVideoPlayer
import com.example.viewmodel.PortalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LecturesScreen(viewModel: PortalViewModel) {
    val batchNullable by viewModel.selectedBatch.collectAsState()
    val batch = batchNullable ?: return

    val subjectNullable by viewModel.selectedSubject.collectAsState()
    val subject = subjectNullable ?: return

    val currentPlayingVideo by viewModel.currentPlayingVideo.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    var activeSubTab by remember { mutableIntStateOf(0) }

    val safeSubject = remember(subject) { subject.replace("[.#\$\\[\\]]".toRegex(), "") }

    val lectures = remember(batch, safeSubject) {
        val rawLectures = batch.lectures?.get(safeSubject) ?: emptyMap()
        rawLectures.values.toList().sortedBy { it.uploadTime ?: 0L }
    }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = currentPlayingVideo != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
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
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = activeVideo.lectureName ?: "",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        activeVideo.topicName?.let {
                            Text(
                                text = "Topic: $it",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 24.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            val tabs = listOf("LECTURES", "NOTES", "PNGS", "DPP PDFS")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeSubTab == index,
                    onClick = { activeSubTab = index },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(title, modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background)) {
            when (activeSubTab) {
                0 -> {
                    if (lectures.isEmpty()) {
                        EmptyPlaceholder(message = "No lecture streams loaded yet.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
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

                                val isPlaying = currentPlayingVideo?.id == lecture.id

                                Card(
                                    onClick = { viewModel.playVideo(lecture) },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f) else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(120.dp)
                                                .aspectRatio(16f / 9f)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Class ${lecture.classNum ?: ""}: ${lecture.lectureName ?: ""}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formattedDate,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (lecture.notesUrl != null) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lecture.notesUrl))
                                                            context.startActivity(intent)
                                                        }.padding(4.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Description, "PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier=Modifier.width(4.dp))
                                                        Text("Notes", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.width(1.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.toggleBookmark(lecture, formattedDate, "")
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Bookmark,
                                                        contentDescription = "Save Bookmarks",
                                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f),
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
                }
                1 -> {
                    val lecturesWithNotes = lectures.filter { it.notesUrl != null }
                    if (lecturesWithNotes.isEmpty()) {
                        EmptyPlaceholder(message = "Notes PDF study files will appear here.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
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
fun InteractiveAttachmentCard(title: String, type: String, icon: ImageVector, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ){
                    Icon(icon, contentDescription = "attachment", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                
                Column {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(type, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "View", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
