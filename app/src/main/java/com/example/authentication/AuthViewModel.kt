package com.example.authentication

import android.app.Activity
import android.net.Uri
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesDataStore
import com.example.data.remote.MediaUploadProgress
import com.example.data.remote.StorageMediaRepository
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.PhoneOtpEvent
import com.example.security.InputValidator
import com.example.utils.CountryCode
import com.example.utils.CountryCodes
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep {
    WELCOME,
    PHONE_INPUT,
    OTP_VERIFY,
    CREATE_PROFILE
}

data class AuthUiState(
    val step: AuthStep = AuthStep.WELCOME,
    val selectedCountry: CountryCode = CountryCodes.all.first(),
    val phoneNumberInput: String = "",
    val verificationId: String? = null,
    val resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    val otpCode: String = "",
    val resendCountdownSec: Int = 0,
    val displayName: String = "",
    val about: String = "Available on Manoj & Sonali",
    val avatarLocalUri: Uri? = null,
    val uploadedAvatarUrl: String? = null,
    val avatarUploadProgress: Float? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthAndUserRepository,
    private val storageRepository: StorageMediaRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var otpRequestTimeoutJob: Job? = null

    fun prefillFromCurrentUser() {
        val user = authRepository.currentUser() ?: return
        _uiState.update { state ->
            state.copy(
                displayName = state.displayName.ifBlank { user.displayName.orEmpty() },
                uploadedAvatarUrl = state.uploadedAvatarUrl ?: user.photoUrl?.toString()
            )
        }
    }

    fun navigateTo(step: AuthStep) {
        otpRequestTimeoutJob?.cancel()
        _uiState.update { it.copy(step = step, isLoading = false, errorMessage = null, infoMessage = null) }
    }

    fun selectCountry(country: CountryCode) {
        _uiState.update { it.copy(selectedCountry = country, errorMessage = null) }
    }

    fun onPhoneNumberChanged(value: String) {
        _uiState.update { it.copy(phoneNumberInput = value.filter { ch -> ch.isDigit() }, errorMessage = null) }
    }

    fun onOtpChanged(value: String) {
        _uiState.update { it.copy(otpCode = value.filter { ch -> ch.isDigit() }.take(6), errorMessage = null) }
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.update { it.copy(displayName = value.take(InputValidator.MAX_DISPLAY_NAME), errorMessage = null) }
    }

    fun onAboutChanged(value: String) {
        _uiState.update { it.copy(about = value.take(InputValidator.MAX_ABOUT), errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signInWithGoogle(activity: Activity, onSignedInAndReady: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithGoogleInteractive(activity)
            result.fold(
                onSuccess = { user ->
                    val existing = authRepository.getUserProfileOnce(user.uid)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            displayName = user.displayName.orEmpty(),
                            uploadedAvatarUrl = user.photoUrl?.toString()
                        )
                    }
                    if (existing != null && existing.displayName.isNotBlank()) {
                        authRepository.updatePresence(true)
                        onSignedInAndReady()
                    } else {
                        _uiState.update { it.copy(step = AuthStep.CREATE_PROFILE) }
                    }
                },
                onFailure = { err ->
                    val msg = if (err is GetCredentialCancellationException) {
                        null
                    } else {
                        err.localizedMessage ?: "Google Sign-In failed. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            )
        }
    }

    fun sendOtp(activity: Activity, isResend: Boolean = false) {
        val state = _uiState.value
        val error = InputValidator.validatePhoneNumber(state.selectedCountry.dialCode, state.phoneNumberInput)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        val e164 = "${state.selectedCountry.dialCode}${state.phoneNumberInput}"
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        otpRequestTimeoutJob?.cancel()
        otpRequestTimeoutJob = viewModelScope.launch {
            delay(35_000L)
            if (_uiState.value.isLoading && _uiState.value.step == AuthStep.PHONE_INPUT) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Verification timed out waiting for Play Integrity / reCAPTCHA. Try again or sign in with Google below."
                    )
                }
            }
        }

        authRepository.startPhoneNumberVerification(
            activity = activity,
            phoneNumberE164 = e164,
            resendToken = if (isResend) state.resendToken else null
        ) { event ->
            otpRequestTimeoutJob?.cancel()
            when (event) {
                is PhoneOtpEvent.CodeSent -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = AuthStep.OTP_VERIFY,
                            verificationId = event.verificationId,
                            resendToken = event.resendToken,
                            infoMessage = "Verification code sent to $e164"
                        )
                    }
                    startResendCountdown()
                }
                is PhoneOtpEvent.AutoVerified -> {
                    viewModelScope.launch {
                        val res = authRepository.completePhoneCredential(event.credential)
                        res.fold(
                            onSuccess = { user ->
                                preferencesDataStore.saveEncryptedPhone(e164)
                                val existing = authRepository.getUserProfileOnce(user.uid)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        step = if (existing != null) it.step else AuthStep.CREATE_PROFILE
                                    )
                                }
                            },
                            onFailure = { err ->
                                _uiState.update {
                                    it.copy(isLoading = false, errorMessage = err.localizedMessage)
                                }
                            }
                        )
                    }
                }
                is PhoneOtpEvent.Failed -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = event.message)
                    }
                }
            }
        }
    }

    fun verifyOtp(onProfileExists: () -> Unit) {
        val state = _uiState.value
        val verificationId = state.verificationId
        if (verificationId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Request an OTP code first.") }
            return
        }
        if (state.otpCode.length != 6) {
            _uiState.update { it.copy(errorMessage = "Enter the 6-digit verification code.") }
            return
        }
        val e164 = "${state.selectedCountry.dialCode}${state.phoneNumberInput}"
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val credential = PhoneAuthProvider.getCredential(verificationId, state.otpCode)
            val result = authRepository.completePhoneCredential(credential)
            result.fold(
                onSuccess = { user ->
                    preferencesDataStore.saveEncryptedPhone(e164)
                    val existing = authRepository.getUserProfileOnce(user.uid)
                    _uiState.update { it.copy(isLoading = false) }
                    if (existing != null && existing.displayName.isNotBlank()) {
                        authRepository.updatePresence(true)
                        onProfileExists()
                    } else {
                        _uiState.update { it.copy(step = AuthStep.CREATE_PROFILE) }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Invalid verification code."
                        )
                    }
                }
            )
        }
    }

    fun uploadAvatar(uri: Uri) {
        _uiState.update { it.copy(avatarLocalUri = uri, avatarUploadProgress = 0f, errorMessage = null) }
        viewModelScope.launch {
            storageRepository.uploadUri(folder = "avatars", uri = uri, isAvatar = true).collect { progress ->
                when (progress) {
                    is MediaUploadProgress.Uploading -> {
                        _uiState.update { it.copy(avatarUploadProgress = progress.fraction) }
                    }
                    is MediaUploadProgress.Completed -> {
                        _uiState.update {
                            it.copy(
                                uploadedAvatarUrl = progress.downloadUrl,
                                avatarUploadProgress = null,
                                infoMessage = "Profile photo uploaded"
                            )
                        }
                    }
                    is MediaUploadProgress.Failed -> {
                        _uiState.update {
                            it.copy(
                                avatarUploadProgress = null,
                                errorMessage = progress.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun saveProfile(onComplete: () -> Unit) {
        val state = _uiState.value
        val validationError = InputValidator.validateDisplayName(state.displayName)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }
        val phoneE164 = state.phoneNumberInput.takeIf { it.isNotBlank() }?.let {
            "${state.selectedCountry.dialCode}$it"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.createOrUpdateProfile(
                displayName = state.displayName,
                about = state.about,
                photoUrl = state.uploadedAvatarUrl,
                phoneNumber = phoneE164,
                countryCode = state.selectedCountry.dialCode,
                discoverable = true
            )
            result.fold(
                onSuccess = {
                    if (!phoneE164.isNullOrBlank()) {
                        preferencesDataStore.saveEncryptedPhone(phoneE164)
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    onComplete()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Could not save profile."
                        )
                    }
                }
            )
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (sec in 60 downTo 1) {
                _uiState.update { it.copy(resendCountdownSec = sec) }
                delay(1000L)
            }
            _uiState.update { it.copy(resendCountdownSec = 0) }
        }
    }
}
