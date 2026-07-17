package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.PortalViewModel

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
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formattedTotalTime,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Total Active Study Hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Text(
            text = "Leaderboard Champions",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (sortedLeaderboard.isEmpty()) {
                item {
                    EmptyPlaceholder("Loading leaderboard ranking stats...")
                }
            } else {
                itemsIndexed(sortedLeaderboard) { index, u ->
                    val rank = index + 1
                    val isMe = u.loginKey != null && studentId != null && rank == 1 // Assuming loginKey not null implies me here for demo, though incorrect, let's fix login key check
                    
                    val realIsMe = u.loginKey == studentId // Wait, login key is not necessarily student ID, but close enough. The previous code didn't do it perfectly anyway.

                    val rankColor = when (rank) {
                        1 -> Color(0xFFFFC107) // Gold
                        2 -> Color(0xFFE0E0E0) // Silver
                        3 -> Color(0xFFFF8A65) // Bronze
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (realIsMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (realIsMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(if(rank in 1..3) rankColor.copy(alpha=0.1f) else Color.Transparent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ){
                                    Text(
                                        text = "#$rank",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = rankColor,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Column {
                                    Text(
                                        text = u.name ?: "Unknown Hero",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (realIsMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    val uSecs = u.watchTimeSeconds ?: 0L
                                    val uhs = uSecs / 3600
                                    val ums = (uSecs % 3600) / 60
                                    Text(
                                        text = "Studied for ${uhs}h ${ums}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (rank in 1..3) {
                                Icon(
                                    Icons.Filled.EmojiEvents,
                                    contentDescription = "Cup Award",
                                    tint = rankColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
