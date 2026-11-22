package com.example.calls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CallSession
import com.example.domain.model.CallStatus
import com.example.domain.model.CallType
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.presentation.components.EmptyIllustrationState
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.LoadingStateBox
import com.example.presentation.components.UserAvatar
import com.example.ui.theme.DangerRose
import com.example.ui.theme.DeepIndigoInk
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.VioletSecondary
import com.example.utils.DateTimeUtils
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallHistoryTabContent(
    currentUserId: String,
    callsState: UiState<List<CallSession>>,
    usersMap: Map<String, UserProfile>,
    onStartVoiceCall: (UserProfile) -> Unit,
    onStartVideoCall: (UserProfile) -> Unit
) {
    when (callsState) {
        is UiState.Loading -> LoadingStateBox("Loading call history...")
        is UiState.Error -> ErrorBannerCard(callsState.message, modifier = Modifier.padding(16.dp))
        is UiState.Success -> {
            if (callsState.data.isEmpty()) {
                EmptyIllustrationState(
                    title = "No calls yet",
                    subtitle = "Start a crystal-clear WebRTC voice or video call from any chat or contact."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)
                ) {
                    items(callsState.data, key = { it.id }) { call ->
                        val isOutgoing = call.callerId == currentUserId
                        val peerId = if (isOutgoing) call.receiverId else call.callerId
                        val peerProfile = usersMap[peerId]
                        val peerName = peerProfile?.displayName
                            ?: (if (isOutgoing) call.receiverName else call.callerName)
                            ?: "Contact"
                        val peerPhoto = peerProfile?.photoUrl
                            ?: if (isOutgoing) call.receiverPhotoUrl else call.callerPhotoUrl

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(peerName, peerPhoto, size = 50.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(peerName, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusIcon = when {
                                        call.statusEnum == CallStatus.MISSED || call.statusEnum == CallStatus.REJECTED -> Icons.AutoMirrored.Filled.CallMissed
                                        isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                                        else -> Icons.AutoMirrored.Filled.CallReceived
                                    }
                                    val tint = if (call.statusEnum == CallStatus.MISSED || call.statusEnum == CallStatus.REJECTED) {
                                        DangerRose
                                    } else {
                                        OnlineEmerald
                                    }
                                    Icon(statusIcon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${call.typeEnum.name.lowercase().replaceFirstChar { it.uppercase() }} · ${DateTimeUtils.formatChatListTime(call.startedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (peerProfile != null) {
                                IconButton(onClick = { onStartVoiceCall(peerProfile) }) {
                                    Icon(Icons.Default.Call, contentDescription = "Voice call")
                                }
                                IconButton(onClick = { onStartVideoCall(peerProfile) }) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Video call")
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
fun ActiveCallScreen(
    callId: String,
    viewModel: CallsViewModel,
    onCallClosed: () -> Unit
) {
    val context = LocalContext.current
    val sessionFlow = remember(callId) { viewModel.observeCall(callId) }
    val session by sessionFlow.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeaker by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val connState by viewModel.connectionState.collectAsStateWithLifecycle()

    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasPermissions = map[Manifest.permission.RECORD_AUDIO] == true
        hasCameraPermission = map[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions || !hasCameraPermission) {
            permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
        }
    }

    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(session, hasPermissions, hasCameraPermission) {
        val s = session ?: return@LaunchedEffect
        if (s.statusEnum == CallStatus.ENDED || s.statusEnum == CallStatus.REJECTED || s.statusEnum == CallStatus.MISSED) {
            viewModel.endCall(s.id)
            onCallClosed()
        } else {
            viewModel.startWebRtcIfReady(s, hasPermissions, hasCameraPermission)
        }
    }

    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(session?.statusEnum) {
        if (session?.statusEnum == CallStatus.ACCEPTED) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            localRenderer?.release()
            remoteRenderer?.release()
        }
    }

    val current = session
    val isCaller = current?.callerId == viewModel.currentUserId
    val peerName = if (isCaller) current?.receiverName else current?.callerName
    val peerPhoto = if (isCaller) current?.receiverPhotoUrl else current?.callerPhotoUrl
    val isVideo = current?.typeEnum == CallType.VIDEO
    val isIncomingRinging = current != null && !isCaller && current.statusEnum == CallStatus.RINGING

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepIndigoInk, Color(0xFF1E1747), Color(0xFF0A0818))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).also { r ->
                        remoteRenderer = r
                        viewModel.attachRenderers(localRenderer, r)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).also { r ->
                        localRenderer = r
                        viewModel.attachRenderers(r, remoteRenderer)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 42.dp)
            ) {
                if (!isVideo) {
                    UserAvatar(peerName ?: "Caller", peerPhoto, size = 112.dp)
                    Spacer(modifier = Modifier.height(22.dp))
                }
                Text(
                    text = peerName ?: "Connecting...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        isIncomingRinging -> "Incoming ${if (isVideo) "Video" else "Voice"} Call"
                        current?.statusEnum == CallStatus.ACCEPTED -> DateTimeUtils.formatDurationMs(elapsedSeconds * 1000L)
                        else -> connState
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            if (isIncomingRinging && current != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { viewModel.rejectCall(current.id); onCallClosed() },
                        modifier = Modifier.size(68.dp).clip(CircleShape).background(DangerRose)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    IconButton(
                        onClick = { viewModel.acceptIncomingCall(current, hasPermissions, hasCameraPermission) },
                        modifier = Modifier.size(68.dp).clip(CircleShape).background(OnlineEmerald)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = viewModel::toggleMute,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Black else Color.White
                        )
                    }
                    IconButton(
                        onClick = viewModel::toggleSpeaker,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaker) Color.White else Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Speaker",
                            tint = if (isSpeaker) Color.Black else Color.White
                        )
                    }
                    if (isVideo) {
                        IconButton(
                            onClick = viewModel::switchCamera,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.endCall(callId)
                            onCallClosed()
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(DangerRose)
                            .testTag("end_call_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End call", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingCallBanner(
    call: CallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(call.callerName ?: "Caller", call.callerPhotoUrl, size = 46.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(call.callerName ?: "Incoming call", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Incoming ${call.typeEnum.name.lowercase()} call",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(
                onClick = onDecline,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(DangerRose)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onAccept,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(OnlineEmerald)
            ) {
                Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White)
            }
        }
    }
}
