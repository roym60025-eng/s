package com.example.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.CallRepository
import com.example.domain.model.CallSession
import com.example.domain.model.CallStatus
import com.example.domain.model.CallType
import com.example.domain.model.UserProfile
import com.example.security.unwrapFirestoreError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer

class CallsViewModel(
    val currentUserId: String,
    private val authRepository: AuthAndUserRepository,
    private val callRepository: CallRepository,
    private val webRtcCallManager: WebRtcCallManager
) : ViewModel() {

    val isMuted: StateFlow<Boolean> = webRtcCallManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = webRtcCallManager.isSpeakerOn
    val connectionState: StateFlow<String> = webRtcCallManager.connectionState

    fun observeCall(callId: String): StateFlow<CallSession?> =
        callRepository.observeCall(callId)
            .catch { unwrapFirestoreError(it); emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    fun startOutgoingCall(
        receiver: UserProfile,
        callType: CallType,
        onCallCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val me = authRepository.getUserProfileOnce(currentUserId)
            callRepository.initiateCall(
                callerName = me?.displayName ?: "Manoj & Sonali User",
                callerPhotoUrl = me?.photoUrl,
                receiverId = receiver.uid,
                receiverName = receiver.displayName,
                receiverPhotoUrl = receiver.photoUrl,
                callType = callType
            ).onSuccess { callId ->
                onCallCreated(callId)
            }
        }
    }

    fun attachRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        webRtcCallManager.attachRenderers(local, remote)
    }

    fun startWebRtcIfReady(
        session: CallSession,
        hasAudioPermission: Boolean,
        hasCameraPermission: Boolean
    ) {
        val isCaller = session.callerId == currentUserId
        val isVideo = session.typeEnum == CallType.VIDEO
        if (isCaller) {
            webRtcCallManager.startSession(session.id, true, isVideo, hasAudioPermission, hasCameraPermission)
            session.answerSdp?.let { webRtcCallManager.handleRemoteAnswer(it) }
            webRtcCallManager.addRemoteIceCandidates(session.receiverCandidates)
        } else if (session.statusEnum == CallStatus.ACCEPTED) {
            webRtcCallManager.startSession(session.id, false, isVideo, hasAudioPermission, hasCameraPermission)
            session.offerSdp?.let { webRtcCallManager.handleRemoteOfferAndCreateAnswer(session.id, it) }
            webRtcCallManager.addRemoteIceCandidates(session.callerCandidates)
        }
    }

    fun acceptIncomingCall(
        session: CallSession,
        hasAudioPermission: Boolean,
        hasCameraPermission: Boolean
    ) {
        val isVideo = session.typeEnum == CallType.VIDEO
        webRtcCallManager.startSession(session.id, false, isVideo, hasAudioPermission, hasCameraPermission)
        val offer = session.offerSdp
        if (!offer.isNullOrBlank()) {
            webRtcCallManager.handleRemoteOfferAndCreateAnswer(session.id, offer)
        } else {
            viewModelScope.launch {
                callRepository.updateCallStatus(session.id, CallStatus.ACCEPTED)
            }
        }
    }

    fun rejectCall(callId: String) {
        webRtcCallManager.endSession()
        viewModelScope.launch {
            callRepository.updateCallStatus(callId, CallStatus.REJECTED)
        }
    }

    fun endCall(callId: String) {
        webRtcCallManager.endSession()
        viewModelScope.launch {
            callRepository.updateCallStatus(callId, CallStatus.ENDED)
        }
    }

    fun toggleMute() = webRtcCallManager.toggleMute()
    fun toggleSpeaker() = webRtcCallManager.toggleSpeaker()
    fun switchCamera() = webRtcCallManager.switchCamera()
}
