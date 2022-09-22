package com.vdotok.streaming.peers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.vdotok.streaming.WebRtcClient
import com.vdotok.streaming.commands.*
import com.vdotok.streaming.enums.CallType
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.interfaces.CallSDKListener
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.utils.*
import org.webrtc.*
import java.util.regex.Matcher
import java.util.regex.Pattern


// we are going to pass call type value in the constructor
// but we will not pass the media type here we can decide that from  screen share flag
class PeerConnectionClient(
    private val client: WebRtcClient,
    private val isInitiator: Boolean,
    val toPeerName: ArrayList<String>,
    val refId: String,
    private val context: Context,
    intent: Intent? = null,
    private var projectId: String,
    var callParams: CallParams,
    private var streamCallback: CallSDKListener,
    var iceCandidatesList: ArrayList<IceCandidate> = ArrayList(),
    var isSameUser: Boolean = true

) : SdpObserver,
    PeerConnection.Observer {
    lateinit var stream: MediaStream
    lateinit var pc: PeerConnection
    private var isVideoCapturerAvailable: Boolean = false
    private var isScreenCapturerAvailable: Boolean = false
    var IpsList = ArrayList<String>()

    /**
     * kotlin init block called when class is formed and here we make our
     * PeerConnection Client that will be used for the communication with the settings from the webRTC client
     * -> setup local Peer Connection
     * -> setup peer connection configurations
     * -> setup audio video tracks based on session and call types provided
     **/
    init {
        val rtcConfig = PeerConnection.RTCConfiguration(client.iceServers)
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE
        rtcConfig.continualGatheringPolicy =
            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        createLocalPeerConnection()?.let {
            pc = it

            if (callParams.sessionType == SessionType.CALL) {
                if (callParams.mediaType == MediaType.VIDEO) {
                    if (callParams.callType == CallType.ONE_TO_ONE || callParams.callType == CallType.MANY_TO_MANY) {
                        addAudioVideoTrackToPeerConnection(it)
                    } else if (callParams.callType == CallType.ONE_TO_MANY && isInitiator) {
                        addAudioVideoTrackToPeerConnection(it)
                    }
                } else if (callParams.mediaType == MediaType.AUDIO) {
                    if (callParams.callType == CallType.ONE_TO_ONE || callParams.callType == CallType.MANY_TO_MANY) {
                        addAudioTrackToPeerConnection(it)
                    } else if (callParams.callType == CallType.ONE_TO_MANY && isInitiator) {
                        addAudioTrackToPeerConnection(it)
                    }
                }
            } else if (callParams.sessionType == SessionType.SCREEN && isInitiator) {
                startScreenShare(intent)
                pc.addTrack(ssVideoTrack)
                pc.addTrack(ssAudioTrack)
            }
            pc.createOffer(this, client.pcConstraints)
        }

    }

    /**
     * initiate and add audio and video tracks to the peer connection
     * @param peerConnection -> peer connection to which the audio track made are set to
     **/
    private fun addAudioVideoTrackToPeerConnection(peerConnection: PeerConnection) {
        initAudioTrack()
        startCamera()
        peerConnection.addTrack(audioTrack)
        peerConnection.addTrack(videoTrack)
    }

    /**
     * initiate and add audio tracks to the peer connection
     * @param peerConnection -> peer connection to which the audio track made are set to
     **/
    private fun addAudioTrackToPeerConnection(peerConnection: PeerConnection) {
        initAudioTrack()
        peerConnection.addTrack(audioTrack)

    }

    /**
     * method to make the peer connection by calling WebRTC "createPeerConnection" method and passing
     * ice server address
     * @return returns a formed Peer Connection object
     **/
    private fun createLocalPeerConnection(): PeerConnection? {

//        val iceServers: MutableList<PeerConnection.IceServer> = ArrayList()
//        val iceServer: PeerConnection.IceServer =
//            PeerConnection.IceServer.builder("stun:ab.togee.io:65000").createIceServer()
//        iceServers.add(iceServer)
        return client.peerConnectionFactory.createPeerConnection(
            client.iceServers,
            this
        )
    }

    /**
     * method initiate the Audio tracks for the session
     **/
    private fun initAudioTrack() {
        if (audioTrack == null) {
            // create AudioSource
            val audioSource = client.peerConnectionFactory.createAudioSource(MediaConstraints())
            audioTrack = client.peerConnectionFactory.createAudioTrack("201", audioSource)
            Log.e("audioTrackStatus", "initAudioTrack: $audioTrack")
        }
    }

    /**
     * method to start the camera of the device when a peer connection is made
     * and provide the camera stream to the application layer
     * -> is called in one2one or many2many video call and
     * -> is called in one2many video call only in case of initiator
     **/
    private fun startCamera() {

        val eglBaseContext = EglBase.create().eglBaseContext

        if (videoTrack == null) {
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
            // create VideoCapturer
            videoCapturer = createCameraCapturer(context)

            val videoSource = client.peerConnectionFactory.createVideoSource(true)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            videoCapturer?.startCapture(640, 960, 20)
            isVideoCapturerAvailable = true

            // create VideoTrack
            videoTrack = client.peerConnectionFactory.createVideoTrack("200", videoSource)
            // display in localView
//        send videoTrack back to application
            videoTrack?.let { streamCallback.onCameraStream(it) }
        }
    }

    /**
     * method to set the audio video tracks for the screen share sessions
     **/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun startScreenShare(mediaProjectionResultData: Intent?) {
        val eglBaseContext = EglBase.create().eglBaseContext

        if (ssAudioTrack == null) {

            val audioSource = client.peerConnectionFactory.createAudioSource(MediaConstraints())

            ssAudioTrack = client.peerConnectionFactory.createAudioTrack("101", audioSource)
        }

        if (ssVideoTrack == null) {
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
            // create VideoCapturer
            // VideoCapturer videoCapturer =createCameraCapturer();
            screenCapturer = mediaProjectionResultData?.let { createScreenCapture(it) }!!

            val videoSource = client.peerConnectionFactory.createVideoSource(
                screenCapturer?.isScreencast ?: false
            )
            screenCapturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)

            screenCapturer?.startCapture(640, 960, 15)
            isScreenCapturerAvailable = true

            ssVideoTrack = client.peerConnectionFactory.createVideoTrack("100", videoSource)

            ssAudioTrack?.setEnabled(true)
        }

        // this.videoTrack.setEnabled(false);
        // display in localView
        //videoTrack?.addSink(localView);
    }

    /**
     * method to initiate the reInvite procedure
     * @param sdp -> session description created upon creating a peer connection
     **/
    private fun reInviteSession(sdp: SessionDescription) {
        val initiateReInviteCommand = ReInviteCommand(
            refId,
            callParams.mcToken,
            callParams.sessionUUID,
            sdp.description,
            if (callParams.isReInvite) 0 else 1,
            projectId
        )
        client.sendMessage(initiateReInviteCommand)
        pc.setLocalDescription(this, sdp)
    }

    /**
     * method to initiate the session i.e. CALL,SCREEN_SHARE
     * @param sdp -> session description created upon creating a peer connection
     **/
    private fun initiateCall(sdp: SessionDescription) {
        /// NEED to update the logic of the code
        // we have to pass the call type wether it is one to one or one to many or many to many
        // we cant decide on the base of participants count that its one to one call or one to many or many to many
        // for current logic we will always do one to many call call in the case of screen share
        if (callParams.sessionType == SessionType.SCREEN)
            callParams.callType = CallType.ONE_TO_MANY

        val initiateCallCommand = MakeCallCommand(
            toPeerName,
            refId,
            sdp.description,
            projectId,
            callParams
        )
        client.sendMessage(initiateCallCommand)
        pc.setLocalDescription(this, sdp)
    }

    /**
     * method to stop and ongoing call
     * @param projectID -> project id that is to be set while initiating StreamingSDK
     **/
    fun stopOngoingCall(projectID: String) {
        client.sendMessage(
            StopCallCommand(
                if (callParams.isBroadcast == 0) toPeerName[0] else "",
                callParams.sessionUUID,
                callParams.mcToken,
                projectID
            )
        )
//        dispose()
    }

    /**
     * method to reset the capturer variables that are set when the sessions start
     * @param sessionType -> type of the SESSION whose variable needs to be reset
     **/
    fun resetCapturerVariables(sessionType: SessionType) {
        if (sessionType == SessionType.CALL)
            isVideoCapturerAvailable = false
        if (sessionType == SessionType.SCREEN)
            isScreenCapturerAvailable = false
    }

    /**
     * method to accept the call from receiver side
     * @param sdp -> session description created upon creating a peer connection
     **/
    private fun receiveCall(sdp: SessionDescription) {
        val receiverCallCommand =
            AcceptCallCommand(
                refId,
                sdp.description,
                projectId,
                callParams
            )
        client.sendMessage(receiverCallCommand)
        pc.setLocalDescription(this, sdp)

    }

    /**
     * method used to request the participant's stream when in one2many or many2many session
     * @param sdp -> session description created upon creating a peer connection
     **/
    private fun requestStream(sdp: SessionDescription) {
        val requestStream = RequestStreamCommand(
            client.callParams.refId, 
            refId,
            callParams.mcToken,
            callParams.sessionUUID,
            refId,
            sdp.description,
            projectId
        )
        client.sendMessage(requestStream)
        pc.setLocalDescription(this, sdp)
    }

    /**
     * method to disable or enable device microphone
     **/
    fun disableEnableMic() {
        if (audioTrack?.enabled() == true)
            audioTrack?.setEnabled(false)
        else {
            audioTrack?.setEnabled(true)
        }
    }

    /**
     * method to disable or enable internal audio in a screen share session
     **/
    fun disableEnableInternalAudio() {
        ssAudioTrack?.let {
            it
            if (it.enabled())
                it.setEnabled(false)
            else {
                it.setEnabled(true)
            }
        }
    }

    /**
     * method to check if audio track of a session is active or not
     * @return returns true if audio is enabled else false
     **/
    fun isAudioEnabled(): Boolean {
        audioTrack?.let {
            return it.enabled()
        } ?: kotlin.run {
            return false
        }
    }

    /**
     * method to check if internal audio track of a screen share session is active or not
     * @return returns true if internal audio is enabled else false
     **/
    fun isInternalAudioEnabled(): Boolean {
        ssAudioTrack?.let {
            return it.enabled()
        } ?: kotlin.run {
            return false
        }
    }

    /**
     * method to check if video capturer is active in a video call session
     * @return returns true if video capturer is enabled else false
     **/
    fun isVideoCapturerEnabled(): Boolean {
        return isVideoCapturerAvailable
    }

    /**
     * method to check if screen capturer is active in a video call session
     * @return returns true if screen capturer is enabled else false
     **/
    fun isScreenCapturerEnabled(): Boolean {
        return isScreenCapturerAvailable
    }

    /**
     * method to switch camera of a session from front to back and vice versa
     **/
    fun switchCamera() {
        val cameraVideoCapturer = videoCapturer as CameraVideoCapturer
        cameraVideoCapturer.switchCamera(null)
    }

    /**
     * method to pause a video stream of a video session by stopping video capturer
     **/
    fun pauseVideo() {
        videoCapturer?.stopCapture()
        isVideoCapturerAvailable = false
        videoTrack?.setEnabled(false)
    }

    /**
     * method to resume a video stream of a video session by stopping video capturer
     **/
    fun resumeVideo() {
        videoCapturer?.startCapture(480, 640, 15)
        isVideoCapturerAvailable = true
        videoTrack?.setEnabled(true)
    }

    /**
     * method to pause a video stream of a screen share session by stopping screen capturer
     **/
    fun pauseSSVideo() {
        ssVideoTrack?.setEnabled(false)
        isScreenCapturerAvailable = false
    }

    /**
     * method to resume a video stream of a screen share session by stopping screen capturer
     **/
    fun resumeSSVideo() {
        ssVideoTrack?.setEnabled(true)
        isScreenCapturerAvailable = true
    }

    override fun onSetFailure(p0: String?) {

    }

    override fun onSetSuccess() {

    }

    override fun onCreateSuccess(sdp: SessionDescription) {
        Log.d("localPeerConnection", sdp.type.canonicalForm())
        when (sdp.type.canonicalForm()) {
            "offer" -> {
                when {
                    isInitiator && !callParams.isReInvite -> {
                        initiateCall(sdp)
                        return
                    }

                    callParams.isReInvite && isSameUser -> {
                        reInviteSession(sdp)
                        return
                    }
                }
            }
            "answer" -> {
            }
            "pranswer" -> {
            }
        }
//        }

//        if not initiator
        if (toPeerName.isNullOrEmpty()) {
            requestStream(sdp)
        } else
            receiveCall(sdp)

    }

    override fun onCreateFailure(p0: String?) {

    }


    override fun onIceCandidate(iceCandidate: IceCandidate) {
        val candidate = OnIceCandidate(
            referenceID = refId,
            sessionUUID = callParams.sessionUUID,
            candidate = iceCandidate
        )
        useRegex(iceCandidate.sdp)
        client.sendMessage(candidate.compile())
    }

    fun useRegex(input: String): ArrayList<String> {
        val matcherValue = "raddr"
        if (input.contains(matcherValue, true) || input.contains("host", true) &&
            input.contains("network-id 3", true)
        ) {
            val IPADDRESS_PATTERN =
                "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
            val pattern = Pattern.compile(IPADDRESS_PATTERN)
            val matcher: Matcher = pattern.matcher(input)
            if (matcher.find()) {
                IpsList.add(matcher.group().toString())
            }
        }
        return IpsList
    }

    override fun onDataChannel(p0: DataChannel?) {

    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {

    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {

    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
            var codecList: List<RtpParameters.Codec> = ArrayList()
            val sender = pc.senders
            sender.forEach { sender ->
                if (sender.track()?.kind() == "video") {
                    codecList = sender.getParameters().codecs;
                }
            }
            codecList.forEach {
                Log.e("SupportedCodec", it.name)
            }

        }
    }

    //verify Your Own
//    if we are the initiator of many to many we will not receive remote stream in our local peerConnection
//    if we are the initiator of one to many we will not receive remote stream any where
//    in any case of audio call we will not receive any stream
    override fun onAddStream(stream: MediaStream) {
        if (callParams.callType == CallType.MANY_TO_MANY && toPeerName.isNotEmpty()) {
            return
        } else if (MediaType.AUDIO == callParams.mediaType) {
            streamCallback.onRemoteStream(refId, callParams.sessionUUID)
        } else if (stream.audioTracks.size <= 1 && stream.videoTracks.size <= 1) {
            if (callParams.callType == CallType.ONE_TO_MANY && isInitiator) {
                return
            }
            val remoteVideoTrack = stream.videoTracks[0]
            remoteVideoTrack?.setEnabled(true)
            if (toPeerName.isNullOrEmpty())
                streamCallback.onRemoteStream(remoteVideoTrack, refId, callParams.sessionUUID)
            else
                streamCallback.onRemoteStream(
                    remoteVideoTrack,
                    toPeerName[0],
                    callParams.sessionUUID
                )
        }
    }

    override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {
        if (PeerConnection.SignalingState.STABLE == signalingState) {
            val it: MutableIterator<IceCandidate> =
                iceCandidatesList.iterator()
            while (it.hasNext()) {
                val candidate = it.next()
                pc.addIceCandidate(candidate)
                it.remove()
            }
        }
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {

    }

    override fun onRemoveStream(p0: MediaStream?) {
        Log.e("onRemoveStream: ", "StreamREmoved")
    }

    override fun onRenegotiationNeeded() {

    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {

    }

    /**
     * method to set a remote view sink for the video stream
     **/
    fun setView(videoView: SurfaceViewRenderer) {
        this.stream.videoTracks[0].addSink(videoView)
    }

    /**
     * method to get the peer connection object
     **/
    fun getPeerConnection(): PeerConnection {
        return pc
    }

    //    public MediaStream getMediaStream() {
    //        return this.mediaStream;
    //    }
    //
    //    public void setMediaStream(MediaStream mediaStream) {
    //        this.mediaStream = mediaStream;
    //    }

    /**
     * method to dispose a peer connection
     **/
    fun dispose() {
        try {
            this.pc.close()
        } catch (e: Exception) {
            Log.e("Dispose PeerConnection", e.message!!)
        }
    }
}
