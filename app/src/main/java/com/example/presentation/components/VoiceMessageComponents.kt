package com.example.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppThemeTokens
import com.example.utils.DateTimeUtils
import com.example.utils.VoicePlaybackState
import com.example.utils.VoiceRecordingState
import kotlin.math.abs

@Composable
fun VoiceRecordingBar(
    recordingState: VoiceRecordingState,
    onDiscard: () -> Unit,
    onPauseResume: () -> Unit,
    onStopAndSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppThemeTokens.current
    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDiscard,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("voice_discard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Discard voice recording",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(if (recordingState.isPaused) 0.55f else pulseAlpha)
                    .clip(CircleShape)
                    .background(
                        if (recordingState.isPaused) Color(0xFFF59E0B) else Color(0xFFEF4444)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = DateTimeUtils.formatDurationMs(recordingState.elapsedMs),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(10.dp))

            val samples = recordingState.amplitudes
            val accentColor = palette.accentColor
            val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
            ) {
                val barCount = 26
                val gap = 3.dp.toPx()
                val totalGap = gap * (barCount - 1)
                val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(2.dp.toPx())
                val startIdx = (samples.size - barCount).coerceAtLeast(0)

                for (i in 0 until barCount) {
                    val sampleIdx = startIdx + i - (barCount - samples.size).coerceAtLeast(0)
                    val amp = if (sampleIdx in samples.indices) {
                        samples[sampleIdx].coerceIn(0.12f, 1f)
                    } else {
                        0.12f
                    }
                    val barHeight = (size.height * amp).coerceAtLeast(4.dp.toPx())
                    val x = i * (barWidth + gap)
                    val y = (size.height - barHeight) / 2f
                    drawRoundRect(
                        color = if (sampleIdx in samples.indices) accentColor else trackColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onPauseResume,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("voice_pause_resume_button")
            ) {
                Icon(
                    imageVector = if (recordingState.isPaused) Icons.Default.Mic else Icons.Default.Pause,
                    contentDescription = if (recordingState.isPaused) "Resume recording" else "Pause recording",
                    tint = palette.buttonColor
                )
            }

            IconButton(
                onClick = onStopAndSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.buttonColor)
                    .testTag("voice_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send voice message",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun VoiceMessageBubbleContent(
    messageId: String,
    durationMs: Long?,
    label: String?,
    playbackState: VoicePlaybackState,
    textColor: Color,
    metaColor: Color,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onCycleSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppThemeTokens.current
    val isActive = playbackState.activeMessageId == messageId
    val isPlaying = isActive && playbackState.isPlaying
    val isBuffering = isActive && playbackState.isBuffering

    val resolvedDurationMs = when {
        isActive && playbackState.durationMs > 0L -> playbackState.durationMs
        durationMs != null && durationMs > 0L -> durationMs
        else -> 0L
    }

    val progressFraction = if (isActive && resolvedDurationMs > 0L) {
        (playbackState.positionMs.toFloat() / resolvedDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val waveformBars = remember(messageId) {
        val seed = abs(messageId.hashCode())
        List(28) { idx ->
            val v = ((seed ushr (idx % 16)) + (idx * 37)) % 100
            0.22f + (v / 100f) * 0.78f
        }
    }

    val displayTime = if (isActive && (isPlaying || playbackState.positionMs > 0L)) {
        DateTimeUtils.formatDurationMs(playbackState.positionMs)
    } else {
        DateTimeUtils.formatDurationMs(resolvedDurationMs)
    }

    val speedLabel = when {
        playbackState.playbackSpeed >= 1.9f -> "2x"
        playbackState.playbackSpeed >= 1.4f -> "1.5x"
        else -> "1x"
    }

    Column(modifier = modifier.widthIn(min = 215.dp, max = 270.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(palette.accentColor)
                    .clickable(onClick = onTogglePlay)
                    .testTag("voice_play_toggle_$messageId"),
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause voice message" else "Play voice message",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                val playedColor = palette.accentColor
                val unplayedColor = textColor.copy(alpha = 0.25f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(messageId, isActive) {
                            detectTapGestures { offset ->
                                if (isActive && size.width > 0) {
                                    onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                                } else {
                                    onTogglePlay()
                                }
                            }
                        }
                ) {
                    val count = waveformBars.size
                    val gap = 2.5.dp.toPx()
                    val barW = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
                    waveformBars.forEachIndexed { idx, amp ->
                        val frac = idx.toFloat() / count.toFloat()
                        val barH = (size.height * amp).coerceAtLeast(4.dp.toPx())
                        val x = idx * (barW + gap)
                        val y = (size.height - barH) / 2f
                        drawRoundRect(
                            color = if (frac <= progressFraction && (isActive || progressFraction > 0f)) {
                                playedColor
                            } else {
                                unplayedColor
                            },
                            topLeft = Offset(x, y),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = palette.accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = label?.takeIf { it.isNotBlank() && !it.startsWith("voice_") } ?: displayTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = metaColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!label.isNullOrBlank() && !label.startsWith("voice_")) {
                            Text(
                                text = " · $displayTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = metaColor,
                                maxLines = 1
                            )
                        }
                    }

                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.accentColor.copy(alpha = 0.18f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onCycleSpeed)
                        ) {
                            Text(
                                text = speedLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.buttonColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
