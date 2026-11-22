package com.example.contacts

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.ChatRepository
import com.example.domain.model.ContactAddedVia
import com.example.domain.model.SavedContact
import com.example.domain.model.UserProfile
import com.example.security.InputValidator
import com.example.security.unwrapFirestoreError
import com.example.utils.CountryCode
import com.example.utils.CountryCodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AddContactTab {
    PHONE, QR_SCAN
}

data class AddContactUiState(
    val activeTab: AddContactTab = AddContactTab.PHONE,
    val selectedCountry: CountryCode = CountryCodes.all.first(),
    val phoneInput: String = "",
    val isSearching: Boolean = false,
    val foundUser: UserProfile? = null,
    val foundPhoneDisplay: String? = null,
    val foundVia: ContactAddedVia = ContactAddedVia.PHONE,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class ContactsViewModel(
    val currentUserId: String,
    private val authRepository: AuthAndUserRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()

    val currentUserProfile: StateFlow<UserProfile?> = authRepository.observeUserProfile(currentUserId)
        .catch { unwrapFirestoreError(it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val savedContacts: StateFlow<List<SavedContact>> = authRepository.observeSavedContacts()
        .catch { unwrapFirestoreError(it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private var lastAnalyzedQr: String? = null

    fun setInitialTab(tab: AddContactTab) {
        _uiState.update { it.copy(activeTab = tab, errorMessage = null, infoMessage = null) }
    }

    fun selectTab(tab: AddContactTab) {
        lastAnalyzedQr = null
        _uiState.update {
            it.copy(
                activeTab = tab,
                errorMessage = null,
                infoMessage = null,
                foundUser = null
            )
        }
    }

    fun selectCountry(country: CountryCode) {
        _uiState.update { it.copy(selectedCountry = country, errorMessage = null) }
    }

    fun onPhoneInputChanged(raw: String) {
        val cleaned = raw.filter { it.isDigit() || it == '+' }.take(16)
        _uiState.update {
            it.copy(
                phoneInput = cleaned,
                errorMessage = null,
                infoMessage = null,
                foundUser = null
            )
        }
    }

    fun lookupByPhone() {
        val state = _uiState.value
        val digits = state.phoneInput.filter { it.isDigit() }
        if (digits.length < 6) {
            _uiState.update { it.copy(errorMessage = "Enter a valid phone number (at least 6 digits).") }
            return
        }
        val formattedDisplay = if (state.phoneInput.trim().startsWith("+")) {
            state.phoneInput.trim()
        } else {
            "${state.selectedCountry.dialCode}$digits"
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSearching = true, errorMessage = null, infoMessage = null, foundUser = null)
            }
            authRepository.findUserByPhone(state.selectedCountry.dialCode, state.phoneInput).fold(
                onSuccess = { profile ->
                    if (profile == null) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                errorMessage = "No Manoj & Sonali user found with $formattedDisplay. Ask them to add their phone number in Profile or share their QR code."
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                foundUser = profile,
                                foundPhoneDisplay = formattedDisplay,
                                foundVia = ContactAddedVia.PHONE
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = err.localizedMessage ?: "Failed to look up phone number."
                        )
                    }
                }
            )
        }
    }

    fun onQrDetected(rawPayload: String) {
        val trimmed = rawPayload.trim()
        if (trimmed.isEmpty() || trimmed == lastAnalyzedQr || _uiState.value.isSearching) return
        if (_uiState.value.foundUser != null) return
        lastAnalyzedQr = trimmed

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null, infoMessage = null) }
            authRepository.resolveScannedContact(trimmed).fold(
                onSuccess = { profile ->
                    if (profile == null) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                errorMessage = "Contact profile not found for this QR code."
                            )
                        }
                    } else {
                        val parsed = InputValidator.parseContactQrPayload(trimmed)
                        val phoneDisplay = (parsed as? InputValidator.ScannedContactTarget.ByPhone)?.rawPhone
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                foundUser = profile,
                                foundPhoneDisplay = phoneDisplay,
                                foundVia = ContactAddedVia.QR
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = err.localizedMessage ?: "Invalid contact QR code."
                        )
                    }
                }
            )
        }
    }

    fun scanFromGalleryImage(context: Context, uri: Uri) {
        _uiState.update { it.copy(isSearching = true, errorMessage = null, infoMessage = null, foundUser = null) }
        lastAnalyzedQr = null
        QrCodeUtils.scanQrFromImageUri(
            context = context,
            uri = uri,
            onSuccess = { raw ->
                _uiState.update { it.copy(isSearching = false) }
                if (raw.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(errorMessage = "No QR code detected in the selected image.")
                    }
                } else {
                    onQrDetected(raw)
                }
            },
            onError = { err ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = err.localizedMessage ?: "Could not scan image."
                    )
                }
            }
        )
    }

    fun saveFoundContact(openChatAfterSave: Boolean, onChatReady: (String) -> Unit) {
        val state = _uiState.value
        val contact = state.foundUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            authRepository.saveContact(
                contact = contact,
                phoneNumber = state.foundPhoneDisplay,
                via = state.foundVia
            ).fold(
                onSuccess = {
                    if (openChatAfterSave) {
                        chatRepository.getOrCreateChat(contact.uid).fold(
                            onSuccess = { chatId ->
                                _uiState.update { it.copy(isSaving = false, foundUser = null) }
                                lastAnalyzedQr = null
                                onChatReady(chatId)
                            },
                            onFailure = { err ->
                                _uiState.update {
                                    it.copy(
                                        isSaving = false,
                                        errorMessage = err.localizedMessage ?: "Saved contact, but failed to open chat."
                                    )
                                }
                            }
                        )
                    } else {
                        lastAnalyzedQr = null
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                foundUser = null,
                                phoneInput = "",
                                infoMessage = "${contact.displayName} added to your contacts."
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = err.localizedMessage ?: "Could not save contact."
                        )
                    }
                }
            )
        }
    }

    fun dismissFoundContact() {
        lastAnalyzedQr = null
        _uiState.update { it.copy(foundUser = null, errorMessage = null) }
    }

    fun openChatWithSavedContact(contactUid: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            chatRepository.getOrCreateChat(contactUid).onSuccess(onChatReady)
        }
    }

    fun removeSavedContact(contactUid: String) {
        viewModelScope.launch {
            authRepository.removeContact(contactUid)
        }
    }
}
