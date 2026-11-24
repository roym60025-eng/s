package com.example.notifications

import android.content.Context
import android.content.Intent
import com.example.data.local.PreferencesDataStore
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GroupRepository
import com.example.domain.model.CallStatus
import com.example.domain.model.CallType
import com.example.domain.model.UserPrivateSettings
import com.example.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class NotificationNavTarget(
    val type: String,
    val targetId: String
)

class IncomingNotificationMonitor(
    private val appContext: Context,
    private val authRepository: AuthAndUserRepository,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val callRepository: CallRepository,
    private val preferencesDataStore: PreferencesDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var monitorJob: Job? = null
    private var observedUid: String? = null

    @Volatile
    private var isAppInForeground: Boolean = true

    @Volatile
    private var activeChatId: String? = null

    @Volatile
    private var activeGroupId: String? = null

    private val _pendingNavTarget = MutableStateFlow<NotificationNavTarget?>(null)
    val pendingNavTarget: StateFlow<NotificationNavTarget?> = _pendingNavTarget.asStateFlow()

    fun setAppForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
    }

    fun setActiveChat(chatId: String?) {
        activeChatId = chatId
    }

    fun setActiveGroup(groupId: String?) {
        activeGroupId = groupId
    }

    fun onNotificationIntent(intent: Intent?) {
        val type = intent?.getStringExtra(ManojSonaliMessagingService.EXTRA_NAV_TYPE) ?: return
        val targetId = intent.getStringExtra(ManojSonaliMessagingService.EXTRA_NAV_ID) ?: return
        _pendingNavTarget.value = NotificationNavTarget(type, targetId)
        intent.removeExtra(ManojSonaliMessagingService.EXTRA_NAV_TYPE)
        intent.removeExtra(ManojSonaliMessagingService.EXTRA_NAV_ID)
    }

    fun consumePendingNavTarget() {
        _pendingNavTarget.value = null
    }

    fun start(uid: String) {
        if (observedUid == uid && monitorJob?.isActive == true) return
        stop()
        observedUid = uid

        monitorJob = scope.launch {
            val usersFlow = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
            val contactsFlow = MutableStateFlow<Map<String, String>>(emptyMap())
            val settingsFlow = MutableStateFlow<UserPrivateSettings?>(null)

            launch {
                authRepository.observeDiscoverableUsers()
                    .catch { emit(emptyList()) }
                    .collectLatest { list ->
                        usersFlow.value = list.associateBy { it.uid }
                    }
            }

            launch {
                authRepository.observeSavedContacts()
                    .catch { emit(emptyList()) }
                    .collectLatest { list ->
                        contactsFlow.value = list.associate { it.contactUid to it.displayName }
                    }
            }

            launch {
                authRepository.observePrivateSettings(uid)
                    .catch { emit(null) }
                    .collectLatest { settings ->
                        settingsFlow.value = settings
                    }
            }

            // 1. Direct Chats Listener
            launch {
                var initialized = false
                val seenChatMessageTime = mutableMapOf<String, Long>()
                val seenChatUnread = mutableMapOf<String, Int>()

                combine(
                    chatRepository.observeMyChats().catch { emit(emptyList()) },
                    usersFlow,
                    contactsFlow,
                    settingsFlow
                ) { chats, usersMap, contactNames, settings ->
                    ChatNotificationSnapshot(chats, usersMap, contactNames, settings)
                }.collectLatest { snap ->
                    val settings = snap.settings
                    val blocked = settings?.blockedUserIds.orEmpty().toSet()
                    val allowMessageNotifications = settings?.notifyMessages ?: true

                    if (!initialized) {
                        snap.chats.forEach { room ->
                            seenChatMessageTime[room.id] = room.lastMessageTime?.toDate()?.time ?: 0L
                            seenChatUnread[room.id] = room.unreadFor(uid)
                        }
                        initialized = true
                        return@collectLatest
                    }

                    snap.chats.forEach { room ->
                        val msgTimeMs = room.lastMessageTime?.toDate()?.time ?: 0L
                        val unreadCount = room.unreadFor(uid)
                        val prevTime = seenChatMessageTime[room.id] ?: 0L
                        val prevUnread = seenChatUnread[room.id] ?: 0

                        seenChatMessageTime[room.id] = maxOf(prevTime, msgTimeMs)
                        seenChatUnread[room.id] = unreadCount

                        val peerId = room.peerId(uid)
                        val isNewIncoming = room.lastMessage.isNotBlank() &&
                            room.lastMessageSenderId != null &&
                            room.lastMessageSenderId != uid &&
                            (msgTimeMs > prevTime || unreadCount > prevUnread)

                        val isViewingThisChat = isAppInForeground && activeChatId == room.id
                        val isMuted = uid in room.mutedBy
                        val isBlocked = peerId in blocked

                        if (isNewIncoming && allowMessageNotifications && !isViewingThisChat && !isMuted && !isBlocked) {
                            val senderName = snap.contactNames[peerId]
                                ?: snap.usersMap[peerId]?.displayName
                                ?: "New message"
                            ManojSonaliMessagingService.showNotification(
                                context = appContext,
                                type = "message",
                                title = senderName,
                                body = room.lastMessage,
                                targetId = room.id
                            )
                            if (msgTimeMs > 0L) {
                                preferencesDataStore.updateLastNotifiedMessageMs(msgTimeMs)
                            }
                        }
                    }
                }
            }

            // 2. Group Chats Listener
            launch {
                var initialized = false
                val seenGroupTime = mutableMapOf<String, Long>()

                combine(
                    groupRepository.observeMyGroups().catch { emit(emptyList()) },
                    usersFlow,
                    settingsFlow
                ) { groups, usersMap, settings ->
                    Triple(groups, usersMap[uid]?.displayName, settings)
                }.collectLatest { (groups, myDisplayName, settings) ->
                    val allowGroupNotifications = settings?.notifyGroups ?: true

                    if (!initialized) {
                        groups.forEach { group ->
                            seenGroupTime[group.id] = group.lastMessageTime?.toDate()?.time ?: 0L
                        }
                        initialized = true
                        return@collectLatest
                    }

                    groups.forEach { group ->
                        val msgTimeMs = group.lastMessageTime?.toDate()?.time ?: 0L
                        val prevTime = seenGroupTime[group.id] ?: 0L
                        seenGroupTime[group.id] = maxOf(prevTime, msgTimeMs)

                        val isNew = group.lastMessage.isNotBlank() &&
                            msgTimeMs > prevTime &&
                            group.lastMessageSenderName != null &&
                            group.lastMessageSenderName != myDisplayName

                        val isViewingThisGroup = isAppInForeground && activeGroupId == group.id
                        val isMuted = uid in group.mutedBy

                        if (isNew && allowGroupNotifications && !isViewingThisGroup && !isMuted) {
                            val body = "${group.lastMessageSenderName}: ${group.lastMessage}"
                            ManojSonaliMessagingService.showNotification(
                                context = appContext,
                                type = "group",
                                title = group.name,
                                body = body,
                                targetId = group.id
                            )
                            if (msgTimeMs > 0L) {
                                preferencesDataStore.updateLastNotifiedMessageMs(msgTimeMs)
                            }
                        }
                    }
                }
            }

            // 3. Incoming Call Listener (when backgrounded)
            launch {
                var initialized = false
                val seenCallIds = mutableSetOf<String>()

                combine(
                    callRepository.observeCallHistory().catch { emit(emptyList()) },
                    settingsFlow
                ) { calls, settings ->
                    calls to settings
                }.collectLatest { (calls, settings) ->
                    val allowCalls = settings?.notifyCalls ?: true
                    val blocked = settings?.blockedUserIds.orEmpty().toSet()

                    if (!initialized) {
                        calls.forEach { seenCallIds.add(it.id) }
                        initialized = true
                        return@collectLatest
                    }

                    calls.forEach { call ->
                        if (call.id !in seenCallIds) {
                            seenCallIds.add(call.id)
                            val isIncomingRinging = call.receiverId == uid &&
                                call.statusEnum == CallStatus.RINGING &&
                                call.callerId !in blocked
                            if (isIncomingRinging && allowCalls && !isAppInForeground) {
                                val kind = if (call.typeEnum == CallType.VIDEO) "video" else "voice"
                                ManojSonaliMessagingService.showNotification(
                                    context = appContext,
                                    type = "call",
                                    title = call.callerName ?: "Incoming Call",
                                    body = "Incoming $kind call",
                                    targetId = call.id
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        observedUid = null
    }

    private data class ChatNotificationSnapshot(
        val chats: List<com.example.domain.model.ChatRoom>,
        val usersMap: Map<String, UserProfile>,
        val contactNames: Map<String, String>,
        val settings: UserPrivateSettings?
    )
}
