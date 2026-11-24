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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.ManojSonaliApp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageType
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.presentation.components.DeliveryTickIcon
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.LoadingStateBox
import com.example.presentation.components.UserAvatar
import com.example.ui.theme.OutgoingBubbleEnd
import com.example.ui.theme.OutgoingBubbleStart
import com.example.utils.DateTimeUtils
import com.example.utils.VoicePlaybackState

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
    var forwardTargetMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var previewMediaMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()

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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                            size = 40.dp,
                            isOnline = peerUser?.online == true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = peerUser?.displayName ?: "Conversation",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    isPeerTyping -> "typing..."
                                    peerUser?.online == true -> "Online"
                                    peerUser?.lastSeen != null -> "Last seen ${DateTimeUtils.formatLastSeen(peerUser?.lastSeen)}"
                                    else -> "Manoj & Sonali"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPeerTyping || peerUser?.online == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                actions = {
                    peerUser?.let { peer ->
                        IconButton(
                            onClick = { onStartVoiceCall(peer) },
                            modifier = Modifier.testTag("chat_voice_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Voice call")
                        }
                        IconButton(
                            onClick = { onStartVideoCall(peer) },
                            modifier = Modifier.testTag("chat_video_call_button")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video call")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            if (uploadProgress != null) {
                LinearProgressIndicator(
                    progress = { uploadProgress ?: 0f },
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
                                Text(
                                    text = "Say hello to start this conversation.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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

                                items(msgs, key = { it.id }) { msg ->
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
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingMessage != null) "Editing message" else "Replying to ${replyingTo?.senderName ?: "message"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
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
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
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

            // Composer / Voice Recorder Bar
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                ) {
                if (recordingState.isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::cancelVoiceRecording) {
                            Icon(Icons.Default.Delete, contentDescription = "Discard voice", tint = MaterialTheme.colorScheme.error)
                        }
                        LinearProgressIndicator(
                            progress = { recordingState.amplitudeNormalized },
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
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Send voice", tint = Color.White)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEmojiBar = !showEmojiBar }) {
                            Icon(Icons.Default.EmojiEmotions, contentDescription = "Emojis")
                        }
                        OutlinedTextField(
                            value = composerText,
                            onValueChange = viewModel::onComposerChanged,
                            placeholder = { Text("Message...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        IconButton(
                            onClick = { showAttachSheet = true },
                            modifier = Modifier.testTag("chat_attach_button")
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach media")
                        }
                        if (composerText.isNotBlank()) {
                            IconButton(
                                onClick = viewModel::sendTextMessage,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(OutgoingBubbleStart, OutgoingBubbleEnd))
                                    )
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
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("chat_voice_record_button")
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Record voice message",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                    items(allUsers.filter { it.uid != viewModel.currentUserId }, key = { it.uid }) { user ->
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
    var showMenu by remember { mutableStateOf(false) }
    val shape = if (isMine) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 22.dp)
    }
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                shape = shape,
                tonalElevation = if (isMine) 0.dp else 2.dp,
                color = if (isMine) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clip(shape)
                    .then(
                        if (isMine) {
                            Modifier.background(
                                Brush.linearGradient(listOf(OutgoingBubbleStart, OutgoingBubbleEnd))
                            )
                        } else {
                            Modifier
                        }
                    )
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
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (message.forwarded) {
                        Text(
                            text = "Forwarded",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                            color = textColor.copy(alpha = 0.78f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = if (isMine) 0.18f else 0.08f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = message.replyToSender ?: "Reply",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor
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
                            color = textColor.copy(alpha = 0.75f)
                        )
                    } else {
                        when (message.messageType) {
                            MessageType.IMAGE -> {
                                AsyncImage(
                                    model = message.mediaUrl,
                                    contentDescription = "Shared image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                if (message.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                                }
                            }
                            MessageType.VIDEO -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Black.copy(alpha = 0.28f),
                                    modifier = Modifier.fillMaxWidth().height(120.dp)
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
                                            tint = textColor
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = {
                                            if (isActive && playbackState.durationMs > 0L) {
                                                (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                                            } else 0f
                                        },
                                        color = textColor,
                                        trackColor = textColor.copy(alpha = 0.25f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = DateTimeUtils.formatDurationMs(
                                            if (isActive && isPlaying) playbackState.positionMs else durationMs
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor
                                    )
                                }
                            }
                            MessageType.DOCUMENT -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable(onClick = onOpenMedia)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = textColor)
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
                                            color = textColor.copy(alpha = 0.78f)
                                        )
                                    }
                                }
                            }
                            MessageType.TEXT -> {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (message.editedAt != null && !message.isDeleted) {
                            Text(
                                text = "edited · ",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.75f)
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatMessageTime(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.78f)
                        )
                        if (isMine) {
                            Spacer(modifier = Modifier.width(4.dp))
                            DeliveryTickIcon(
                                status = message.status,
                                tintDefault = Color.White.copy(alpha = 0.85f)
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
                tonalElevation = 3.dp,
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
