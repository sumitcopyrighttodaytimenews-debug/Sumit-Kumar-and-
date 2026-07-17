package com.example.ui.components

import android.media.MediaPlayer
import android.os.Build
import android.widget.FrameLayout
import android.widget.VideoView
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.util.Locale

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

    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = isFullScreen) {
        isFullScreen = false
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3500)
            isControlsVisible = false
        }
    }

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

    var showForwardRipple by remember { mutableStateOf(false) }
    var showRewindRipple by remember { mutableStateOf(false) }

    LaunchedEffect(showForwardRipple) {
        if (showForwardRipple) {
            delay(500)
            showForwardRipple = false
        }
    }
    LaunchedEffect(showRewindRipple) {
        if (showRewindRipple) {
            delay(500)
            showRewindRipple = false
        }
    }

    val playerContent = remember {
        movableContentOf {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                                        showRewindRipple = true
                                    } else if (clickX > rectWidth * 0.6f) {
                                        vv.seekTo(minOf(vv.duration, vv.currentPosition + 10000))
                                        currentPosition = vv.currentPosition.toLong()
                                        showForwardRipple = true
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
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isFullScreen) {
                                    IconButton(
                                        onClick = {
                                            isFullScreen = false
                                            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Exit Fullscreen", tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            var showSettings by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showSettings = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showSettings,
                                    onDismissRequest = { showSettings = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Playback Speed (${customSpeed}x)") },
                                        onClick = {
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
                                            showSettings = false
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Quality (Auto)") },
                                        onClick = { showSettings = false },
                                        leadingIcon = { Icon(Icons.Filled.HighQuality, contentDescription = null) }
                                    )
                                }
                            }
                        }

                        // Middle Controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    videoViewRefs?.let { vv ->
                                        vv.seekTo(maxOf(0, vv.currentPosition - 10000))
                                        currentPosition = vv.currentPosition.toLong()
                                        showRewindRipple = true
                                    }
                                },
                                modifier = Modifier.size(56.dp)
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
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
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
                                        showForwardRipple = true
                                    }
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Filled.FastForward,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Bottom Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            isFullScreen = !isFullScreen
                                            (context as? Activity)?.requestedOrientation = if (isFullScreen) {
                                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            } else {
                                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                            contentDescription = "fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

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
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                            )
                        }
                    }
                }

                // Double tap ripples
                if (showRewindRipple) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.4f)
                            .align(Alignment.CenterStart)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(topEnd = 100.dp, bottomEnd = 100.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Text("10 seconds", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                if (showForwardRipple) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.4f)
                            .align(Alignment.CenterEnd)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Text("10 seconds", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                if (customSpeed == 2.0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Holding 2X Speed >>",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (isFullScreen) {
        Dialog(
            onDismissRequest = {
                isFullScreen = false
                (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                decorFitsSystemWindows = false
            )
        ) {
            playerContent()
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            playerContent()
        }
    }
}
