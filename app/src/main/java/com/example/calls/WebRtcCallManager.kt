package com.example.calls

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.data.repository.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class WebRtcCallManager(
    private val appContext: Context,
    private val callRepository: CallRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val eglBase: EglBase by lazy { EglBase.create() }
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var remoteVideoTrack: VideoTrack? = null

    private val appliedRemoteCandidates = mutableSetOf<String>()
    private var sentLocalCandidatesCount = 0
    @Volatile
    private var isCallerRole: Boolean = false
    @Volatile
    private var activeCallId: String? = null
    @Volatile
    private var isSessionEnded: Boolean = false

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _connectionState = MutableStateFlow("Connecting...")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private fun ensureFactory() {
        if (factory != null) return
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun attachRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        localRenderer = local
        remoteRenderer = remote
        local?.let { renderer ->
            try {
                renderer.init(eglBase.eglBaseContext, null)
                renderer.setMirror(true)
                localVideoTrack?.addSink(renderer)
            } catch (_: Exception) {
            }
        }
        remote?.let { renderer ->
            try {
                renderer.init(eglBase.eglBaseContext, null)
                remoteVideoTrack?.addSink(renderer)
            } catch (_: Exception) {
            }
        }
    }

    fun startSession(
        callId: String,
        isCaller: Boolean,
        isVideo: Boolean,
        hasAudioPermission: Boolean,
        hasCameraPermission: Boolean
    ) {
        if (activeCallId == callId && peerConnection != null) return
        activeCallId = callId
        isSessionEnded = false
        isCallerRole = isCaller
        sentLocalCandidatesCount = 0
        appliedRemoteCandidates.clear()
        _connectionState.value = if (isCaller) "Calling..." else "Connecting..."

        try {
            ensureFactory()
            val rtcConfig = PeerConnection.RTCConfiguration(
                listOf(
                    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                    PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
                )
            ).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            }

            peerConnection = factory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    if (isSessionEnded || activeCallId != callId) return
                    if (sentLocalCandidatesCount >= 180) return
                    val cand = candidate ?: return
                    sentLocalCandidatesCount++
                    val serialized = JSONObject().apply {
                        put("sdpMid", cand.sdpMid)
                        put("sdpMLineIndex", cand.sdpMLineIndex)
                        put("sdp", cand.sdp)
                    }.toString()
                    scope.launch {
                        if (!isSessionEnded && activeCallId == callId) {
                            callRepository.addIceCandidate(callId, isCallerRole, serialized)
                        }
                    }
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    _connectionState.value = when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> "Connected"
                        PeerConnection.IceConnectionState.DISCONNECTED -> "Reconnecting..."
                        PeerConnection.IceConnectionState.FAILED -> "Connection failed"
                        PeerConnection.IceConnectionState.CLOSED -> "Ended"
                        else -> "Connecting..."
                    }
                }

                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        remoteVideoTrack = track
                        remoteRenderer?.let { track.addSink(it) }
                    }
                }

                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onAddStream(p0: MediaStream?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
            })

            if (hasAudioPermission) {
                localAudioSource = factory?.createAudioSource(MediaConstraints())
                localAudioTrack = factory?.createAudioTrack("ARDAMSa0", localAudioSource)
                localAudioTrack?.setEnabled(true)
                localAudioTrack?.let { peerConnection?.addTrack(it, listOf("ARDAMS")) }
            }

            if (isVideo && hasCameraPermission) {
                val capturer = createCameraCapturer()
                if (capturer != null) {
                    videoCapturer = capturer
                    surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
                    localVideoSource = factory?.createVideoSource(capturer.isScreencast)
                    capturer.initialize(surfaceTextureHelper, appContext, localVideoSource?.capturerObserver)
                    capturer.startCapture(720, 1280, 24)
                    localVideoTrack = factory?.createVideoTrack("ARDAMSv0", localVideoSource)
                    localVideoTrack?.setEnabled(true)
                    localVideoTrack?.let { track ->
                        peerConnection?.addTrack(track, listOf("ARDAMS"))
                        localRenderer?.let { track.addSink(it) }
                    }
                }
            }

            setSpeakerphone(isVideo)

            if (isCaller) {
                createAndPublishOffer(callId)
            }
        } catch (e: Exception) {
            Log.e("WebRTC", "Error starting WebRTC session", e)
            _connectionState.value = "Signaling active"
        }
    }

    private fun createAndPublishOffer(callId: String) {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                if (isSessionEnded || activeCallId != callId) return
                val desc = p0 ?: return
                pc.setLocalDescription(SimpleSdpObserver(), desc)
                scope.launch {
                    if (!isSessionEnded && activeCallId == callId) {
                        callRepository.publishOfferSdp(callId, desc.description)
                    }
                }
            }
        }, constraints)
    }

    fun handleRemoteOfferAndCreateAnswer(callId: String, offerSdp: String) {
        if (isSessionEnded || activeCallId != callId) return
        val pc = peerConnection ?: return
        if (pc.remoteDescription != null) return
        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                if (isSessionEnded || activeCallId != callId) return
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
                pc.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        if (isSessionEnded || activeCallId != callId) return
                        val ans = p0 ?: return
                        pc.setLocalDescription(SimpleSdpObserver(), ans)
                        scope.launch {
                            if (!isSessionEnded && activeCallId == callId) {
                                callRepository.acceptCallWithAnswer(callId, ans.description)
                            }
                        }
                    }
                }, constraints)
            }
        }, remoteDesc)
    }

    fun handleRemoteAnswer(answerSdp: String) {
        val pc = peerConnection ?: return
        if (pc.remoteDescription != null) return
        val remoteDesc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        pc.setRemoteDescription(SimpleSdpObserver(), remoteDesc)
    }

    fun addRemoteIceCandidates(candidates: List<String>) {
        val pc = peerConnection ?: return
        candidates.forEach { raw ->
            if (appliedRemoteCandidates.add(raw)) {
                try {
                    val json = JSONObject(raw)
                    val candidate = IceCandidate(
                        json.getString("sdpMid"),
                        json.getInt("sdpMLineIndex"),
                        json.getString("sdp")
                    )
                    pc.addIceCandidate(candidate)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        localAudioTrack?.setEnabled(!next)
    }

    fun toggleSpeaker() {
        setSpeakerphone(!_isSpeakerOn.value)
    }

    private fun setSpeakerphone(enabled: Boolean) {
        _isSpeakerOn.value = enabled
        try {
            val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            am?.isSpeakerphoneOn = enabled
        } catch (_: Exception) {
        }
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun endSession() {
        isSessionEnded = true
        activeCallId = null
        sentLocalCandidatesCount = 0
        appliedRemoteCandidates.clear()
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {
        }
        try {
            videoCapturer?.dispose()
        } catch (_: Exception) {
        }
        videoCapturer = null
        localVideoTrack?.let { t -> localRenderer?.let { t.removeSink(it) } }
        remoteVideoTrack?.let { t -> remoteRenderer?.let { t.removeSink(it) } }
        localRenderer = null
        remoteRenderer = null
        remoteVideoTrack = null
        localVideoTrack?.dispose()
        localVideoTrack = null
        localVideoSource?.dispose()
        localVideoSource = null
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        localAudioTrack?.dispose()
        localAudioTrack = null
        localAudioSource?.dispose()
        localAudioSource = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        _isMuted.value = false
        _connectionState.value = "Ended"
        try {
            val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {
        }
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(appContext)
        val deviceNames = enumerator.deviceNames
        deviceNames.firstOrNull { enumerator.isFrontFacing(it) }?.let { front ->
            enumerator.createCapturer(front, null)?.let { return it }
        }
        deviceNames.firstOrNull { !enumerator.isFrontFacing(it) }?.let { back ->
            enumerator.createCapturer(back, null)?.let { return it }
        }
        return null
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
