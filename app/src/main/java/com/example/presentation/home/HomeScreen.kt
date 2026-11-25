package com.example.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
import androidx.compose.material.icons.filled.Palette
import com.example.ui.theme.AppThemeTokens
import com.example.utils.DateTimeUtils

private enum class ChatListFilter {
    ALL, DIRECT, GROUPS
}

private sealed interface FeedItem {
    val sortTimeMs: Long
    val pinned: Boolean

    data class Direct(
        val room: ChatRoom,
        override val sortTimeMs: Long,
        override val pinned: Boolean
    ) : FeedItem

    data class Group(
        val group: GroupChat,
        override val sortTimeMs: Long,
        override val pinned: Boolean = false
    ) : FeedItem
}

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
    onOpenMyQr: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartVoiceCall: (UserProfile) -> Unit,
    onStartVideoCall: (UserProfile) -> Unit,
    callsTabContent: @Composable (UiState<List<com.example.domain.model.CallSession>>, Map<String, UserProfile>) -> Unit
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
    var showOverflowMenu by remember { mutableStateOf(false) }
    var chatFilter by remember { mutableStateOf(ChatListFilter.ALL) }

    val palette = AppThemeTokens.current
    val headerColor = palette.headerColor

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
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                topBar = {
                    Surface(
                        color = headerColor,
                        contentColor = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(start = 18.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (selectedTab) {
                                        HomeTab.CHATS -> stringResource(R.string.brand_title_upper)
                                        HomeTab.STATUS -> stringResource(R.string.tab_status)
                                        HomeTab.CALLS -> stringResource(R.string.tab_calls)
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { viewModel.toggleSearch(!isSearchActive) },
                                    modifier = Modifier.testTag("home_search_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White
                                    )
                                }

                                if (selectedTab == HomeTab.STATUS) {
                                    IconButton(
                                        onClick = onCreateStatus,
                                        modifier = Modifier.testTag("status_header_add_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Add status",
                                            tint = Color.White
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { onOpenAddContact(AddContactTab.QR_SCAN) },
                                        modifier = Modifier.testTag("home_qr_scan_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan QR to add contact",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { showOverflowMenu = true },
                                        modifier = Modifier.testTag("home_more_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More options",
                                            tint = Color.White
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("My Profile") },
                                            leadingIcon = { Icon(Icons.Default.Person, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onOpenProfile()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("New Group") },
                                            leadingIcon = { Icon(Icons.Default.GroupAdd, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onCreateGroup()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add Contact") },
                                            leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onOpenAddContact(AddContactTab.PHONE)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("My QR Code") },
                                            leadingIcon = { Icon(Icons.Default.QrCode2, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onOpenMyQr()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Themes & Settings") },
                                            leadingIcon = { Icon(Icons.Default.Palette, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onOpenSettings()
                                            }
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = isSearchActive) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = viewModel::onSearchQueryChanged,
                                    placeholder = { Text("Search people, messages, or groups...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                                        .testTag("home_search_input")
                                )
                            }

                            // Top sub-tabs bar matching Showcase #4 ("Chats | Status | Calls")
                            if (selectedTab == HomeTab.CHATS && !useRail) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    HomeTab.entries.forEach { tab ->
                                        val isSelected = selectedTab == tab
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.selectTab(tab) }
                                                .padding(top = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = tabLabel(tab),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f),
                                                modifier = Modifier.padding(bottom = 10.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.7f)
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                                    .background(if (isSelected) Color.White else Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!useRail && (selectedTab == HomeTab.STATUS || selectedTab == HomeTab.CALLS || selectedTab == HomeTab.CHATS)) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            NavigationBar(
                                windowInsets = WindowInsets.navigationBars,
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                HomeTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(tab) },
                                        icon = { TabIcon(tab, selected) },
                                        label = {
                                            Text(
                                                text = tabLabel(tab),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = palette.buttonColor,
                                            selectedTextColor = palette.buttonColor,
                                            indicatorColor = palette.accentColor.copy(alpha = 0.14f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    when (selectedTab) {
                        HomeTab.CHATS -> FloatingActionButton(
                            onClick = { showNewChatSheet = true },
                            shape = CircleShape,
                            containerColor = palette.accentColor,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_new_chat")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Start new chat")
                        }

                        HomeTab.STATUS -> FloatingActionButton(
                            onClick = onCreateStatus,
                            shape = CircleShape,
                            containerColor = palette.accentColor,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_new_status")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Add status")
                        }

                        HomeTab.CALLS -> FloatingActionButton(
                            onClick = { showNewChatSheet = true },
                            shape = CircleShape,
                            containerColor = palette.accentColor,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_new_call")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Start new call")
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 820.dp)
                            .fillMaxSize()
                    ) {
                        when (selectedTab) {
                            HomeTab.CHATS -> ChatsHomeContent(
                                currentUserId = viewModel.currentUserId,
                                blockedIds = privateSettings?.blockedUserIds.orEmpty(),
                                chatsState = chatsState,
                                groupsState = groupsState,
                                usersMap = usersMap,
                                statuses = statuses,
                                filter = chatFilter,
                                onFilterChange = { chatFilter = it },
                                onOpenChat = onOpenChat,
                                onOpenGroup = onOpenGroup,
                                onNewChat = { showNewChatSheet = true },
                                onTogglePin = viewModel::togglePin,
                                onToggleMute = viewModel::toggleMute
                            )

                            HomeTab.STATUS -> StatusTabContent(
                                currentUserId = viewModel.currentUserId,
                                currentProfile = currentProfile,
                                statuses = statuses,
                                searchQuery = searchQuery,
                                onCreateStatus = onCreateStatus,
                                onViewStatuses = onViewUserStatuses
                            )

                            HomeTab.CALLS -> callsTabContent(callsState, usersMap)
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
            onCreateGroup = {
                showNewChatSheet = false
                onCreateGroup()
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
private fun ChatsHomeContent(
    currentUserId: String,
    blockedIds: List<String>,
    chatsState: UiState<List<ChatRoom>>,
    groupsState: UiState<List<GroupChat>>,
    usersMap: Map<String, UserProfile>,
    statuses: List<UserStatus>,
    filter: ChatListFilter,
    onFilterChange: (ChatListFilter) -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onNewChat: () -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onToggleMute: (String, Boolean) -> Unit
) {
    val groupedStatuses = remember(statuses) { statuses.groupBy { it.userId } }

    when (chatsState) {
        is UiState.Loading -> LoadingStateBox("Loading conversations...")
        is UiState.Error -> ErrorBannerCard(
            message = chatsState.message,
            modifier = Modifier.padding(16.dp)
        )
        is UiState.Success -> {
            val directRooms = chatsState.data.filter { it.peerId(currentUserId) !in blockedIds }
            val groups = (groupsState as? UiState.Success)?.data.orEmpty()

            val feedItems = remember(directRooms, groups, filter, currentUserId) {
                val list = buildList {
                    if (filter != ChatListFilter.GROUPS) {
                        directRooms.forEach { room ->
                            val t = (room.lastMessageTime ?: room.createdAt)?.toDate()?.time ?: 0L
                            add(FeedItem.Direct(room, t, currentUserId in room.pinnedBy))
                        }
                    }
                    if (filter != ChatListFilter.DIRECT) {
                        groups.forEach { group ->
                            val t = (group.lastMessageTime ?: group.createdAt)?.toDate()?.time ?: 0L
                            add(FeedItem.Group(group, t, false))
                        }
                    }
                }
                list.sortedWith(
                    compareByDescending<FeedItem> { it.pinned }
                        .thenByDescending { it.sortTimeMs }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                if (groups.isNotEmpty()) {
                    item(key = "chat_filter_strip") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ChatListFilter.ALL to "All",
                                ChatListFilter.DIRECT to "Direct",
                                ChatListFilter.GROUPS to "Groups"
                            ).forEach { (chipFilter, title) ->
                                FilterChip(
                                    selected = filter == chipFilter,
                                    onClick = { onFilterChange(chipFilter) },
                                    label = { Text(title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppThemeTokens.current.accentColor.copy(alpha = 0.16f),
                                        selectedLabelColor = AppThemeTokens.current.buttonColor
                                    )
                                )
                            }
                        }
                    }
                }

                if (feedItems.isEmpty()) {
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
                    items(
                        items = feedItems,
                        key = { item ->
                            when (item) {
                                is FeedItem.Direct -> "chat_${item.room.id}"
                                is FeedItem.Group -> "group_${item.group.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is FeedItem.Direct -> {
                                val room = item.room
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

                            is FeedItem.Group -> {
                                GroupFeedRow(
                                    group = item.group,
                                    onClick = { onOpenGroup(item.group.id) }
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupFeedRow(
    group: GroupChat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!group.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = group.imageUrl,
                contentDescription = "${group.name} avatar",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(AppThemeTokens.current.accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = group.name,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
            val preview = if (!group.lastMessageSenderName.isNullOrBlank() && group.lastMessage.isNotBlank()) {
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = AppThemeTokens.current.accentColor,
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
                        color = if (unread > 0) AppThemeTokens.current.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (room.lastMessageSenderId == currentUserId && !isPeerTyping) {
                        DeliveryTickIcon(status = DeliveryStatus.READ)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = when {
                            isPeerTyping -> "typing..."
                            room.lastMessage.isNotBlank() -> room.lastMessage
                            else -> "Tap to say hello"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPeerTyping) AppThemeTokens.current.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (unread > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = AppThemeTokens.current.accentColor
                        ) {
                            Text(
                                text = if (unread > 99) "99+" else unread.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
private fun StatusTabContent(
    currentUserId: String,
    currentProfile: UserProfile?,
    statuses: List<UserStatus>,
    searchQuery: String,
    onCreateStatus: () -> Unit,
    onViewStatuses: (String) -> Unit
) {
    val grouped = remember(statuses) { statuses.groupBy { it.userId } }
    val myStatuses = grouped[currentUserId].orEmpty()
    val q = searchQuery.trim().lowercase()

    val peerEntries = remember(grouped, currentUserId, q) {
        grouped.entries
            .filter { it.key != currentUserId }
            .filter { (_, list) ->
                q.isEmpty() || (list.firstOrNull()?.userName?.lowercase()?.contains(q) == true)
            }
    }

    val recentUpdates = remember(peerEntries, currentUserId) {
        peerEntries.filter { (_, list) ->
            list.any { currentUserId !in it.viewerIds }
        }
    }
    val viewedUpdates = remember(peerEntries, currentUserId) {
        peerEntries.filter { (_, list) ->
            list.all { currentUserId in it.viewerIds }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp, top = 6.dp)
    ) {
        // My Status Header Row matching Showcase #6
        item(key = "my_status_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (myStatuses.isNotEmpty()) onViewStatuses(currentUserId) else onCreateStatus()
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("my_status_item"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    UserAvatar(
                        name = currentProfile?.displayName ?: "You",
                        photoUrl = currentProfile?.photoUrl,
                        size = 56.dp,
                        hasStatusRing = myStatuses.isNotEmpty()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(AppThemeTokens.current.accentColor)
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
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (myStatuses.isNotEmpty()) {
                            DateTimeUtils.formatStatusAge(myStatuses.first().createdAt)
                        } else {
                            "Tap to add status update"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (recentUpdates.isNotEmpty()) {
            item(key = "recent_header") {
                Text(
                    text = "Recent Updates",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(recentUpdates, key = { "recent_${it.key}" }) { (uid, list) ->
                val latest = list.firstOrNull() ?: return@items
                StatusContactRow(
                    userName = latest.userName,
                    photoUrl = latest.userPhotoUrl,
                    timeText = DateTimeUtils.formatStatusAge(latest.createdAt),
                    isViewed = false,
                    onClick = { onViewStatuses(uid) }
                )
            }
        }

        if (viewedUpdates.isNotEmpty()) {
            item(key = "viewed_header") {
                Text(
                    text = "Viewed Updates",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(viewedUpdates, key = { "viewed_${it.key}" }) { (uid, list) ->
                val latest = list.firstOrNull() ?: return@items
                StatusContactRow(
                    userName = latest.userName,
                    photoUrl = latest.userPhotoUrl,
                    timeText = DateTimeUtils.formatStatusAge(latest.createdAt),
                    isViewed = true,
                    onClick = { onViewStatuses(uid) }
                )
            }
        }

        if (recentUpdates.isEmpty() && viewedUpdates.isEmpty()) {
            item(key = "empty_status_hint") {
                EmptyIllustrationState(
                    title = "No status updates yet",
                    subtitle = "Share a text, photo, or video update that stays live for 24 hours.",
                    actionLabel = "Add Status Update",
                    onAction = onCreateStatus
                )
            }
        }
    }
}

@Composable
private fun StatusContactRow(
    userName: String,
    photoUrl: String?,
    timeText: String,
    isViewed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = userName,
            photoUrl = photoUrl,
            size = 54.dp,
            hasStatusRing = true,
            statusViewed = isViewed
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    onCreateGroup: () -> Unit,
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
                text = "Create a group, add a contact by number, scan QR, or pick someone below",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCreateGroup,
                    modifier = Modifier.weight(1f).testTag("sheet_new_group_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Group", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    onClick = onAddByNumber,
                    modifier = Modifier.weight(1f).testTag("sheet_add_by_phone_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Number", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    onClick = onScanQr,
                    modifier = Modifier.weight(1f).testTag("sheet_scan_qr_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan QR", maxLines = 1, style = MaterialTheme.typography.labelLarge)
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
                    subtitle = "Tap Add Number or Scan QR above to add a friend."
                )
            } else {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    if (filteredSaved.isNotEmpty()) {
                        item(key = "header_saved") {
                            Text(
                                text = "My Contacts",
                                style = MaterialTheme.typography.labelLarge,
                                color = AppThemeTokens.current.buttonColor,
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
        HomeTab.STATUS -> if (selected) Icons.Filled.DonutLarge else Icons.Outlined.DonutLarge
        HomeTab.CALLS -> if (selected) Icons.Filled.Call else Icons.Outlined.Call
    }
    Icon(icon, contentDescription = tabLabel(tab))
}

@Composable
private fun tabLabel(tab: HomeTab): String = when (tab) {
    HomeTab.CHATS -> stringResource(R.string.tab_chats)
    HomeTab.STATUS -> stringResource(R.string.tab_status)
    HomeTab.CALLS -> stringResource(R.string.tab_calls)
}
