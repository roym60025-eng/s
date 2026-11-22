package com.example.messaging

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.MediaUploadProgress
import com.example.data.remote.StorageMediaRepository
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.ChatRepository
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatRoom
import com.example.domain.model.MessageType
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.security.unwrapFirestoreError
import com.example.utils.VoicePlaybackState
import com.example.utils.VoiceRecorderPlayer
import com.example.utils.VoiceRecordingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModel(
    val chatId: String,
    val currentUserId: String,
    private val authRepository: AuthAndUserRepository,
    private val chatRepository: ChatRepository,
    private val storageRepository: StorageMediaRepository,
    private val voiceRecorderPlayer: VoiceRecorderPlayer
) : ViewModel() {

    private val pageSize = MutableStateFlow(40L)

    val chatRoom: StateFlow<ChatRoom?> = chatRepository.observeChatRoom(chatId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val currentUser: StateFlow<UserProfile?> = authRepository.observeUserProfile(currentUserId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val peerUser: StateFlow<UserProfile?> = chatRoom
        .flatMapLatest { room ->
            val peerId = room?.peerId(currentUserId).orEmpty()
            if (peerId.isBlank()) MutableStateFlow(null)
            else authRepository.observeUserProfile(peerId)
        }
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = pageSize
        .flatMapLatest { limit ->
            chatRepository.observeMessages(chatId, limit)
                .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load messages."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allUsers: StateFlow<List<UserProfile>> = authRepository.observeDiscoverableUsers()
        .catch { unwrapFirestoreError(it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val voiceRecordingState: StateFlow<VoiceRecordingState> = voiceRecorderPlayer.recordingState
    val voicePlaybackState: StateFlow<VoicePlaybackState> = voiceRecorderPlayer.playbackState

    private val _composerText = MutableStateFlow("")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    val replyingTo: StateFlow<ChatMessage?> = _replyingTo.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private var typingDebounceJob: Job? = null

    fun onComposerChanged(text: String) {
        _composerText.value = text
        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            chatRepository.setTyping(chatId, text.isNotBlank())
            if (text.isNotBlank()) {
                delay(2500L)
                chatRepository.setTyping(chatId, false)
            }
        }
    }

    fun startReply(message: ChatMessage) {
        _editingMessage.value = null
        _replyingTo.value = message
    }

    fun startEdit(message: ChatMessage) {
        _replyingTo.value = null
        _editingMessage.value = message
        _composerText.value = message.text
    }

    fun cancelReplyOrEdit() {
        _replyingTo.value = null
        if (_editingMessage.value != null) {
            _editingMessage.value = null
            _composerText.value = ""
        }
    }

    fun clearError() {
        _actionError.value = null
    }

    fun loadOlderMessages() {
        pageSize.value = (pageSize.value + 30L).coerceAtMost(250L)
    }

    fun markMessagesAsRead(messages: List<ChatMessage>) {
        viewModelScope.launch {
            chatRepository.markChatRead(chatId, messages)
        }
    }

    fun sendTextMessage() {
        val text = _composerText.value.trim()
        if (text.isEmpty()) return
        val editTarget = _editingMessage.value
        if (editTarget != null) {
            viewModelScope.launch {
                chatRepository.editMessage(chatId, editTarget.id, text).onFailure {
                    _actionError.value = it.localizedMessage ?: "Failed to edit message"
                }
                _editingMessage.value = null
                _composerText.value = ""
            }
            return
        }

        val room = chatRoom.value ?: return
        val senderName = currentUser.value?.displayName ?: "Me"
        val reply = _replyingTo.value
        _composerText.value = ""
        _replyingTo.value = null

        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                participantIds = room.participantIds,
                senderName = senderName,
                type = MessageType.TEXT,
                text = text,
                replyTo = reply
            ).onFailure {
                _actionError.value = it.localizedMessage ?: "Message failed to send."
            }
        }
    }

    fun sendMediaUri(uri: Uri, type: MessageType, caption: String = "") {
        val room = chatRoom.value ?: return
        val senderName = currentUser.value?.displayName ?: "Me"
        val reply = _replyingTo.value
        _replyingTo.value = null

        viewModelScope.launch {
            storageRepository.uploadUri(folder = "chat_media/$chatId", uri = uri).collect { progress ->
                when (progress) {
                    is MediaUploadProgress.Uploading -> _uploadProgress.value = progress.fraction
                    is MediaUploadProgress.Completed -> {
                        _uploadProgress.value = null
                        chatRepository.sendMessage(
                            chatId = chatId,
                            participantIds = room.participantIds,
                            senderName = senderName,
                            type = type,
                            text = caption,
                            mediaUrl = progress.downloadUrl,
                            mediaName = progress.fileName,
                            mediaSizeBytes = progress.sizeBytes,
                            replyTo = reply
                        ).onFailure {
                            _actionError.value = it.localizedMessage
                        }
                    }
                    is MediaUploadProgress.Failed -> {
                        _uploadProgress.value = null
                        _actionError.value = progress.message
                    }
                }
            }
        }
    }

    fun startVoiceRecording() {
        voiceRecorderPlayer.startRecording().onFailure {
            _actionError.value = it.localizedMessage ?: "Microphone unavailable."
        }
    }

    fun cancelVoiceRecording() {
        voiceRecorderPlayer.cancelRecording()
    }

    fun stopAndSendVoiceRecording() {
        val (file, durationMs) = voiceRecorderPlayer.stopRecording() ?: return
        sendVoiceFile(file, durationMs)
    }

    private fun sendVoiceFile(file: File, durationMs: Long) {
        val room = chatRoom.value ?: return
        val senderName = currentUser.value?.displayName ?: "Me"
        viewModelScope.launch {
            storageRepository.uploadFile("chat_media/$chatId", file, "audio/mp4").collect { progress ->
                when (progress) {
                    is MediaUploadProgress.Uploading -> _uploadProgress.value = progress.fraction
                    is MediaUploadProgress.Completed -> {
                        _uploadProgress.value = null
                        chatRepository.sendMessage(
                            chatId = chatId,
                            participantIds = room.participantIds,
                            senderName = senderName,
                            type = MessageType.VOICE,
                            text = "",
                            mediaUrl = progress.downloadUrl,
                            mediaName = progress.fileName,
                            mediaSizeBytes = progress.sizeBytes,
                            voiceDurationMs = durationMs
                        )
                        file.delete()
                    }
                    is MediaUploadProgress.Failed -> {
                        _uploadProgress.value = null
                        _actionError.value = progress.message
                    }
                }
            }
        }
    }

    fun toggleVoicePlayback(message: ChatMessage) {
        val url = message.mediaUrl ?: return
        voiceRecorderPlayer.togglePlay(message.id, url)
    }

    fun toggleReaction(message: ChatMessage, emoji: String) {
        viewModelScope.launch {
            chatRepository.toggleReaction(chatId, message, emoji)
        }
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            chatRepository.deleteMessageForEveryone(chatId, message.id).onFailure {
                _actionError.value = it.localizedMessage
            }
        }
    }

    fun forwardMessage(message: ChatMessage, targetPeer: UserProfile) {
        viewModelScope.launch {
            val targetChatId = chatRepository.getOrCreateChat(targetPeer.uid).getOrNull() ?: return@launch
            val senderName = currentUser.value?.displayName ?: "Me"
            chatRepository.sendMessage(
                chatId = targetChatId,
                participantIds = listOf(currentUserId, targetPeer.uid).sorted(),
                senderName = senderName,
                type = message.messageType,
                text = message.text,
                mediaUrl = message.mediaUrl,
                mediaName = message.mediaName,
                mediaSizeBytes = message.mediaSizeBytes,
                voiceDurationMs = message.voiceDurationMs,
                forwarded = true
            )
        }
    }

    fun downloadMediaToCache(message: ChatMessage, onResult: (String) -> Unit) {
        val url = message.mediaUrl ?: return
        viewModelScope.launch {
            storageRepository.downloadToCache(url, message.mediaName ?: "attachment").fold(
                onSuccess = { onResult("Saved to ${it.name}") },
                onFailure = { onResult(it.localizedMessage ?: "Download failed") }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorderPlayer.stopPlayback()
    }
}
