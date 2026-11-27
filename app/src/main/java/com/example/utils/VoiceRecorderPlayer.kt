package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitudeNormalized: Float = 0f,
    val amplitudes: List<Float> = emptyList(),
    val recordedFile: File? = null
)

data class VoicePlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val errorMessage: String? = null
)

class VoiceRecorderPlayer(private val appContext: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingJob: Job? = null
    private var segmentStartTimeMs: Long = 0L
    private var accumulatedElapsedMs: Long = 0L
    private val amplitudeHistory = ArrayDeque<Float>(MAX_WAVEFORM_BARS)

    private var player: MediaPlayer? = null
    private var playbackJob: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _recordingState = MutableStateFlow(VoiceRecordingState())
    val recordingState: StateFlow<VoiceRecordingState> = _recordingState.asStateFlow()

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    fun startRecording(): Result<Unit> {
        return try {
            stopPlayback()
            releaseRecorder()
            val dir = File(appContext.cacheDir, "voice").apply { mkdirs() }
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            accumulatedElapsedMs = 0L
            amplitudeHistory.clear()

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            segmentStartTimeMs = System.currentTimeMillis()
            _recordingState.value = VoiceRecordingState(
                isRecording = true,
                isPaused = false,
                elapsedMs = 0L
            )
            startAmplitudeTicker()
            Result.success(Unit)
        } catch (e: Exception) {
            releaseRecorder()
            outputFile?.delete()
            outputFile = null
            _recordingState.value = VoiceRecordingState()
            Result.failure(e)
        }
    }

    fun pauseRecording() {
        val current = _recordingState.value
        val activeRecorder = recorder ?: return
        if (!current.isRecording || current.isPaused) return
        runCatching {
            activeRecorder.pause()
            accumulatedElapsedMs += (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
            recordingJob?.cancel()
            recordingJob = null
            _recordingState.value = current.copy(
                isPaused = true,
                elapsedMs = accumulatedElapsedMs,
                amplitudeNormalized = 0f
            )
        }
    }

    fun resumeRecording() {
        val current = _recordingState.value
        val activeRecorder = recorder ?: return
        if (!current.isRecording || !current.isPaused) return
        runCatching {
            activeRecorder.resume()
            segmentStartTimeMs = System.currentTimeMillis()
            _recordingState.value = current.copy(isPaused = false)
            startAmplitudeTicker()
        }
    }

    fun stopRecording(): Result<Pair<File, Long>> {
        val current = _recordingState.value
        val elapsed = if (current.isPaused) {
            accumulatedElapsedMs
        } else {
            accumulatedElapsedMs + (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
        }
        val file = outputFile
        return try {
            recordingJob?.cancel()
            recordingJob = null
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile = null
            _recordingState.value = VoiceRecordingState(
                isRecording = false,
                isPaused = false,
                elapsedMs = elapsed,
                recordedFile = file
            )
            if (file == null || !file.exists() || file.length() <= 0L || elapsed < 350L) {
                file?.delete()
                Result.failure(IllegalStateException("Voice recording was too short. Hold or speak for at least 1 second."))
            } else {
                Result.success(file to elapsed.coerceAtLeast(500L))
            }
        } catch (_: Exception) {
            releaseRecorder()
            file?.delete()
            outputFile = null
            _recordingState.value = VoiceRecordingState()
            Result.failure(IllegalStateException("Voice recording was too short. Please try again."))
        }
    }

    fun cancelRecording() {
        releaseRecorder()
        outputFile?.delete()
        outputFile = null
        accumulatedElapsedMs = 0L
        amplitudeHistory.clear()
        _recordingState.value = VoiceRecordingState()
    }

    fun togglePlay(messageId: String, sourceUrlOrPath: String) {
        if (sourceUrlOrPath.isBlank()) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "Audio file is unavailable."
            )
            return
        }
        val current = _playbackState.value
        if (current.activeMessageId == messageId && current.isBuffering) {
            stopPlayback()
            return
        }
        if (current.activeMessageId == messageId && current.isPlaying) {
            runCatching { player?.pause() }
            _playbackState.value = current.copy(isPlaying = false, errorMessage = null)
            return
        }
        if (current.activeMessageId == messageId && !current.isPlaying && player != null) {
            runCatching {
                applyPlaybackSpeed(player, current.playbackSpeed)
                player?.start()
                _playbackState.value = current.copy(isPlaying = true, errorMessage = null)
                startPlaybackTicker(messageId)
            }.onFailure {
                stopPlayback()
            }
            return
        }

        val preservedSpeed = current.playbackSpeed
        stopPlayback()
        _playbackState.value = VoicePlaybackState(
            activeMessageId = messageId,
            isPlaying = false,
            isBuffering = true,
            playbackSpeed = preservedSpeed
        )

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (sourceUrlOrPath.startsWith("http", ignoreCase = true) ||
                    sourceUrlOrPath.startsWith("content://", ignoreCase = true)
                ) {
                    setDataSource(appContext, Uri.parse(sourceUrlOrPath))
                } else {
                    setDataSource(sourceUrlOrPath)
                }
                setOnPreparedListener { readyPlayer ->
                    applyPlaybackSpeed(readyPlayer, _playbackState.value.playbackSpeed)
                    readyPlayer.start()
                    _playbackState.value = _playbackState.value.copy(
                        activeMessageId = messageId,
                        isPlaying = true,
                        isBuffering = false,
                        positionMs = 0L,
                        durationMs = readyPlayer.duration.toLong().coerceAtLeast(1L),
                        errorMessage = null
                    )
                    startPlaybackTicker(messageId)
                }
                setOnCompletionListener {
                    stopPlayback()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback(errorMessage = "Unable to play this voice message.")
                    true
                }
                prepareAsync()
            }
            player = mp
        } catch (_: Exception) {
            stopPlayback(errorMessage = "Unable to play this voice message.")
        }
    }

    fun seekTo(messageId: String, fraction: Float) {
        val current = _playbackState.value
        if (current.activeMessageId != messageId || current.isBuffering) return
        val activePlayer = player ?: return
        val duration = current.durationMs.coerceAtLeast(1L)
        val targetMs = (duration * fraction.coerceIn(0f, 1f)).toInt()
        runCatching {
            activePlayer.seekTo(targetMs)
            _playbackState.value = current.copy(positionMs = targetMs.toLong())
        }
    }

    fun cyclePlaybackSpeed() {
        val current = _playbackState.value
        val nextSpeed = when {
            current.playbackSpeed < 1.25f -> 1.5f
            current.playbackSpeed < 1.75f -> 2.0f
            else -> 1.0f
        }
        _playbackState.value = current.copy(playbackSpeed = nextSpeed)
        if (!current.isBuffering) {
            applyPlaybackSpeed(player, nextSpeed)
        }
    }

    fun clearPlaybackError() {
        _playbackState.value = _playbackState.value.copy(errorMessage = null)
    }

    private fun applyPlaybackSpeed(targetPlayer: MediaPlayer?, speed: Float) {
        targetPlayer ?: return
        runCatching {
            val wasPlaying = targetPlayer.isPlaying
            targetPlayer.playbackParams = targetPlayer.playbackParams.setSpeed(speed)
            if (!wasPlaying && _playbackState.value.isPlaying.not()) {
                targetPlayer.pause()
            }
        }
    }

    private fun startAmplitudeTicker() {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            while (recorder != null && !_recordingState.value.isPaused) {
                val amp = try {
                    ((recorder?.maxAmplitude ?: 0) / 28_000f).coerceIn(0.06f, 1f)
                } catch (_: Exception) {
                    0.06f
                }
                if (amplitudeHistory.size >= MAX_WAVEFORM_BARS) {
                    amplitudeHistory.removeFirst()
                }
                amplitudeHistory.addLast(amp)
                val totalElapsed = accumulatedElapsedMs + (System.currentTimeMillis() - segmentStartTimeMs).coerceAtLeast(0L)
                _recordingState.value = _recordingState.value.copy(
                    isRecording = true,
                    isPaused = false,
                    elapsedMs = totalElapsed,
                    amplitudeNormalized = amp,
                    amplitudes = amplitudeHistory.toList()
                )
                delay(80L)
            }
        }
    }

    private fun startPlaybackTicker(messageId: String) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (player != null && _playbackState.value.activeMessageId == messageId) {
                val p = player ?: break
                val playing = runCatching { p.isPlaying }.getOrDefault(false)
                if (playing) {
                    val pos = runCatching { p.currentPosition.toLong() }.getOrDefault(0L)
                    val dur = runCatching { p.duration.toLong() }.getOrDefault(1L).coerceAtLeast(1L)
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = pos,
                        durationMs = dur
                    )
                }
                delay(100L)
            }
        }
    }

    fun stopPlayback(errorMessage: String? = null) {
        playbackJob?.cancel()
        playbackJob = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        _playbackState.value = VoicePlaybackState(
            playbackSpeed = _playbackState.value.playbackSpeed,
            errorMessage = errorMessage
        )
    }

    private fun releaseRecorder() {
        recordingJob?.cancel()
        recordingJob = null
        runCatching { recorder?.release() }
        recorder = null
    }

    fun releaseAll() {
        cancelRecording()
        stopPlayback()
    }

    companion object {
        private const val MAX_WAVEFORM_BARS = 36
    }
}
