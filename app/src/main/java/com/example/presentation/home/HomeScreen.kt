package com.example.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.contacts.AddContactTab
import com.example.domain.model.ChatRoom
import com.example.domain.model.DeliveryStatus
import com.example.domain.model.GroupChat
import com.example.domain.model.SavedContact
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.domain.model.UserStatus
import com.example.presentation.components.BrandLogoMark
import com.example.presentation.components.DeliveryTickIcon
import com.example.presentation.components.EmptyIllustrationState
import com.example.presentation.components.ErrorBannerCard
import com.example.presentation.components.LoadingStateBox
import com.example.presentation.components.UserAvatar
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SoftBlueTertiary
import com.example.ui.theme.VioletSecondary
import com.example.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenChat: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onCreateStatus: () -> Unit,
    onViewUserStatuses: (String) -> Unit,
    onOpenAddContact: (AddContactTab) -> Unit,
    onStartVoiceCall: (UserProfile) -> Unit,
    onStartVideoCall: (UserProfile) -> Unit,
    callsTabContent: @Composable (UiState<List<com.example.domain.model.CallSession>>, Map<String, UserProfile>) -> Unit,
    profileTabContent: @Composable () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val privateSettings by viewModel.privateSettings.collectAsStateWithLifecycle()
    val usersMap by viewModel.usersMap.collectAsStateWithLifecycle()
    val savedContacts by viewModel.savedContacts.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val groupsState by viewModel.groupsState.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val callsState by viewModel.callHistoryState.collectAsStateWithLifecycle()

    var showNewChatSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    header = {
                        BrandLogoMark(size = 38.dp, modifier = Modifier.padding(top = 12.dp))
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HomeTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = { TabIcon(tab, selected) },
                            label = { Text(tabLabel(tab)) }
                        )
                    }
                }
            }

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!useRail) {
                                        BrandLogoMark(size = 32.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Text(
                                        text = stringResource(R.string.brand_title_upper),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { onOpenAddContact(AddContactTab.QR_SCAN) },
                                    modifier = Modifier.testTag("home_qr_scan_button")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR to add contact")
                                }
                                IconButton(
                                    onClick = { viewModel.toggleSearch(!isSearchActive) },
                                    modifier = Modifier.testTag("home_search_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                }
                                IconButton(
                                    onClick = { showNewChatSheet = true },
                                    modifier = Modifier.testTag("home_new_chat_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "New Chat")
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.selectTab(HomeTab.PROFILE) }
                                        .testTag("home_profile_avatar")
                                ) {
                                    UserAvatar(
                                        name = currentProfile?.displayName ?: "M",
                                        photoUrl = currentProfile?.photoUrl,
                                        size = 36.dp,
                                        isOnline = currentProfile?.online == true
                                    )
                                }
                            }
                        )

                        AnimatedVisibility(visible = isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = viewModel::onSearchQueryChanged,
                                placeholder = { Text("Search people, messages, or groups...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("home_search_input")
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            HomeTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { TabIcon(tab, selected) },
                                    label = { Text(tabLabel(tab)) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    when (selectedTab) {
                        HomeTab.CHATS -> FloatingActionButton(
                            onClick = { showNewChatSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("fab_new_chat")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Start new chat")
                        }
                        HomeTab.GROUPS -> FloatingActionButton(
                            onClick = onCreateGroup,
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("fab_new_group")
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Create group")
                        }
                        else -> Unit
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 820.dp).fillMaxSize()) {
                        when (selectedTab) {
                            HomeTab.CHATS -> ChatsTabContent(
                                currentUserId = viewModel.currentUserId,
                                currentProfile = currentProfile,
                                blockedIds = privateSettings?.blockedUserIds.orEmpty(),
                                chatsState = chatsState,
                                usersMap = usersMap,
                                statuses = statuses,
                                onCreateStatus = onCreateStatus,
                                onViewStatuses = onViewUserStatuses,
                                onOpenChat = onOpenChat,
                                onNewChat = { showNewChatSheet = true },
                                onTogglePin = viewModel::togglePin,
                                onToggleMute = viewModel::toggleMute
                            )

                            HomeTab.GROUPS -> GroupsTabContent(
                                groupsState = groupsState,
                                onOpenGroup = onOpenGroup,
                                onCreateGroup = onCreateGroup
                            )

                            HomeTab.CALLS -> callsTabContent(callsState, usersMap)

                            HomeTab.PROFILE -> profileTabContent()
                        }
                    }
                }
            }
        }
    }

    if (showNewChatSheet) {
        NewChatUserDiscoverySheet(
            currentUserId = viewModel.currentUserId,
            blockedIds = privateSettings?.blockedUserIds.orEmpty(),
            savedContacts = savedContacts,
            users = usersMap.values.filter { it.uid != viewModel.currentUserId },
            usersMap = usersMap,
            onDismiss = { showNewChatSheet = false },
            onAddByNumber = {
                showNewChatSheet = false
                onOpenAddContact(AddContactTab.PHONE)
            },
            onScanQr = {
                showNewChatSheet = false
                onOpenAddContact(AddContactTab.QR_SCAN)
            },
            onSelectUid = { targetUid ->
                showNewChatSheet = false
                viewModel.startDirectChat(targetUid, onOpenChat)
            },
            onVoiceCall = { user ->
                showNewChatSheet = false
                onStartVoiceCall(user)
            },
            onVideoCall = { user ->
                showNewChatSheet = false
                onStartVideoCall(user)
            }
        )
    }
}

@Composable
private fun ChatsTabContent(
    currentUserId: String,
    currentProfile: UserProfile?,
    blockedIds: List<String>,
    chatsState: UiState<List<ChatRoom>>,
    usersMap: Map<String, UserProfile>,
    statuses: List<UserStatus>,
    onCreateStatus: () -> Unit,
    onViewStatuses: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onToggleMute: (String, Boolean) -> Unit
) {
    val groupedStatuses = remember(statuses) {
        statuses.groupBy { it.userId }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            StatusStoriesStrip(
                currentUserId = currentUserId,
                currentProfile = currentProfile,
                groupedStatuses = groupedStatuses,
                onCreateStatus = onCreateStatus,
                onViewStatuses = onViewStatuses
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }

        when (chatsState) {
            is UiState.Loading -> item {
                Box(modifier = Modifier.fillParentMaxHeight(0.6f)) {
                    LoadingStateBox("Loading conversations...")
                }
            }
            is UiState.Error -> item {
                ErrorBannerCard(
                    message = chatsState.message,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is UiState.Success -> {
                val visibleChats = chatsState.data.filter { room ->
                    room.peerId(currentUserId) !in blockedIds
                }
                if (visibleChats.isEmpty()) {
                    item {
                        EmptyIllustrationState(
                            drawableRes = R.drawable.img_empty_chats,
                            title = stringResource(R.string.empty_chats_title),
                            subtitle = stringResource(R.string.empty_chats_subtitle),
                            actionLabel = "Start a Chat",
                            onAction = onNewChat
                        )
                    }
                } else {
                    items(visibleChats, key = { it.id }) { room ->
                        val peerId = room.peerId(currentUserId)
                        val peer = usersMap[peerId]
                        val isPinned = currentUserId in room.pinnedBy
                        val isMuted = currentUserId in room.mutedBy
                        ChatRoomRow(
                            currentUserId = currentUserId,
                            room = room,
                            peer = peer,
                            hasStatus = groupedStatuses.containsKey(peerId),
                            isPinned = isPinned,
                            isMuted = isMuted,
                            onClick = { onOpenChat(room.id) },
                            onTogglePin = { onTogglePin(room.id, isPinned) },
                            onToggleMute = { onToggleMute(room.id, isMuted) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusStoriesStrip(
    currentUserId: String,
    currentProfile: UserProfile?,
    groupedStatuses: Map<String, List<UserStatus>>,
    onCreateStatus: () -> Unit,
    onViewStatuses: (String) -> Unit
) {
    val ownHasStatus = groupedStatuses.containsKey(currentUserId)
    val peerStories = groupedStatuses.entries.filter { it.key != currentUserId }

    Column(modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
        Text(
            text = "24h Stories",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable {
                            if (ownHasStatus) onViewStatuses(currentUserId) else onCreateStatus()
                        }
                        .testTag("my_status_item")
                ) {
                    Box {
                        UserAvatar(
                            name = currentProfile?.displayName ?: "You",
                            photoUrl = currentProfile?.photoUrl,
                            size = 58.dp,
                            hasStatusRing = ownHasStatus
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary))
                                )
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .clickable { onCreateStatus() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add status",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your Story",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            items(peerStories, key = { it.key }) { (userId, list) ->
                val latest = list.firstOrNull() ?: return@items
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onViewStatuses(userId) }
                ) {
                    UserAvatar(
                        name = latest.userName,
                        photoUrl = latest.userPhotoUrl,
                        size = 58.dp,
                        hasStatusRing = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = latest.userName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRoomRow(
    currentUserId: String,
    room: ChatRoom,
    peer: UserProfile?,
    hasStatus: Boolean,
    isPinned: Boolean,
    isMuted: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val unread = room.unreadFor(currentUserId)
    val isPeerTyping = room.typingUserIds.any { it != currentUserId }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = peer?.displayName ?: "Contact",
                photoUrl = peer?.photoUrl,
                size = 54.dp,
                isOnline = peer?.online == true,
                hasStatusRing = hasStatus
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = peer?.displayName ?: "Manoj & Sonali Member",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp).padding(end = 4.dp)
                        )
                    }
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = DateTimeUtils.formatChatListTime(room.lastMessageTime ?: room.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unread > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (room.lastMessageSenderId == currentUserId && !isPeerTyping) {
                        DeliveryTickIcon(status = DeliveryStatus.DELIVERED)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = when {
                            isPeerTyping -> "typing..."
                            room.lastMessage.isNotBlank() -> room.lastMessage
                            else -> "Tap to say hello"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPeerTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (unread > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = if (unread > 99) "99+" else unread.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(if (isPinned) "Unpin chat" else "Pin chat") },
                onClick = { showMenu = false; onTogglePin() }
            )
            DropdownMenuItem(
                text = { Text(if (isMuted) "Unmute notifications" else "Mute notifications") },
                onClick = { showMenu = false; onToggleMute() }
            )
        }
    }
}

@Composable
private fun GroupsTabContent(
    groupsState: UiState<List<GroupChat>>,
    onOpenGroup: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    when (groupsState) {
        is UiState.Loading -> LoadingStateBox("Loading groups...")
        is UiState.Error -> ErrorBannerCard(groupsState.message, modifier = Modifier.padding(16.dp))
        is UiState.Success -> {
            if (groupsState.data.isEmpty()) {
                EmptyIllustrationState(
                    drawableRes = R.drawable.img_empty_chats,
                    title = "No group spaces yet",
                    subtitle = "Create a group to share messages, media, and voice notes with multiple members.",
                    actionLabel = "Create Group",
                    onAction = onCreateGroup
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)
                ) {
                    items(groupsState.data, key = { it.id }) { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenGroup(group.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = group.name,
                                photoUrl = group.imageUrl,
                                size = 54.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = DateTimeUtils.formatChatListTime(group.lastMessageTime ?: group.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val preview = if (group.lastMessageSenderName != null) {
                                    "${group.lastMessageSenderName}: ${group.lastMessage}"
                                } else {
                                    group.lastMessage.ifBlank { "${group.memberIds.size} members" }
                                }
                                Text(
                                    text = preview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatUserDiscoverySheet(
    currentUserId: String,
    blockedIds: List<String>,
    savedContacts: List<SavedContact>,
    users: List<UserProfile>,
    usersMap: Map<String, UserProfile>,
    onDismiss: () -> Unit,
    onAddByNumber: () -> Unit,
    onScanQr: () -> Unit,
    onSelectUid: (String) -> Unit,
    onVoiceCall: (UserProfile) -> Unit,
    onVideoCall: (UserProfile) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val filteredSaved = remember(savedContacts, blockedIds, filter) {
        savedContacts.filter { c ->
            c.contactUid != currentUserId &&
                c.contactUid !in blockedIds &&
                (filter.isBlank() ||
                    c.displayName.contains(filter, ignoreCase = true) ||
                    (c.phoneNumber?.contains(filter, ignoreCase = true) == true) ||
                    c.about.contains(filter, ignoreCase = true))
        }
    }
    val savedIds = remember(savedContacts) { savedContacts.map { it.contactUid }.toSet() }
    val filteredUsers = remember(users, blockedIds, savedIds, filter) {
        users.filter { u ->
            u.uid != currentUserId &&
                u.uid !in blockedIds &&
                u.uid !in savedIds &&
                (filter.isBlank() || u.displayName.contains(filter, ignoreCase = true) || u.about.contains(filter, ignoreCase = true))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Start a Conversation", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add a contact by number, scan a QR code, or pick someone below",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onAddByNumber,
                    modifier = Modifier.weight(1f).testTag("sheet_add_by_phone_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add by Number")
                }
                FilledTonalButton(
                    onClick = onScanQr,
                    modifier = Modifier.weight(1f).testTag("sheet_scan_qr_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan QR")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Search name or phone number...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (filteredSaved.isEmpty() && filteredUsers.isEmpty()) {
                EmptyIllustrationState(
                    title = "No contacts found",
                    subtitle = "Tap Add by Number or Scan QR above to add a friend."
                )
            } else {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    if (filteredSaved.isNotEmpty()) {
                        item(key = "header_saved") {
                            Text(
                                text = "My Contacts",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        items(filteredSaved, key = { "saved_${it.contactUid}" }) { contact ->
                            val liveUser = usersMap[contact.contactUid]
                            val callTarget = liveUser ?: UserProfile(
                                uid = contact.contactUid,
                                displayName = contact.displayName,
                                photoUrl = contact.photoUrl,
                                about = contact.about
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUid(contact.contactUid) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = liveUser?.displayName ?: contact.displayName,
                                    photoUrl = liveUser?.photoUrl ?: contact.photoUrl,
                                    size = 48.dp,
                                    isOnline = liveUser?.online == true
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = liveUser?.displayName ?: contact.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = contact.phoneNumber ?: (liveUser?.about ?: contact.about),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onVoiceCall(callTarget) }) {
                                    Icon(Icons.Outlined.Call, contentDescription = "Voice call")
                                }
                            }
                        }
                    }

                    if (filteredUsers.isNotEmpty()) {
                        item(key = "header_directory") {
                            Text(
                                text = "Discover People",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                            )
                        }
                        items(filteredUsers, key = { it.uid }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUid(user.uid) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = user.displayName,
                                    photoUrl = user.photoUrl,
                                    size = 48.dp,
                                    isOnline = user.online
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        user.about,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onVoiceCall(user) }) {
                                    Icon(Icons.Outlined.Call, contentDescription = "Voice call")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TabIcon(tab: HomeTab, selected: Boolean) {
    val icon = when (tab) {
        HomeTab.CHATS -> if (selected) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat
        HomeTab.GROUPS -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
        HomeTab.CALLS -> if (selected) Icons.Filled.Call else Icons.Outlined.Call
        HomeTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
    }
    Icon(icon, contentDescription = tabLabel(tab))
}

@Composable
private fun tabLabel(tab: HomeTab): String = when (tab) {
    HomeTab.CHATS -> stringResource(R.string.tab_chats)
    HomeTab.GROUPS -> stringResource(R.string.tab_groups)
    HomeTab.CALLS -> stringResource(R.string.tab_calls)
    HomeTab.PROFILE -> stringResource(R.string.tab_profile)
}
