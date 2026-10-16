package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Track
import com.example.ui.components.TrackArtworkView
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    durationMs: Long,
    currentPositionFlow: StateFlow<Long>,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swipeUpOffset by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        swipeUpOffset += dragAmount
                    },
                    onDragEnd = {
                        if (swipeUpOffset < -30f) {
                            onExpand()
                        }
                        swipeUpOffset = 0f
                    },
                    onDragCancel = {
                        swipeUpOffset = 0f
                    }
                )
            }
            .clickable(onClick = onExpand)
            .testTag("mini_player"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            // Isolated progress bar: only this 3.5dp bar recomposes on position ticks
            MiniPlayerProgressBar(
                currentPositionFlow = currentPositionFlow,
                durationMs = durationMs
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art / Logo with embedded artwork support
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtworkView(
                        track = track,
                        modifier = Modifier.fillMaxSize(),
                        placeholderIconSize = 34.dp,
                        targetSizePx = 144,
                        contentScale = ContentScale.Crop,
                        contentDescription = track.title
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPlaying) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StaticWaveformIndicator()
                        }
                    }
                }

                // Previous Track Button
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("mini_skip_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Play / Pause Button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
                        .testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Next Button
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("mini_skip_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    currentPositionFlow: StateFlow<Long>,
    durationMs: Long
) {
    val currentPositionMs by currentPositionFlow.collectAsStateWithLifecycle()
    val progress = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.5.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
    )
}

@Composable
private fun StaticWaveformIndicator() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.size(width = 16.dp, height = 14.dp)) {
        val barWidth = 3.dp.toPx()
        val spacing = (size.width - barWidth * 3) / 2
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        val b1H = size.height * 0.55f
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(0f, size.height - b1H),
            size = Size(barWidth, b1H),
            cornerRadius = cornerRadius
        )

        val b2H = size.height * 0.95f
        drawRoundRect(
            color = secondaryColor,
            topLeft = Offset(barWidth + spacing, size.height - b2H),
            size = Size(barWidth, b2H),
            cornerRadius = cornerRadius
        )

        val b3H = size.height * 0.70f
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset((barWidth + spacing) * 2, size.height - b3H),
            size = Size(barWidth, b3H),
            cornerRadius = cornerRadius
        )
    }
}