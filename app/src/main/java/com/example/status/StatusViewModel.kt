package com.example.status

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.MediaUploadProgress
import com.example.data.remote.StorageMediaRepository
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.StatusRepository
import com.example.domain.model.StatusPrivacy
import com.example.domain.model.StatusType
import com.example.domain.model.UserProfile
import com.example.domain.model.UserStatus
import com.example.security.unwrapFirestoreError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatusViewModel(
    val currentUserId: String,
    private val authRepository: AuthAndUserRepository,
    private val statusRepository: StatusRepository,
    private val storageRepository: StorageMediaRepository
) : ViewModel() {

    val allStatuses: StateFlow<List<UserStatus>> = statusRepository.observeActiveStatuses()
        .catch { unwrapFirestoreError(it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val usersMap: StateFlow<Map<String, UserProfile>> = authRepository.observeDiscoverableUsers()
        .map { list -> list.associateBy { it.uid } }
        .catch { unwrapFirestoreError(it); emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val currentUser: StateFlow<UserProfile?> = authRepository.observeUserProfile(currentUserId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun postTextStatus(text: String, bgHex: String, privacy: StatusPrivacy, onDone: () -> Unit) {
        val me = currentUser.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            statusRepository.postStatus(
                userName = me.displayName.ifBlank { "Member" },
                userPhotoUrl = me.photoUrl,
                type = StatusType.TEXT,
                caption = text,
                mediaUrl = null,
                backgroundColorHex = bgHex,
                privacy = privacy
            ).fold(
                onSuccess = { onDone() },
                onFailure = { _errorMessage.value = it.localizedMessage }
            )
        }
    }

    fun postMediaStatus(uri: Uri, type: StatusType, caption: String, privacy: StatusPrivacy, onDone: () -> Unit) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            storageRepository.uploadUri("status_media", uri).collect { prog ->
                when (prog) {
                    is MediaUploadProgress.Uploading -> _uploadProgress.value = prog.fraction
                    is MediaUploadProgress.Completed -> {
                        _uploadProgress.value = null
                        statusRepository.postStatus(
                            userName = me.displayName.ifBlank { "Member" },
                            userPhotoUrl = me.photoUrl,
                            type = type,
                            caption = caption,
                            mediaUrl = prog.downloadUrl,
                            backgroundColorHex = null,
                            privacy = privacy
                        ).fold(
                            onSuccess = { onDone() },
                            onFailure = { _errorMessage.value = it.localizedMessage }
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

    fun markViewed(status: UserStatus) {
        if (status.userId == currentUserId || currentUserId in status.viewerIds) return
        viewModelScope.launch {
            statusRepository.markStatusViewed(status.id)
        }
    }

    fun deleteStatus(statusId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            statusRepository.deleteStatus(statusId).onSuccess { onDone() }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
