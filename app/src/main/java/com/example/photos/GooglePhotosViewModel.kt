package com.example.photos

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GooglePhotosUiState(
    val isSignedIn: Boolean = false,
    val accountEmail: String? = null,
    val accountName: String? = null,
    val accountPhotoUrl: String? = null,
    val accessToken: String? = null,
    val isAuthorizing: Boolean = false,
    val isLoadingPhotos: Boolean = false,
    val activeSessionId: String? = null,
    val awaitingPickerReturn: Boolean = false,
    val items: List<GooglePhotoItem> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

sealed interface GooglePhotosEvent {
    data class LaunchConsent(val pendingIntent: PendingIntent) : GooglePhotosEvent
    data class OpenPickerUri(val uri: String) : GooglePhotosEvent
}

class GooglePhotosViewModel(
    private val photosRepository: GooglePhotosRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(GooglePhotosUiState())
    val uiState: StateFlow<GooglePhotosUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GooglePhotosEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<GooglePhotosEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            preferencesDataStore.googlePhotosAccountFlow.collectLatest { saved ->
                _uiState.update { state ->
                    state.copy(
                        isSignedIn = saved.isConnected,
                        accountEmail = saved.email,
                        accountName = saved.displayName,
                        accountPhotoUrl = saved.photoUrl
                    )
                }
            }
        }
    }

    fun signInToGooglePhotos(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthorizing = true, errorMessage = null, infoMessage = null) }
            when (val outcome = photosRepository.authorizeGooglePhotos(activity)) {
                is GooglePhotosAuthOutcome.Authorized -> {
                    onAccessTokenGranted(outcome.accessToken)
                }
                is GooglePhotosAuthOutcome.NeedsConsent -> {
                    _uiState.update { it.copy(isAuthorizing = false) }
                    _events.tryEmit(GooglePhotosEvent.LaunchConsent(outcome.pendingIntent))
                }
                is GooglePhotosAuthOutcome.Failed -> {
                    _uiState.update {
                        it.copy(
                            isAuthorizing = false,
                            errorMessage = outcome.message
                        )
                    }
                }
            }
        }
    }

    fun onConsentIntentResult(activity: Activity, resultOk: Boolean, data: Intent?) {
        if (!resultOk) {
            _uiState.update {
                it.copy(
                    isAuthorizing = false,
                    errorMessage = "Google Photos sign-in was cancelled."
                )
            }
            return
        }
        val tokenResult = photosRepository.extractTokenFromConsentResult(activity, data)
        tokenResult.fold(
            onSuccess = { token -> onAccessTokenGranted(token) },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        isAuthorizing = false,
                        errorMessage = err.localizedMessage ?: "Could not complete Google Photos sign-in."
                    )
                }
            }
        )
    }

    private fun onAccessTokenGranted(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(accessToken = token, isAuthorizing = true, errorMessage = null) }
            val userInfoResult = photosRepository.fetchUserInfo(token)
            val info = userInfoResult.getOrNull()
            preferencesDataStore.saveGooglePhotosAccount(
                email = info?.email ?: _uiState.value.accountEmail,
                displayName = info?.displayName ?: _uiState.value.accountName ?: "Google Account",
                photoUrl = info?.photoUrl ?: _uiState.value.accountPhotoUrl
            )
            _uiState.update {
                it.copy(
                    isSignedIn = true,
                    accessToken = token,
                    isAuthorizing = false,
                    infoMessage = "Connected to Google Photos"
                )
            }
        }
    }

    fun openGooglePhotosPicker(activity: Activity) {
        viewModelScope.launch {
            val currentToken = _uiState.value.accessToken
            if (currentToken.isNullOrBlank()) {
                _uiState.update { it.copy(isLoadingPhotos = true, errorMessage = null, infoMessage = null) }
                when (val outcome = photosRepository.authorizeGooglePhotos(activity)) {
                    is GooglePhotosAuthOutcome.Authorized -> {
                        onAccessTokenGrantedAndOpenPicker(outcome.accessToken)
                    }
                    is GooglePhotosAuthOutcome.NeedsConsent -> {
                        _uiState.update { it.copy(isLoadingPhotos = false) }
                        _events.tryEmit(GooglePhotosEvent.LaunchConsent(outcome.pendingIntent))
                    }
                    is GooglePhotosAuthOutcome.Failed -> {
                        _uiState.update { it.copy(isLoadingPhotos = false, errorMessage = outcome.message) }
                    }
                }
            } else {
                onAccessTokenGrantedAndOpenPicker(currentToken)
            }
        }
    }

    private suspend fun onAccessTokenGrantedAndOpenPicker(token: String) {
        _uiState.update { it.copy(isLoadingPhotos = true, accessToken = token, errorMessage = null) }
        photosRepository.createPickerSession(token).fold(
            onSuccess = { session ->
                _uiState.update {
                    it.copy(
                        isLoadingPhotos = false,
                        activeSessionId = session.id,
                        awaitingPickerReturn = true,
                        infoMessage = "Select photos in Google Photos, then return here."
                    )
                }
                _events.tryEmit(GooglePhotosEvent.OpenPickerUri("${session.pickerUri}/autoclose"))
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        isLoadingPhotos = false,
                        errorMessage = err.localizedMessage ?: "Could not start Google Photos session."
                    )
                }
            }
        )
    }

    fun refreshActiveSessionIfNeeded() {
        val state = _uiState.value
        val token = state.accessToken ?: return
        val sessionId = state.activeSessionId ?: return
        if (!state.awaitingPickerReturn) return
        refreshPickedItems(token, sessionId)
    }

    fun refreshManually() {
        val state = _uiState.value
        val token = state.accessToken
        val sessionId = state.activeSessionId
        if (token.isNullOrBlank() || sessionId.isNullOrBlank()) return
        refreshPickedItems(token, sessionId)
    }

    private fun refreshPickedItems(token: String, sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPhotos = true, errorMessage = null) }
            val sessionRes = photosRepository.getPickerSession(token, sessionId)
            sessionRes.fold(
                onSuccess = { session ->
                    if (!session.mediaItemsSet) {
                        _uiState.update {
                            it.copy(
                                isLoadingPhotos = false,
                                awaitingPickerReturn = false,
                                infoMessage = "No new photos were selected yet. Tap Select Photos to choose from Google Photos."
                            )
                        }
                        return@fold
                    }
                    photosRepository.listPickedMediaItems(token, sessionId).fold(
                        onSuccess = { items ->
                            _uiState.update {
                                it.copy(
                                    isLoadingPhotos = false,
                                    awaitingPickerReturn = false,
                                    items = items,
                                    infoMessage = if (items.isNotEmpty()) {
                                        "Loaded ${items.size} photo${if (items.size == 1) "" else "s"} from Google Photos"
                                    } else {
                                        "No photos selected in this session."
                                    }
                                )
                            }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isLoadingPhotos = false,
                                    awaitingPickerReturn = false,
                                    errorMessage = err.localizedMessage ?: "Failed to load selected photos."
                                )
                            }
                        }
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingPhotos = false,
                            awaitingPickerReturn = false,
                            errorMessage = err.localizedMessage ?: "Could not check Google Photos session."
                        )
                    }
                }
            )
        }
    }

    fun signOutGooglePhotos() {
        viewModelScope.launch {
            preferencesDataStore.clearGooglePhotosAccount()
            _uiState.value = GooglePhotosUiState(infoMessage = "Signed out of Google Photos.")
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
