package com.example.calls

import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.CallSession
import com.example.domain.model.CallStatus
import com.example.domain.model.CallType
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.presentation.components.EmptyIllustrationState
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.LoadingStateBox
import com.example.presentation.components.UserAvatar
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.DangerRose
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.VoiceCallGradientBottom
import com.example.ui.theme.VoiceCallGradientMid
import com.example.ui.theme.VoiceCallGradientTop
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
                    contentPadding = PaddingValues(bottom = 96.dp, top = 6.dp)
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

                        val isMissedOrRejected = call.statusEnum == CallStatus.MISSED || call.statusEnum == CallStatus.REJECTED
                        val statusLabel = when {
                            isMissedOrRejected -> "Missed call"
                            isOutgoing -> "Outgoing call"
                            else -> "Incoming call"
                        }
                        val statusIcon = when {
                            isMissedOrRejected -> Icons.AutoMirrored.Filled.CallMissed
                            isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.AutoMirrored.Filled.CallReceived
                        }
                        val tint = if (isMissedOrRejected) DangerRose else BrandGreen

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(peerName, peerPhoto, size = 52.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peerName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(statusIcon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isMissedOrRejected) DangerRose else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = DateTimeUtils.formatChatListTime(call.startedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOutgoing || isMissedOrRejected) MaterialTheme.colorScheme.onSurfaceVariant else BrandGreen,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            if (peerProfile != null) {
                                IconButton(
                                    onClick = {
                                        if (call.typeEnum == CallType.VIDEO) onStartVideoCall(peerProfile)
                                        else onStartVoiceCall(peerProfile)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (call.typeEnum == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                                        contentDescription = if (call.typeEnum == CallType.VIDEO) "Video call" else "Voice call",
                                        tint = BrandGreen
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                        )
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
    var isClosing by remember { mutableStateOf(false) }

    fun closeScreenSafely(endRemoteCall: Boolean) {
        if (isClosing) return
        isClosing = true
        viewModel.detachRenderers(localRenderer, remoteRenderer)
        localRenderer = null
        remoteRenderer = null
        if (endRemoteCall) {
            viewModel.endCall(callId)
        } else {
            viewModel.closeLocalSession()
        }
        onCallClosed()
    }

    LaunchedEffect(session, hasPermissions, hasCameraPermission) {
        val s = session ?: return@LaunchedEffect
        if (s.statusEnum == CallStatus.ENDED || s.statusEnum == CallStatus.REJECTED || s.statusEnum == CallStatus.MISSED) {
            closeScreenSafely(endRemoteCall = false)
        } else if (!isClosing) {
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
            viewModel.detachRenderers(localRenderer, remoteRenderer)
            viewModel.closeLocalSession()
        }
    }

    val current = session
    val isCaller = current?.callerId == viewModel.currentUserId
    val peerName = if (isCaller) current.receiverName else current?.callerName
    val peerPhoto = if (isCaller) current.receiverPhotoUrl else current?.callerPhotoUrl
    val isVideo = current?.typeEnum == CallType.VIDEO
    val incomingSession = current?.takeIf { !isCaller && it.statusEnum == CallStatus.RINGING }
    val isIncomingRinging = incomingSession != null

    BackHandler {
        closeScreenSafely(endRemoteCall = !isIncomingRinging)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VoiceCallGradientTop, VoiceCallGradientMid, VoiceCallGradientBottom)
                )
            )
    ) {
        if (isVideo && !isClosing) {
            AndroidView(
                factory = { ctx ->
                    val renderer = LayoutInflater.from(ctx)
                        .inflate(R.layout.view_remote_video_renderer, null, false) as SurfaceViewRenderer
                    remoteRenderer = renderer
                    viewModel.attachRemoteRenderer(renderer)
                    renderer
                },
                onRelease = { renderer ->
                    if (remoteRenderer === renderer) remoteRenderer = null
                    viewModel.detachRemoteRenderer(renderer)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isVideo) {
                // Showcase #9 Video Call top timer + close/switch controls
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = when {
                            isIncomingRinging -> "Incoming Video Call"
                            current?.statusEnum == CallStatus.ACCEPTED -> DateTimeUtils.formatDurationMs(elapsedSeconds * 1000L)
                            else -> connState
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = { closeScreenSafely(endRemoteCall = !isIncomingRinging) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close call view", tint = Color.White)
                    }
                }
            } else {
                // Showcase #8 Voice Call layout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 36.dp)
                ) {
                    Text(
                        text = when {
                            isIncomingRinging -> "Incoming Voice Call…"
                            current?.statusEnum == CallStatus.ACCEPTED -> DateTimeUtils.formatDurationMs(elapsedSeconds * 1000L)
                            else -> "Calling..."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Spacer(modifier = Modifier.height(34.dp))
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                            .padding(3.dp)
                    ) {
                        UserAvatar(
                            name = peerName ?: "Caller",
                            photoUrl = peerPhoto,
                            size = 138.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = peerName ?: "Connecting...",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (current?.statusEnum == CallStatus.ACCEPTED) "Connected · Voice Call" else "Voice Call",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                // Floating bottom-right PiP local camera preview matching Showcase #9
                if (isVideo && !isClosing && hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 18.dp)
                            .size(width = 104.dp, height = 148.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val renderer = LayoutInflater.from(ctx)
                                    .inflate(R.layout.view_local_video_renderer, null, false) as SurfaceViewRenderer
                                localRenderer = renderer
                                viewModel.attachLocalRenderer(renderer)
                                renderer
                            },
                            onRelease = { renderer ->
                                if (localRenderer === renderer) localRenderer = null
                                viewModel.detachLocalRenderer(renderer)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (incomingSession != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                if (!isClosing) {
                                    isClosing = true
                                    viewModel.detachRenderers(localRenderer, remoteRenderer)
                                    localRenderer = null
                                    remoteRenderer = null
                                    viewModel.rejectCall(incomingSession.id)
                                    onCallClosed()
                                }
                            },
                            modifier = Modifier.size(68.dp).clip(CircleShape).background(DangerRose)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        IconButton(
                            onClick = { viewModel.acceptIncomingCall(incomingSession, hasPermissions, hasCameraPermission) },
                            modifier = Modifier.size(68.dp).clip(CircleShape).background(OnlineEmerald)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isVideo) {
                            IconButton(
                                onClick = viewModel::switchCamera,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
                            }
                        } else {
                            IconButton(
                                onClick = viewModel::toggleMute,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.22f))
                            ) {
                                Icon(
                                    if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) Color.Black else Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = { closeScreenSafely(endRemoteCall = true) },
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(DangerRose)
                                .testTag("end_call_button")
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End call", tint = Color.White, modifier = Modifier.size(30.dp))
                        }

                        if (isVideo) {
                            IconButton(
                                onClick = viewModel::toggleMute,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isMuted) Color.White else Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) Color.Black else Color.White
                                )
                            }
                        } else {
                            IconButton(
                                onClick = viewModel::toggleSpeaker,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeaker) Color.White else Color.White.copy(alpha = 0.22f))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speaker",
                                    tint = if (isSpeaker) Color.Black else Color.White
                                )
                            }
                        }
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
