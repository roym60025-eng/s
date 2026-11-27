package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GroupRepository
import com.example.domain.model.CallSession
import com.example.domain.model.CallStatus
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatRoom
import com.example.domain.model.GroupChat
import com.example.domain.model.GroupMessage
import com.example.domain.model.SavedContact
import com.example.domain.model.UiState
import com.example.domain.model.UserPrivateSettings
import com.example.domain.model.UserProfile
import com.example.security.unwrapFirestoreError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeTab {
    CHATS, PHOTOS, CALLS
}

data class MessageSearchMatches(
    val chatMatches: Map<String, ChatMessage> = emptyMap(),
    val groupMatches: Map<String, GroupMessage> = emptyMap(),
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    val currentUserId: String,
    private val authRepository: AuthAndUserRepository,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val callRepository: CallRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HomeTab.CHATS)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val currentUserProfile: StateFlow<UserProfile?> = authRepository.observeUserProfile(currentUserId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val privateSettings: StateFlow<UserPrivateSettings?> = authRepository.observePrivateSettings(currentUserId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val usersMap: StateFlow<Map<String, UserProfile>> = authRepository.observeDiscoverableUsers()
        .map { list -> list.associateBy { it.uid } }
        .catch { unwrapFirestoreError(it); emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val savedContacts: StateFlow<List<SavedContact>> = authRepository.observeSavedContacts()
        .catch { unwrapFirestoreError(it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val rawChatsFlow = chatRepository.observeMyChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val rawGroupsFlow = groupRepository.observeMyGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val messageSearchMatches: StateFlow<MessageSearchMatches> = combine(
        _searchQuery.map { it.trim() }.distinctUntilChanged().debounce(220L),
        rawChatsFlow.map { list -> list.map { it.id } }.distinctUntilChanged(),
        rawGroupsFlow.map { list -> list.map { it.id } }.distinctUntilChanged()
    ) { query, chatIds, groupIds ->
        Triple(query, chatIds, groupIds)
    }.flatMapLatest { (query, chatIds, groupIds) ->
        flow {
            if (query.length < 2) {
                emit(MessageSearchMatches())
            } else {
                emit(MessageSearchMatches(isSearching = true))
                val chatMatches = chatRepository.searchMessagesInChats(chatIds, query)
                val groupMatches = groupRepository.searchMessagesInGroups(groupIds, query)
                emit(
                    MessageSearchMatches(
                        chatMatches = chatMatches,
                        groupMatches = groupMatches,
                        isSearching = false
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), MessageSearchMatches())

    val chatsState: StateFlow<UiState<List<ChatRoom>>> = combine(
        chatRepository.observeMyChats(),
        usersMap,
        savedContacts,
        _searchQuery,
        messageSearchMatches
    ) { chats, users, contacts, query, deepMatches ->
        val q = query.trim().lowercase()
        val contactsById = contacts.associateBy { it.contactUid }
        val filtered = if (q.isEmpty()) chats else chats.filter { room ->
            val peerUid = room.peerId(currentUserId)
            val peer = users[peerUid]
            val saved = contactsById[peerUid]
            (peer?.displayName?.lowercase()?.contains(q) == true) ||
                (saved?.displayName?.lowercase()?.contains(q) == true) ||
                (saved?.phoneNumber?.lowercase()?.contains(q) == true) ||
                room.lastMessage.lowercase().contains(q) ||
                deepMatches.chatMatches.containsKey(room.id)
        }
        UiState.Success(filtered) as UiState<List<ChatRoom>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load chats."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val groupsState: StateFlow<UiState<List<GroupChat>>> = combine(
        groupRepository.observeMyGroups(),
        _searchQuery,
        messageSearchMatches
    ) { groups, query, deepMatches ->
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) groups else groups.filter {
            it.name.lowercase().contains(q) ||
                it.lastMessage.lowercase().contains(q) ||
                (it.lastMessageSenderName?.lowercase()?.contains(q) == true) ||
                deepMatches.groupMatches.containsKey(it.id)
        }
        UiState.Success(filtered) as UiState<List<GroupChat>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load groups."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val callHistoryState: StateFlow<UiState<List<CallSession>>> = combine(
        callRepository.observeCallHistory(),
        usersMap,
        _searchQuery
    ) { calls, users, query ->
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) calls else calls.filter { call ->
            val isOutgoing = call.callerId == currentUserId
            val peerId = if (isOutgoing) call.receiverId else call.callerId
            val name = users[peerId]?.displayName
                ?: (if (isOutgoing) call.receiverName else call.callerName)
                ?: ""
            name.lowercase().contains(q)
        }
        UiState.Success(filtered) as UiState<List<CallSession>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load call history."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val incomingCall: StateFlow<CallSession?> = callRepository.observeCallHistory()
        .map { calls ->
            calls.firstOrNull {
                it.receiverId == currentUserId && it.statusEnum == CallStatus.RINGING
            }
        }
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearchActive.value = false
    }

    fun startDirectChat(peerUid: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            chatRepository.getOrCreateChat(peerUid).onSuccess { chatId ->
                onChatReady(chatId)
            }
        }
    }

    fun togglePin(chatId: String, currentlyPinned: Boolean) {
        viewModelScope.launch {
            chatRepository.togglePinChat(chatId, !currentlyPinned)
        }
    }

    fun toggleMute(chatId: String, currentlyMuted: Boolean) {
        viewModelScope.launch {
            chatRepository.toggleMuteChat(chatId, !currentlyMuted)
        }
    }
}
