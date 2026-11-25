package com.example.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesDataStore
import com.example.data.remote.MediaUploadProgress
import com.example.data.remote.StorageMediaRepository
import com.example.data.repository.AuthAndUserRepository
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPrivateSettings
import com.example.domain.model.UserProfile
import com.example.security.unwrapFirestoreError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileSettingsViewModel(
    val currentUserId: String,
    private val authRepository: AuthAndUserRepository,
    private val storageRepository: StorageMediaRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = authRepository.observeUserProfile(currentUserId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val privateSettings: StateFlow<UserPrivateSettings?> = authRepository.observePrivateSettings(currentUserId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allUsers: StateFlow<List<UserProfile>> = authRepository.observeDiscoverableUsers()
        .catch { unwrapFirestoreError(it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val themeMode: StateFlow<ThemeMode> = preferencesDataStore.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemeMode.SYSTEM)

    val appThemeStyle: StateFlow<AppThemeStyle> = preferencesDataStore.appThemeStyleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppThemeStyle.EMERALD)

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isErrorStatus = MutableStateFlow(false)
    val isErrorStatus: StateFlow<Boolean> = _isErrorStatus.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesDataStore.setThemeMode(mode)
            authRepository.updatePrivateSettings(mapOf("darkThemeMode" to mode.name))
        }
    }

    fun setAppThemeStyle(style: AppThemeStyle) {
        viewModelScope.launch {
            preferencesDataStore.setAppThemeStyle(style)
            authRepository.updatePrivateSettings(mapOf("appThemeStyle" to style.name))
        }
    }

    fun updateSettingToggle(field: String, value: Boolean) {
        viewModelScope.launch {
            authRepository.updatePrivateSettings(mapOf(field to value))
        }
    }

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            authRepository.toggleBlockUser(targetUserId, block = false)
        }
    }

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            authRepository.toggleBlockUser(targetUserId, block = true)
        }
    }

    fun uploadNewAvatar(uri: Uri) {
        viewModelScope.launch {
            _statusMessage.value = null
            _isErrorStatus.value = false
            _uploadProgress.value = 0.05f
            storageRepository.uploadUri("avatars", uri, isAvatar = true).collect { prog ->
                when (prog) {
                    is MediaUploadProgress.Uploading -> _uploadProgress.value = prog.fraction
                    is MediaUploadProgress.Completed -> {
                        val current = userProfile.value ?: authRepository.getUserProfileOnce(currentUserId)
                        val settings = privateSettings.value
                        val fallbackName = authRepository.currentUser()?.displayName?.takeIf { it.isNotBlank() } ?: "Member"
                        val saveResult = authRepository.createOrUpdateProfile(
                            displayName = current?.displayName?.takeIf { it.isNotBlank() } ?: fallbackName,
                            about = current?.about ?: "",
                            photoUrl = prog.downloadUrl,
                            phoneNumber = settings?.phoneNumber,
                            countryCode = settings?.countryCode ?: "+91",
                            discoverable = current?.discoverable ?: true
                        )
                        _uploadProgress.value = null
                        saveResult.fold(
                            onSuccess = {
                                _isErrorStatus.value = false
                                _statusMessage.value = "Profile photo updated"
                            },
                            onFailure = { err ->
                                _isErrorStatus.value = true
                                _statusMessage.value = err.localizedMessage ?: "Could not save profile photo."
                            }
                        )
                    }
                    is MediaUploadProgress.Failed -> {
                        _uploadProgress.value = null
                        _isErrorStatus.value = true
                        _statusMessage.value = prog.message
                    }
                }
            }
        }
    }

    fun saveProfileEdits(displayName: String, about: String, phone: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _statusMessage.value = null
            _isErrorStatus.value = false
            val current = userProfile.value ?: authRepository.getUserProfileOnce(currentUserId)
            val settings = privateSettings.value
            authRepository.createOrUpdateProfile(
                displayName = displayName,
                about = about,
                photoUrl = current?.photoUrl,
                phoneNumber = phone.ifBlank { settings?.phoneNumber },
                countryCode = settings?.countryCode ?: "+91",
                discoverable = current?.discoverable ?: true
            ).fold(
                onSuccess = {
                    _isErrorStatus.value = false
                    _statusMessage.value = "Profile saved"
                    onDone()
                },
                onFailure = {
                    _isErrorStatus.value = true
                    _statusMessage.value = it.localizedMessage ?: "Could not save profile changes."
                }
            )
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
        }
    }

    fun deleteAccount(context: Context) {
        viewModelScope.launch {
            preferencesDataStore.clearSensitivePrefs()
            authRepository.deleteAccount(context).onFailure {
                _isErrorStatus.value = true
                _statusMessage.value = it.localizedMessage ?: "Recent sign-in required to delete account."
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
        _isErrorStatus.value = false
    }
}
