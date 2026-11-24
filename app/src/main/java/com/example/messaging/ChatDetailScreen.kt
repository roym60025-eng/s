package com.example.messaging

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ManojSonaliApp
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageType
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.presentation.components.ChatDoodleWallpaper
import com.example.presentation.components.DeliveryTickIcon
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.LoadingStateBox
import com.example.presentation.components.UserAvatar
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.DeepIndigoInk
import com.example.ui.theme.EmeraldButton
import com.example.ui.theme.ForestGreenDarkHeader
import com.example.ui.theme.ForestGreenHeader
import com.example.ui.theme.IncomingBubbleDark
import com.example.ui.theme.IncomingBubbleLight
import com.example.ui.theme.MutedSlateDark
import com.example.ui.theme.MutedSlateLight
import com.example.ui.theme.OutgoingGreenDark
import com.example.ui.theme.OutgoingGreenLight
import com.example.utils.DateTimeUtils
import com.example.utils.VoicePlaybackState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatDetailViewModel,
    onBack: () -> Unit,
    onStartVoiceCall: (UserProfile) -> Unit,
    onStartVideoCall: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? ManojSonaliApp
    DisposableEffect(viewModel.chatId) {
        app?.container?.notificationMonitor?.setActiveChat(viewModel.chatId)
        onDispose {
            app?.container?.notificationMonitor?.setActiveChat(null)
        }
    }
    val chatRoom by viewModel.chatRoom.collectAsStateWithLifecycle()
    val peerUser by viewModel.peerUser.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val composerText by viewModel.composerText.collectAsStateWithLifecycle()
    val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle()
    val editingMessage by viewModel.editingMessage.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val recordingState by viewModel.voiceRecordingState.collectAsStateWithLifecycle()
    val playbackState by viewModel.voicePlaybackState.collectAsStateWithLifecycle()

    var showAttachSheet by remember { mutableStateOf(false) }
    var showEmojiBar by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var forwardTargetMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var previewMediaMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) ForestGreenDarkHeader else ForestGreenHeader

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.sendMediaUri(uri, MessageType.IMAGE)
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.sendMediaUri(uri, MessageType.VIDEO)
    }

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.sendMediaUri(uri, MessageType.DOCUMENT)
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.sendMediaUri(uri, MessageType.AUDIO)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCamera = true
        else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoiceRecording()
        else Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
    }

    if (showCamera) {
        CameraCaptureScreen(
            onCaptured = { uri ->
                showCamera = false
                viewModel.sendMediaUri(uri, MessageType.IMAGE)
            },
            onClose = { showCamera = false }
        )
        return
    }

    val isPeerTyping = chatRoom?.typingUserIds?.any { it != viewModel.currentUserId } == true

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerColor,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            name = peerUser?.displayName ?: "Contact",
                            photoUrl = peerUser?.photoUrl,
                            size = 38.dp,
                            isOnline = peerUser?.online == true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = peerUser?.displayName ?: "Conversation",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    isPeerTyping -> "typing..."
                                    peerUser?.online == true -> "online"
                                    peerUser?.lastSeen != null -> "last seen ${DateTimeUtils.formatLastSeen(peerUser?.lastSeen)}"
                                    else -> "Manoj & Sonali"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    peerUser?.let { peer ->
                        IconButton(
                            onClick = { onStartVideoCall(peer) },
                            modifier = Modifier.testTag("chat_video_call_button")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video call")
                        }
                        IconButton(
                            onClick = { onStartVoiceCall(peer) },
                            modifier = Modifier.testTag("chat_voice_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Voice call")
                        }
                    }
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Chat options")
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Attach Media") },
                                onClick = {
                                    showTopMenu = false
                                    showAttachSheet = true
                                }
                            )
                            peerUser?.let { peer ->
                                DropdownMenuItem(
                                    text = { Text("Voice Call") },
                                    onClick = {
                                        showTopMenu = false
                                        onStartVoiceCall(peer)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Video Call") },
                                    onClick = {
                                        showTopMenu = false
                                        onStartVideoCall(peer)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        ChatDoodleWallpaper(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uploadProgress != null) {
                    LinearProgressIndicator(
                        progress = { uploadProgress ?: 0f },
                        color = BrandGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (actionError != null) {
                    ErrorBannerCard(
                        message = actionError!!,
                        onRetry = viewModel::clearError,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (val state = messagesState) {
                        is UiState.Loading -> LoadingStateBox("Loading chat history...")
                        is UiState.Error -> ErrorBannerCard(state.message, modifier = Modifier.padding(16.dp))
                        is UiState.Success -> {
                            val msgs = state.data
                            LaunchedEffect(msgs.size) {
                                if (msgs.isNotEmpty()) {
                                    listState.animateScrollToItem(msgs.lastIndex)
                                    viewModel.markMessagesAsRead(msgs)
                                }
                            }

                            if (msgs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                        tonalElevation = 1.dp
                                    ) {
                                        Text(
                                            text = "Say hello to start this conversation.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (msgs.size >= 40) {
                                        item {
                                            TextButton(
                                                onClick = viewModel::loadOlderMessages,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Load older messages")
                                            }
                                        }
                                    }

                                    itemsIndexed(msgs, key = { _, m -> m.id }) { index, msg ->
                                        val dateLabel = formatDaySeparator(msg.timestamp?.toDate())
                                        val prevLabel = if (index > 0) formatDaySeparator(msgs[index - 1].timestamp?.toDate()) else null
                                        if (dateLabel != null && dateLabel != prevLabel) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isDark) Color(0xFF1F2C34) else Color.White.copy(alpha = 0.92f),
                                                    shadowElevation = 0.5.dp
                                                ) {
                                                    Text(
                                                        text = dateLabel,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        val isMine = msg.senderId == viewModel.currentUserId
                                        ChatMessageBubble(
                                            message = msg,
                                            isMine = isMine,
                                            playbackState = playbackState,
                                            onPlayVoice = { viewModel.toggleVoicePlayback(msg) },
                                            onOpenMedia = { previewMediaMessage = msg },
                                            onReply = { viewModel.startReply(msg) },
                                            onForward = { forwardTargetMessage = msg },
                                            onEdit = { viewModel.startEdit(msg) },
                                            onDelete = { viewModel.deleteMessage(msg) },
                                            onReact = { emoji -> viewModel.toggleReaction(msg, emoji) },
                                            onCopy = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Message", msg.text))
                                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Reply or Edit bar
                AnimatedVisibility(visible = replyingTo != null || editingMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(BrandGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (editingMessage != null) "Editing message" else "Replying to ${replyingTo?.senderName ?: "message"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandGreen
                                )
                                Text(
                                    text = editingMessage?.text ?: replyingTo?.text.orEmpty().ifBlank { "Media attachment" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = viewModel::cancelReplyOrEdit) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        }
                    }
                }

                // Quick Emoji Strip
                AnimatedVisibility(visible = showEmojiBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("😀", "❤️", "🙏", "👍", "🔥", "🎉", "🥳", "✨").forEach { emoji ->
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { viewModel.onComposerChanged(composerText + emoji) }
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                // Showcase #5 floating pill composer + emerald Mic/Send circular button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    if (recordingState.isRecording) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = viewModel::cancelVoiceRecording) {
                                    Icon(Icons.Default.Delete, contentDescription = "Discard voice", tint = MaterialTheme.colorScheme.error)
                                }
                                LinearProgressIndicator(
                                    progress = { recordingState.amplitudeNormalized },
                                    color = BrandGreen,
                                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = DateTimeUtils.formatDurationMs(recordingState.elapsedMs),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = viewModel::stopAndSendVoiceRecording,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldButton)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Send voice", tint = Color.White)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = if (isDark) ForestGreenDarkHeader else Color.White,
                                shadowElevation = 1.5.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { showEmojiBar = !showEmojiBar },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SentimentSatisfiedAlt,
                                            contentDescription = "Emojis",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (composerText.isEmpty()) {
                                            Text(
                                                text = "Type a message...",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        BasicTextField(
                                            value = composerText,
                                            onValueChange = viewModel::onComposerChanged,
                                            maxLines = 4,
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(BrandGreen),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("chat_message_input")
                                        )
                                    }

                                    IconButton(
                                        onClick = { showAttachSheet = true },
                                        modifier = Modifier.size(40.dp).testTag("chat_attach_button")
                                    ) {
                                        Icon(
                                            Icons.Default.AttachFile,
                                            contentDescription = "Attach media",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val granted = ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (granted) showCamera = true
                                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        },
                                        modifier = Modifier.size(40.dp).testTag("chat_camera_button")
                                    ) {
                                        Icon(
                                            Icons.Default.PhotoCamera,
                                            contentDescription = "Take photo",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            if (composerText.isNotBlank()) {
                                IconButton(
                                    onClick = viewModel::sendTextMessage,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldButton)
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.White
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) viewModel.startVoiceRecording()
                                        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldButton)
                                        .testTag("chat_voice_record_button")
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Record voice message",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Share Media & Files", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachOptionItem(Icons.Default.CameraAlt, "Camera") {
                        showAttachSheet = false
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) showCamera = true
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    AttachOptionItem(Icons.Default.Image, "Photo") {
                        showAttachSheet = false
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    AttachOptionItem(Icons.Default.VideoLibrary, "Video") {
                        showAttachSheet = false
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachOptionItem(Icons.Default.Description, "Document") {
                        showAttachSheet = false
                        documentPicker.launch(arrayOf("application/pdf", "text/plain", "application/*"))
                    }
                    AttachOptionItem(Icons.Default.AudioFile, "Audio") {
                        showAttachSheet = false
                        audioPicker.launch(arrayOf("audio/*"))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    forwardTargetMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { forwardTargetMessage = null },
            title = { Text("Forward message to") },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    itemsIndexed(allUsers.filter { it.uid != viewModel.currentUserId }, key = { _, u -> u.uid }) { _, user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.forwardMessage(msg, user)
                                    forwardTargetMessage = null
                                    Toast.makeText(context, "Forwarded to ${user.displayName}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(user.displayName, user.photoUrl, size = 38.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(user.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { forwardTargetMessage = null }) { Text("Cancel") }
            }
        )
    }

    previewMediaMessage?.let { mediaMsg ->
        Dialog(onDismissRequest = { previewMediaMessage = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mediaMsg.mediaName ?: "Media Preview",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            viewModel.downloadMediaToCache(mediaMsg) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                        }
                        IconButton(onClick = { previewMediaMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    when (mediaMsg.messageType) {
                        MessageType.IMAGE -> AsyncImage(
                            model = mediaMsg.mediaUrl,
                            contentDescription = "Full image preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(340.dp)
                        )
                        MessageType.VIDEO -> AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoPath(mediaMsg.mediaUrl)
                                    setOnPreparedListener { it.isLooping = true; start() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(320.dp)
                        )
                        else -> Unit
                    }
                }
            }
        }
    }
}

private fun formatDaySeparator(date: Date?): String? {
    date ?: return null
    val cal = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    }
}

@Composable
private fun AttachOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    playbackState: VoicePlaybackState,
    onPlayVoice: () -> Unit,
    onOpenMedia: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onCopy: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var showMenu by remember { mutableStateOf(false) }

    val shape = if (isMine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val bubbleColor = when {
        isMine && isDark -> OutgoingGreenDark
        isMine -> OutgoingGreenLight
        isDark -> IncomingBubbleDark
        else -> IncomingBubbleLight
    }

    val textColor = if (isDark) Color(0xFFE9EDEF) else DeepIndigoInk
    val metaColor = if (isDark) MutedSlateDark else MutedSlateLight

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                shape = shape,
                shadowElevation = 0.8.dp,
                color = bubbleColor,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clip(shape)
                    .combinedClickable(
                        onClick = {
                            if (message.messageType == MessageType.IMAGE || message.messageType == MessageType.VIDEO) {
                                onOpenMedia()
                            } else if (message.messageType == MessageType.VOICE || message.messageType == MessageType.AUDIO) {
                                onPlayVoice()
                            }
                        },
                        onLongClick = { if (!message.isDeleted) showMenu = true }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.forwarded) {
                        Text(
                            text = "Forwarded",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                            color = metaColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = if (isDark) 0.25f else 0.06f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = message.replyToSender ?: "Reply",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandGreen
                                )
                                Text(
                                    text = message.replyToText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.85f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (message.isDeleted) {
                        Text(
                            text = "This message was deleted",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = metaColor
                        )
                    } else {
                        when (message.messageType) {
                            MessageType.IMAGE -> {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    AsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = "Shared image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(195.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                if (message.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                                }
                            }
                            MessageType.VIDEO -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.28f),
                                    modifier = Modifier.fillMaxWidth().height(124.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play video", tint = Color.White, modifier = Modifier.size(44.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(message.mediaName ?: "Video", style = MaterialTheme.typography.bodySmall, color = textColor)
                            }
                            MessageType.VOICE, MessageType.AUDIO -> {
                                val isActive = playbackState.activeMessageId == message.id
                                val isPlaying = isActive && playbackState.isPlaying
                                val durationMs = message.voiceDurationMs ?: playbackState.durationMs
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onPlayVoice, modifier = Modifier.size(38.dp)) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = BrandGreen
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = {
                                            if (isActive && playbackState.durationMs > 0L) {
                                                (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                                            } else 0f
                                        },
                                        color = BrandGreen,
                                        trackColor = textColor.copy(alpha = 0.18f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = DateTimeUtils.formatDurationMs(
                                            if (isActive && isPlaying) playbackState.positionMs else durationMs
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = metaColor
                                    )
                                }
                            }
                            MessageType.DOCUMENT -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable(onClick = onOpenMedia)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = BrandGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = message.mediaName ?: "Document",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = DateTimeUtils.formatBytes(message.mediaSizeBytes ?: 0L),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = metaColor
                                        )
                                    }
                                }
                            }
                            MessageType.TEXT -> {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = textColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (message.editedAt != null && !message.isDeleted) {
                            Text(
                                text = "edited · ",
                                style = MaterialTheme.typography.labelSmall,
                                color = metaColor
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatMessageTime(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = metaColor
                        )
                        if (isMine) {
                            Spacer(modifier = Modifier.width(4.dp))
                            DeliveryTickIcon(
                                status = message.status,
                                tintDefault = metaColor
                            )
                        }
                    }
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    listOf("❤️", "👍", "😂", "🔥", "🙏").forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .clickable { showMenu = false; onReact(emoji) }
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
                DropdownMenuItem(
                    text = { Text("Reply") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, null) },
                    onClick = { showMenu = false; onReply() }
                )
                DropdownMenuItem(
                    text = { Text("Forward") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, null) },
                    onClick = { showMenu = false; onForward() }
                )
                if (message.text.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = { showMenu = false; onCopy() }
                    )
                }
                if (isMine && message.messageType == MessageType.TEXT) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { showMenu = false; onEdit() }
                    )
                }
                if (isMine) {
                    DropdownMenuItem(
                        text = { Text("Delete for everyone") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(50),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = message.reactions.values.groupingBy { it }.eachCount()
                        .entries.joinToString(" ") { (e, c) -> if (c > 1) "$e$c" else e },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
