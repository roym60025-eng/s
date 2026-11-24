package com.example.groups

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.GroupMessage
import com.example.domain.model.MessageType
import com.example.domain.model.UiState
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.LoadingStateBox
import com.example.presentation.components.UserAvatar
import com.example.ui.theme.OutgoingBubbleEnd
import com.example.ui.theme.OutgoingBubbleStart
import com.example.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: GroupsViewModel,
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val usersMap by viewModel.allUsers.collectAsStateWithLifecycle()
    val uploadedPhoto by viewModel.uploadedGroupPhotoUrl.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()

    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.uploadGroupAvatar(uri) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Create Group") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (uploadProgress != null) {
                LinearProgressIndicator(progress = { uploadProgress ?: 0f }, modifier = Modifier.fillMaxWidth())
            }
            if (error != null) {
                ErrorBannerCard(error!!, onRetry = viewModel::clearError)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uploadedPhoto != null) {
                        AsyncImage(
                            model = uploadedPhoto,
                            contentDescription = "Group avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Pick group photo")
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).testTag("create_group_name_input")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Group Description") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Select Members (${selectedIds.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                val peers = usersMap.values.filter { it.uid != viewModel.currentUserId }
                items(peers, key = { it.uid }) { user ->
                    val checked = user.uid in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) selectedIds.remove(user.uid) else selectedIds.add(user.uid)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        UserAvatar(user.displayName, user.photoUrl, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(user.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.createGroup(groupName, description, selectedIds.toList(), onGroupCreated)
                },
                enabled = groupName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("create_group_submit_button"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Create Group")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    viewModel: GroupsViewModel,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as? com.example.ManojSonaliApp
    androidx.compose.runtime.DisposableEffect(groupId) {
        app?.container?.notificationMonitor?.setActiveGroup(groupId)
        onDispose {
            app?.container?.notificationMonitor?.setActiveGroup(null)
        }
    }

    val groupFlow = remember(groupId) { viewModel.observeGroup(groupId) }
    val messagesFlow = remember(groupId) { viewModel.observeGroupMessages(groupId) }

    val group by groupFlow.collectAsStateWithLifecycle()
    val messagesState by messagesFlow.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()

    var composer by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<GroupMessage?>(null) }
    val listState = rememberLazyListState()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val g = group
        if (uri != null && g != null) viewModel.sendGroupMedia(g, uri, MessageType.IMAGE)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.clickable(onClick = onOpenInfo),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(group?.name ?: "Group", group?.imageUrl, size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = group?.name ?: "Group",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${group?.memberIds?.size ?: 0} members · Tap for info",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInfo, modifier = Modifier.testTag("group_info_button")) {
                        Icon(Icons.Default.Info, contentDescription = "Group info")
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
                LinearProgressIndicator(progress = { uploadProgress ?: 0f }, modifier = Modifier.fillMaxWidth())
            }
            if (error != null) {
                ErrorBannerCard(error!!, onRetry = viewModel::clearError, modifier = Modifier.padding(12.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = messagesState) {
                    is UiState.Loading -> LoadingStateBox("Loading group messages...")
                    is UiState.Error -> ErrorBannerCard(state.message, modifier = Modifier.padding(16.dp))
                    is UiState.Success -> {
                        val list = state.data
                        LaunchedEffect(list.size) {
                            if (list.isNotEmpty()) listState.animateScrollToItem(list.lastIndex)
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(list, key = { it.id }) { msg ->
                                GroupMessageBubble(
                                    message = msg,
                                    isMine = msg.senderId == viewModel.currentUserId,
                                    onReply = { replyTarget = msg },
                                    onReact = { emoji -> viewModel.toggleReaction(groupId, msg, emoji) }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = replyTarget != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Replying to ${replyTarget?.senderName ?: "member"}", style = MaterialTheme.typography.labelMedium)
                            Text(replyTarget?.text.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        IconButton(onClick = { replyTarget = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Share image")
                    }
                    OutlinedTextField(
                        value = composer,
                        onValueChange = { composer = it },
                        placeholder = { Text("Message group...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val g = group ?: return@IconButton
                            if (composer.isNotBlank()) {
                                viewModel.sendGroupMessage(g, composer, replyTarget)
                                composer = ""
                                replyTarget = null
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(OutgoingBubbleStart, OutgoingBubbleEnd)))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupMessageBubble(
    message: GroupMessage,
    isMine: Boolean,
    onReply: () -> Unit,
    onReact: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                shape = shape,
                color = if (isMine) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .then(
                        if (isMine) Modifier.background(Brush.linearGradient(listOf(OutgoingBubbleStart, OutgoingBubbleEnd)))
                        else Modifier
                    )
                    .combinedClickable(onClick = {}, onLongClick = { showMenu = true })
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (!isMine && !message.senderName.isNullOrBlank()) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "${message.replyToSender ?: ""}: ${message.replyToText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                maxLines = 2,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    if (message.messageType == MessageType.IMAGE && !message.mediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Group photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text(message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateTimeUtils.formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.78f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    listOf("❤️", "👍", "😂", "🔥").forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.clickable { showMenu = false; onReact(emoji) }.padding(6.dp)
                        )
                    }
                }
                DropdownMenuItem(
                    text = { Text("Reply") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, null) },
                    onClick = { showMenu = false; onReply() }
                )
            }
        }

        if (message.reactions.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(50), tonalElevation = 2.dp, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = message.reactions.values.distinct().joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    viewModel: GroupsViewModel,
    onBack: () -> Unit,
    onExitedGroup: () -> Unit
) {
    val groupFlow = remember(groupId) { viewModel.observeGroup(groupId) }
    val group by groupFlow.collectAsStateWithLifecycle()
    val usersMap by viewModel.allUsers.collectAsStateWithLifecycle()

    var showAddMember by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val g = group
    val isAdmin = g != null && viewModel.currentUserId in g.adminIds

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Group Info") }
            )
        }
    ) { innerPadding ->
        if (g == null) {
            LoadingStateBox("Loading group details...", modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UserAvatar(g.name, g.imageUrl, size = 86.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(g.name, style = MaterialTheme.typography.headlineMedium)
                        if (!g.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(g.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = { showEditDialog = true }) {
                                Text("Edit Group Details")
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Members (${g.memberIds.size})", style = MaterialTheme.typography.titleMedium)
                        if (isAdmin) {
                            TextButton(onClick = { showAddMember = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add")
                            }
                        }
                    }
                }

                items(g.memberIds, key = { it }) { memberId ->
                    val member = usersMap[memberId]
                    val memberIsAdmin = memberId in g.adminIds
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(member?.displayName ?: "Member", member?.photoUrl, size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (memberId == viewModel.currentUserId) "You" else (member?.displayName ?: memberId),
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (memberIsAdmin) {
                                Text("Admin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (isAdmin && memberId != viewModel.currentUserId) {
                            IconButton(onClick = { viewModel.toggleAdmin(g.id, memberId, !memberIsAdmin) }) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Toggle admin")
                            }
                            IconButton(onClick = { viewModel.removeMember(g.id, memberId) }) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "Remove member", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.leaveGroup(g, onExitedGroup) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leave Group")
                    }
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.deleteGroup(g.id, onExitedGroup) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Group")
                        }
                    }
                }
            }
        }
    }

    if (showAddMember && g != null) {
        val candidates = usersMap.values.filter { it.uid !in g.memberIds }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add Member") },
            text = {
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(candidates, key = { it.uid }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addMember(g.id, user.uid)
                                    showAddMember = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(user.displayName, user.photoUrl, size = 38.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(user.displayName)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddMember = false }) { Text("Done") } }
        )
    }

    if (showEditDialog && g != null) {
        var newName by remember { mutableStateOf(g.name) }
        var newDesc by remember { mutableStateOf(g.description.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") })
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateGroupInfo(g.id, newName, newDesc)
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }
}
