package com.example.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitudeNormalized: Float = 0f,
    val recordedFile: File? = null
)

data class VoicePlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class VoiceRecorderPlayer(private val appContext: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingJob: Job? = null
    private var startTimeMs: Long = 0L

    private var player: MediaPlayer? = null
    private var playbackJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main)

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
            startTimeMs = System.currentTimeMillis()
            _recordingState.value = VoiceRecordingState(isRecording = true, elapsedMs = 0L)

            recordingJob = scope.launch {
                while (recorder != null) {
                    val amp = try {
                        (recorder?.maxAmplitude ?: 0) / 32767f
                    } catch (_: Exception) {
                        0f
                    }
                    _recordingState.value = VoiceRecordingState(
                        isRecording = true,
                        elapsedMs = System.currentTimeMillis() - startTimeMs,
                        amplitudeNormalized = amp.coerceIn(0f, 1f)
                    )
                    delay(80L)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            releaseRecorder()
            _recordingState.value = VoiceRecordingState()
            Result.failure(e)
        }
    }

    fun stopRecording(): Pair<File, Long>? {
        val elapsed = System.currentTimeMillis() - startTimeMs
        val file = outputFile
        return try {
            recordingJob?.cancel()
            recordingJob = null
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            _recordingState.value = VoiceRecordingState(
                isRecording = false,
                elapsedMs = elapsed,
                recordedFile = file
            )
            if (file != null && file.exists() && file.length() > 0L) {
                file to elapsed.coerceAtLeast(500L)
            } else {
                null
            }
        } catch (_: Exception) {
            releaseRecorder()
            _recordingState.value = VoiceRecordingState()
            null
        }
    }

    fun cancelRecording() {
        releaseRecorder()
        outputFile?.delete()
        outputFile = null
        _recordingState.value = VoiceRecordingState()
    }

    fun togglePlay(messageId: String, sourceUrlOrPath: String) {
        val current = _playbackState.value
        if (current.activeMessageId == messageId && current.isPlaying) {
            player?.pause()
            _playbackState.value = current.copy(isPlaying = false)
            return
        }
        if (current.activeMessageId == messageId && !current.isPlaying && player != null) {
            player?.start()
            _playbackState.value = current.copy(isPlaying = true)
            startPlaybackTicker(messageId)
            return
        }

        stopPlayback()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(sourceUrlOrPath)
                setOnPreparedListener { readyPlayer ->
                    readyPlayer.start()
                    _playbackState.value = VoicePlaybackState(
                        activeMessageId = messageId,
                        isPlaying = true,
                        positionMs = 0L,
                        durationMs = readyPlayer.duration.toLong().coerceAtLeast(1L)
                    )
                    startPlaybackTicker(messageId)
                }
                setOnCompletionListener {
                    stopPlayback()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }
                prepareAsync()
            }
            player = mp
        } catch (_: Exception) {
            stopPlayback()
        }
    }

    private fun startPlaybackTicker(messageId: String) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (player != null && _playbackState.value.activeMessageId == messageId) {
                val p = player ?: break
                if (p.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = p.currentPosition.toLong(),
                        durationMs = p.duration.toLong().coerceAtLeast(1L)
                    )
                }
                delay(120L)
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        _playbackState.value = VoicePlaybackState()
    }

    private fun releaseRecorder() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }

    fun releaseAll() {
        releaseRecorder()
        stopPlayback()
    }
}
