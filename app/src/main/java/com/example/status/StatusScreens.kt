package com.example.status

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.StatusPrivacy
import com.example.domain.model.StatusType
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.UserAvatar
import com.example.utils.DateTimeUtils
import kotlinx.coroutines.delay

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.theme.AppThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStatusScreen(
    viewModel: StatusViewModel,
    onBack: () -> Unit
) {
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val isPosting by viewModel.isPosting.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()

    var caption by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf(StatusType.TEXT) }
    var privacy by remember { mutableStateOf(StatusPrivacy.ALL_CONTACTS) }
    val palette = listOf("#075E54", "#12A84B", "#0F766E", "#1D4ED8", "#D8005C")
    var bgHex by remember { mutableStateOf(palette.first()) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            mediaType = StatusType.IMAGE
            viewModel.clearError()
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            mediaType = StatusType.VIDEO
            viewModel.clearError()
        }
    }

    val paletteTokens = AppThemeTokens.current
    val headerColor = paletteTokens.headerColor

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerColor,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("New 24h Status") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (uploadProgress != null) {
                    LinearProgressIndicator(progress = { uploadProgress ?: 0f }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (isPosting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (error != null) {
                    ErrorBannerCard(error!!, onRetry = viewModel::clearError)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(android.graphics.Color.parseColor(bgHex))),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri != null && mediaType == StatusType.IMAGE) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Status preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (selectedUri != null && mediaType == StatusType.VIDEO) {
                        Text("Video selected and ready to post", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(
                            text = caption.ifBlank { "Share a moment for 24 hours..." },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    if (selectedUri != null) {
                        FilledTonalIconButton(
                            onClick = {
                                selectedUri = null
                                mediaType = StatusType.TEXT
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .testTag("clear_status_media_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove media")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedUri == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        palette.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (bgHex == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        shape = CircleShape
                                    )
                                    .clickable { bgHex = hex }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.Image, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Photo")
                    }
                    OutlinedButton(onClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }) {
                        Icon(Icons.Default.VideoLibrary, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Video")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text(if (selectedUri == null) "Status Text" else "Caption (optional)") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("status_caption_input")
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = privacy == StatusPrivacy.ALL_CONTACTS,
                        onClick = { privacy = StatusPrivacy.ALL_CONTACTS },
                        leadingIcon = { Icon(Icons.Default.Public, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Contacts") }
                    )
                    FilterChip(
                        selected = privacy == StatusPrivacy.ONLY_ME,
                        onClick = { privacy = StatusPrivacy.ONLY_ME },
                        leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Only Me") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val uri = selectedUri
                    if (uri != null) {
                        viewModel.postMediaStatus(uri, mediaType, caption, privacy, onBack)
                    } else {
                        viewModel.postTextStatus(caption, bgHex, privacy, onBack)
                    }
                },
                enabled = (selectedUri != null || caption.isNotBlank()) && !isPosting && uploadProgress == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("post_status_button"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = paletteTokens.buttonColor)
            ) {
                if (isPosting || uploadProgress != null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sharing...")
                } else {
                    Text("Share 24h Status")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(
    targetUserId: String,
    viewModel: StatusViewModel,
    onClose: () -> Unit
) {
    val allStatuses by viewModel.allStatuses.collectAsStateWithLifecycle()
    val usersMap by viewModel.usersMap.collectAsStateWithLifecycle()
    val userStatuses = remember(allStatuses, targetUserId) {
        allStatuses.filter { it.userId == targetUserId }.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var showViewersSheet by remember { mutableStateOf(false) }

    if (userStatuses.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val safeIndex = currentIndex.coerceIn(0, userStatuses.lastIndex)
    val current = userStatuses[safeIndex]

    LaunchedEffect(current.id) {
        viewModel.markViewed(current)
        if (current.statusType != StatusType.VIDEO) {
            delay(6000L)
            if (safeIndex < userStatuses.lastIndex) currentIndex = safeIndex + 1
            else onClose()
        }
    }

    val bg = try {
        Color(android.graphics.Color.parseColor(current.backgroundColorHex ?: "#281D5A"))
    } catch (_: Exception) {
        Color(0xFF281D5A)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (current.statusType == StatusType.TEXT) bg else Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        when (current.statusType) {
            StatusType.IMAGE -> AsyncImage(
                model = current.mediaUrl,
                contentDescription = "Status image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            StatusType.VIDEO -> AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoPath(current.mediaUrl)
                        setOnPreparedListener { it.start() }
                        setOnCompletionListener {
                            if (safeIndex < userStatuses.lastIndex) currentIndex = safeIndex + 1 else onClose()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            StatusType.TEXT -> Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = current.caption.orEmpty(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Tap zones left/right
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (safeIndex > 0) currentIndex = safeIndex - 1
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (safeIndex < userStatuses.lastIndex) currentIndex = safeIndex + 1 else onClose()
                    }
            )
        }

        // Top bar + progress indicators
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                userStatuses.forEachIndexed { idx, _ ->
                    LinearProgressIndicator(
                        progress = { if (idx <= safeIndex) 1f else 0.25f },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.weight(1f).height(3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(current.userName, current.userPhotoUrl, size = 40.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(current.userName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(DateTimeUtils.formatStatusAge(current.createdAt), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
                }
                if (current.userId == viewModel.currentUserId) {
                    IconButton(onClick = { showViewersSheet = true }) {
                        Icon(Icons.Default.RemoveRedEye, contentDescription = "Viewers", tint = Color.White)
                    }
                    IconButton(onClick = {
                        viewModel.deleteStatus(current.id) {
                            if (userStatuses.size <= 1) onClose()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete status", tint = Color.White)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        if (!current.caption.isNullOrBlank() && current.statusType != StatusType.TEXT) {
            Text(
                text = current.caption,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(16.dp)
            )
        }
    }

    if (showViewersSheet) {
        ModalBottomSheet(onDismissRequest = { showViewersSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Viewed by (${current.viewerIds.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                if (current.viewerIds.isEmpty()) {
                    Text("No views yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(current.viewerIds, key = { it }) { viewerId ->
                            val viewer = usersMap[viewerId]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(viewer?.displayName ?: "Viewer", viewer?.photoUrl, size = 38.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(viewer?.displayName ?: viewerId, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
