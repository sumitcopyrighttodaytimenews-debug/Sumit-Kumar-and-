package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.viewmodel.PortalViewModel

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
                Icon(Icons.Filled.PlayArrow, contentDescription = "None", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No available batches at this time.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(batches) { batch ->
                Card(
                    onClick = { viewModel.selectBatch(batch) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)
                                        )
                                    )
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Edu Premium Class", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge)
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = batch.name ?: "Course Batch",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val subjectsCount = when (val s = batch.subjects) {
                                is List<*> -> s.size
                                is Map<*, *> -> s.size
                                else -> 0
                            }
                            Text(
                                text = "$subjectsCount Subject(s) Available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
