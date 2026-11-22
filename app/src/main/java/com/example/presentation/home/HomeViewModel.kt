package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GroupRepository
import com.example.data.repository.StatusRepository
import com.example.domain.model.CallSession
import com.example.domain.model.CallStatus
import com.example.domain.model.ChatRoom
import com.example.domain.model.GroupChat
import com.example.domain.model.SavedContact
import com.example.domain.model.UiState
import com.example.domain.model.UserPrivateSettings
import com.example.domain.model.UserProfile
import com.example.domain.model.UserStatus
import com.example.security.unwrapFirestoreError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeTab {
    CHATS, GROUPS, CALLS, PROFILE
}

class HomeViewModel(
    val currentUserId: String,
    private val authRepository: AuthAndUserRepository,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val statusRepository: StatusRepository,
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

    val chatsState: StateFlow<UiState<List<ChatRoom>>> = combine(
        chatRepository.observeMyChats(),
        usersMap,
        _searchQuery
    ) { chats, users, query ->
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) chats else chats.filter { room ->
            val peer = users[room.peerId(currentUserId)]
            (peer?.displayName?.lowercase()?.contains(q) == true) ||
                room.lastMessage.lowercase().contains(q)
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
        _searchQuery
    ) { groups, query ->
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) groups else groups.filter {
            it.name.lowercase().contains(q) || it.lastMessage.lowercase().contains(q)
        }
        UiState.Success(filtered) as UiState<List<GroupChat>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load groups."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val statuses: StateFlow<List<UserStatus>> = statusRepository.observeActiveStatuses()
        .catch { unwrapFirestoreError(it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val callHistoryState: StateFlow<UiState<List<CallSession>>> = callRepository.observeCallHistory()
        .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
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
