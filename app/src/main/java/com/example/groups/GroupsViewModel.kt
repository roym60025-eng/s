package com.example.groups

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.MediaUploadProgress
import com.example.data.remote.StorageMediaRepository
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.GroupRepository
import com.example.domain.model.GroupChat
import com.example.domain.model.GroupMessage
import com.example.domain.model.MessageType
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.security.unwrapFirestoreError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupsViewModel(
    val currentUserId: String,
    private val authRepository: AuthAndUserRepository,
    private val groupRepository: GroupRepository,
    private val storageRepository: StorageMediaRepository
) : ViewModel() {

    val allUsers: StateFlow<Map<String, UserProfile>> = authRepository.observeDiscoverableUsers()
        .map { list -> list.associateBy { it.uid } }
        .catch { unwrapFirestoreError(it); emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val currentUser: StateFlow<UserProfile?> = authRepository.observeUserProfile(currentUserId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _uploadedGroupPhotoUrl = MutableStateFlow<String?>(null)
    val uploadedGroupPhotoUrl: StateFlow<String?> = _uploadedGroupPhotoUrl.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun observeGroup(groupId: String): StateFlow<GroupChat?> = groupRepository.observeGroup(groupId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    fun observeGroupMessages(groupId: String): StateFlow<UiState<List<GroupMessage>>> =
        groupRepository.observeGroupMessages(groupId)
            .map<List<GroupMessage>, UiState<List<GroupMessage>>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                emit(UiState.Error(root.localizedMessage ?: "Failed to load group messages."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun uploadGroupAvatar(uri: Uri) {
        viewModelScope.launch {
            storageRepository.uploadUri("group_media/avatars", uri, isAvatar = true).collect { prog ->
                when (prog) {
                    is MediaUploadProgress.Uploading -> _uploadProgress.value = prog.fraction
                    is MediaUploadProgress.Completed -> {
                        _uploadProgress.value = null
                        _uploadedGroupPhotoUrl.value = prog.downloadUrl
                    }
                    is MediaUploadProgress.Failed -> {
                        _uploadProgress.value = null
                        _errorMessage.value = prog.message
                    }
                }
            }
        }
    }

    fun createGroup(
        name: String,
        description: String,
        selectedMemberIds: List<String>,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            groupRepository.createGroup(
                name = name,
                description = description,
                imageUrl = _uploadedGroupPhotoUrl.value,
                memberIds = selectedMemberIds
            ).fold(
                onSuccess = { groupId ->
                    _uploadedGroupPhotoUrl.value = null
                    onCreated(groupId)
                },
                onFailure = { err ->
                    _errorMessage.value = err.localizedMessage ?: "Failed to create group"
                }
            )
        }
    }

    fun sendGroupMessage(
        group: GroupChat,
        text: String,
        replyTo: GroupMessage? = null
    ) {
        val senderName = currentUser.value?.displayName ?: "Member"
        viewModelScope.launch {
            groupRepository.sendGroupMessage(
                groupId = group.id,
                memberIds = group.memberIds,
                senderName = senderName,
                type = MessageType.TEXT,
                text = text,
                replyTo = replyTo
            ).onFailure { _errorMessage.value = it.localizedMessage }
        }
    }

    fun sendGroupMedia(group: GroupChat, uri: Uri, type: MessageType) {
        val senderName = currentUser.value?.displayName ?: "Member"
        viewModelScope.launch {
            storageRepository.uploadUri("group_media/${group.id}", uri).collect { prog ->
                when (prog) {
                    is MediaUploadProgress.Uploading -> _uploadProgress.value = prog.fraction
                    is MediaUploadProgress.Completed -> {
                        _uploadProgress.value = null
                        groupRepository.sendGroupMessage(
                            groupId = group.id,
                            memberIds = group.memberIds,
                            senderName = senderName,
                            type = type,
                            text = "",
                            mediaUrl = prog.downloadUrl,
                            mediaName = prog.fileName,
                            mediaSizeBytes = prog.sizeBytes
                        )
                    }
                    is MediaUploadProgress.Failed -> {
                        _uploadProgress.value = null
                        _errorMessage.value = prog.message
                    }
                }
            }
        }
    }

    fun toggleReaction(groupId: String, message: GroupMessage, emoji: String) {
        viewModelScope.launch {
            groupRepository.toggleGroupReaction(groupId, message, emoji)
        }
    }

    fun updateGroupInfo(groupId: String, name: String, description: String) {
        viewModelScope.launch {
            groupRepository.updateGroupInfo(groupId, name, description).onFailure {
                _errorMessage.value = it.localizedMessage
            }
        }
    }

    fun addMember(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.addMember(groupId, userId).onFailure { _errorMessage.value = it.localizedMessage }
        }
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.removeMember(groupId, userId).onFailure { _errorMessage.value = it.localizedMessage }
        }
    }

    fun toggleAdmin(groupId: String, userId: String, makeAdmin: Boolean) {
        viewModelScope.launch {
            groupRepository.setAdmin(groupId, userId, makeAdmin).onFailure { _errorMessage.value = it.localizedMessage }
        }
    }

    fun leaveGroup(group: GroupChat, onLeft: () -> Unit) {
        viewModelScope.launch {
            groupRepository.leaveGroup(group.id, group.memberIds, group.adminIds).fold(
                onSuccess = { onLeft() },
                onFailure = { _errorMessage.value = it.localizedMessage }
            )
        }
    }

    fun deleteGroup(groupId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId).fold(
                onSuccess = { onDeleted() },
                onFailure = { _errorMessage.value = it.localizedMessage }
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
