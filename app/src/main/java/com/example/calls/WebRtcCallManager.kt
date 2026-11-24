package com.example.calls

import android.content.Context
import android.media.AudioManager
import android.os.Build
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
import org.webrtc.SoftwareVideoDecoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.Collections
import java.util.IdentityHashMap

class WebRtcCallManager(
    private val appContext: Context,
    private val callRepository: CallRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionLock = Any()
    private val rendererLock = Any()
    private val initializedRenderers: MutableSet<SurfaceViewRenderer> =
        Collections.newSetFromMap(IdentityHashMap())

    val eglBase: EglBase by lazy { EglBase.create(null, EglBase.CONFIG_PLAIN) }
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

    private fun isEmulatorDevice(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val hw = Build.HARDWARE.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val board = Build.BOARD.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        return fp.startsWith("generic") ||
            fp.startsWith("unknown") ||
            fp.contains("emulator") ||
            fp.contains("vsoc") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("cuttlefish") ||
            model.contains("android sdk built for") ||
            hw.contains("goldfish") ||
            hw.contains("ranchu") ||
            hw.contains("cutf") ||
            device.contains("vsoc") ||
            device.contains("emulator") ||
            product.contains("vsoc") ||
            product.contains("cf_") ||
            product.contains("sdk_gphone") ||
            board.contains("goldfish") ||
            board.contains("ranchu") ||
            manufacturer.contains("genymotion") ||
            (brand.startsWith("generic") && device.startsWith("generic"))
    }

    private fun ensureFactory() {
        if (factory != null) return
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        val useSoftwareCodecs = isEmulatorDevice()
        val encoderFactory = if (useSoftwareCodecs) {
            SoftwareVideoEncoderFactory()
        } else {
            DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        }
        val decoderFactory = if (useSoftwareCodecs) {
            SoftwareVideoDecoderFactory()
        } else {
            DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        }
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        synchronized(rendererLock) {
            if (localRenderer !== renderer) {
                localRenderer?.let { prev ->
                    runCatching { localVideoTrack?.removeSink(prev) }
                }
                localRenderer = renderer
            }
            if (initializedRenderers.add(renderer)) {
                runCatching {
                    renderer.init(eglBase.eglBaseContext, null)
                    renderer.setZOrderMediaOverlay(true)
                    renderer.setMirror(true)
                }
            }
            if (!isSessionEnded) {
                runCatching { localVideoTrack?.addSink(renderer) }
            }
        }
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        synchronized(rendererLock) {
            if (remoteRenderer !== renderer) {
                remoteRenderer?.let { prev ->
                    runCatching { remoteVideoTrack?.removeSink(prev) }
                }
                remoteRenderer = renderer
            }
            if (initializedRenderers.add(renderer)) {
                runCatching {
                    renderer.init(eglBase.eglBaseContext, null)
                }
            }
            if (!isSessionEnded) {
                runCatching { remoteVideoTrack?.addSink(renderer) }
            }
        }
    }

    fun detachLocalRenderer(renderer: SurfaceViewRenderer?) {
        val target = renderer ?: return
        synchronized(rendererLock) {
            runCatching { localVideoTrack?.removeSink(target) }
            if (localRenderer === target) {
                localRenderer = null
            }
            if (initializedRenderers.remove(target)) {
                runCatching { target.release() }
            }
        }
    }

    fun detachRemoteRenderer(renderer: SurfaceViewRenderer?) {
        val target = renderer ?: return
        synchronized(rendererLock) {
            runCatching { remoteVideoTrack?.removeSink(target) }
            if (remoteRenderer === target) {
                remoteRenderer = null
            }
            if (initializedRenderers.remove(target)) {
                runCatching { target.release() }
            }
        }
    }

    fun detachRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        detachLocalRenderer(local)
        detachRemoteRenderer(remote)
    }

    fun startSession(
        callId: String,
        isCaller: Boolean,
        isVideo: Boolean,
        hasAudioPermission: Boolean,
        hasCameraPermission: Boolean
    ) {
        synchronized(sessionLock) {
            if (activeCallId == callId && peerConnection != null && !isSessionEnded) return
            if (peerConnection != null || videoCapturer != null) {
                endSession()
            }
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
                            synchronized(rendererLock) {
                                if (!isSessionEnded) {
                                    remoteVideoTrack = track
                                    remoteRenderer?.takeIf { initializedRenderers.contains(it) }?.let {
                                        runCatching { track.addSink(it) }
                                    }
                                }
                            }
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
                            synchronized(rendererLock) {
                                localRenderer?.takeIf { initializedRenderers.contains(it) }?.let {
                                    runCatching { track.addSink(it) }
                                }
                            }
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
        synchronized(sessionLock) {
            isSessionEnded = true
            activeCallId = null
            sentLocalCandidatesCount = 0
            appliedRemoteCandidates.clear()

            synchronized(rendererLock) {
                localVideoTrack?.let { track ->
                    localRenderer?.let { runCatching { track.removeSink(it) } }
                }
                remoteVideoTrack?.let { track ->
                    remoteRenderer?.let { runCatching { track.removeSink(it) } }
                }
                remoteVideoTrack = null
            }

            runCatching { videoCapturer?.stopCapture() }
            runCatching { surfaceTextureHelper?.stopListening() }
            runCatching { videoCapturer?.dispose() }
            videoCapturer = null

            runCatching { peerConnection?.close() }
            runCatching { peerConnection?.dispose() }
            peerConnection = null

            runCatching { localVideoTrack?.dispose() }
            localVideoTrack = null
            runCatching { localVideoSource?.dispose() }
            localVideoSource = null

            runCatching { localAudioTrack?.dispose() }
            localAudioTrack = null
            runCatching { localAudioSource?.dispose() }
            localAudioSource = null

            runCatching { surfaceTextureHelper?.dispose() }
            surfaceTextureHelper = null

            _isMuted.value = false
            _connectionState.value = "Ended"
            runCatching {
                val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.mode = AudioManager.MODE_NORMAL
            }
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
