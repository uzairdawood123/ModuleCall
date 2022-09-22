package com.vdotok.streaming

import android.content.Context
import android.content.Intent
import android.media.AudioRecord
import android.util.Log
import com.vdotok.streaming.commands.CommandBase
import com.vdotok.streaming.enums.CallType
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.interfaces.CallSDKListener
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.peers.PeerConnectionClient
import com.vdotok.streaming.utils.*
import com.vdotok.streaming.websocket.CustomWebSocket

import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.*
import kotlin.collections.HashMap
import org.webrtc.MediaConstraints





// we will init WebRtcClint with well know knowledge of call type
// so we can handle the list of remote participant for many2Many call
class WebRtcClient(
    var socketService: CustomWebSocket?,
    private val audioRecorder: AudioRecord,
    private val context: Context,
    private var intent: Intent? = null,
    var isInitiator: Boolean = true,
    var callParams: CallParams,
    private val streamCallback: CallSDKListener
) {
    var peerConnectionFactory: PeerConnectionFactory
    var iceServers = LinkedList<PeerConnection.IceServer>()
    var pcConstraints = MediaConstraints()
    var prefs: Prefs = Prefs(context)

    lateinit var peerConnectionClient: PeerConnectionClient

    //Add HashMap for many to many to handle local and remote participants using peerConnection client class
    var m2MPeerConnectionClients: HashMap<String, PeerConnectionClient> = HashMap()

    /**
     * kotlin init block called when class is formed and here we make our
     * PeerConnection Client that will be used for the communication i.e. call or screen share sessions.
     * -> setup configurations for the peer connection
     * -> setup ice candidate servers
     * -> setup peer connection object
     **/
    init {

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        val videoEncoderFactory: VideoEncoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext, true, true
        )
        
        for (i in videoEncoderFactory.supportedCodecs.indices) {
            Log.d("Codecs", "Supported codecs: " + videoEncoderFactory.supportedCodecs[i].name)
        }


        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(SoftwareVideoEncoderFactory())
            .setVideoDecoderFactory(SoftwareVideoDecoderFactory())
            .setOptions(PeerConnectionFactory.Options())
            .setAudioDeviceModule(createJavaAudioDevice())

        peerConnectionFactory = builder.createPeerConnectionFactory()


        iceServers.add(PeerConnection.IceServer.builder("stun:23.21.150.121").createIceServer())
        iceServers.add(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

        val isOfferToReceiveVideo = when (callParams.mediaType) {
            MediaType.AUDIO -> false
            MediaType.VIDEO -> true
            MediaType.DEFAULT -> false
        }
        pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo",isOfferToReceiveVideo.toString()))
        if (isOfferToReceiveVideo){
            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxHeight",Integer.toString(960)))
            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxWidth", Integer.toString(640)))
            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(30)))
            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(15)))
//            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxHeight",Integer.toString(2340)))
//            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxWidth", Integer.toString(1080)))
//            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(60)))
////            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("minFrFameRate", Integer.toString(15)))
//            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxAspectRatio", "1.77"))


//            pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("maxAspectRatio", "5/3"))

        }
        pcConstraints.optional.add(MediaConstraints.KeyValuePair("VoiceActivityDetection", "true"))
        pcConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        pcConstraints.optional.add(MediaConstraints.KeyValuePair("IceRestart", "true"))

//        if (mediaType.value != MediaType.SCREEN.value)
//            initRendererViews()


        initPeerConnection()

        if (callParams.callType == CallType.MANY_TO_MANY)
            m2MPeerConnectionClients[callParams.refId] = peerConnectionClient

    }

    /**
     * method to make a Peer Connection client object for the local user
     **/
    private fun initPeerConnection() {
        prefs.projectID?.let { tenantID ->
            peerConnectionClient = PeerConnectionClient(
                this,
                isInitiator,
                callParams.toRefIds,
                callParams.refId,
                context,
                intent,
                tenantID,
                callParams,
                streamCallback,
                isSameUser = true
            )
        }
    }
    // initPeerConnectionForRemoteParticipants

    /**
     * method used to create the AUDIO_DEVICE_MODULE that is to be set to the PC configurations
     * for audio usage.
     **/
    private fun createJavaAudioDevice(): AudioDeviceModule {

        // Set audio record error callbacks.
        val audioRecordErrorCallback: JavaAudioDeviceModule.AudioRecordErrorCallback = object :
            JavaAudioDeviceModule.AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {}

            override fun onWebRtcAudioRecordStartError(
                errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode,
                errorMessage: String
            ) {
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {}
        }
        val audioTrackErrorCallback: JavaAudioDeviceModule.AudioTrackErrorCallback = object :
            JavaAudioDeviceModule.AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {}

            override fun onWebRtcAudioTrackStartError(
                errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode,
                errorMessage: String
            ) {
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {}
        }

        return JavaAudioDeviceModule.builder(context)
            .setSampleRate(SAMPLE_RATE_IN_HZ)
            .setUseStereoInput(true)
            .setAudioSource(if (callParams.isAppAudio) AUDIO_SOURCE_APP_AUDIO else AUDIO_SOURCE_MIC)
            .setAudioFormat(AUDIO_FORMAT)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .setAudioRecorder(audioRecorder)
            .createAudioDeviceModule()
    }

    /**
     * method to send the message to the server
     * @param message -> class of JSON format to be sent
     **/
    fun sendMessage(message: CommandBase) {
        socketService?.let {
            if (it.isConnected()) {
                Log.i("Sent Command ", message.compile())
                it.sendMessage(message.compile())
            } else {
                Log.e("Socket Error ", "Socket service is not open")
            }
        }
    }

    /**
     * method to send the message to the server
     * @param message -> class of String format to be sent
     **/
    fun sendMessage(message: String) {
        socketService?.let {
            if (it.isConnected()) {
                Log.i("Sent String ", message)
                it.sendMessage(message)
            } else {
                Log.e("Socket Error ", "Socket service is not open")
            }
        }
    }

    /**
     * method to get a session's MEDIA_TYPE
     * @return returns a MEDIA_TYPE Enum i.e. AUDIO,VIDEO
     **/
    fun getSessionMediaType(): MediaType {
        return callParams.mediaType
    }

    /**
     * method to get a session's SESSION_TYPE
     * @return returns a SESSION_TYPE Enum i.e. CALL,SCREEN
     **/
    fun getSessionType(): SessionType {
        return callParams.sessionType
    }

    /**
     * method to get a session's CALL_TYPE
     * @return returns a CALL_TYPE Enum i.e. one2one, one2many, many2many
     **/
    fun getCallType(): CallType {
        return callParams.callType
    }

    /**
     * method to make a Peer Connection client object for a remote participant
     * @param remoteRefId -> reference Id of the participant or receiver
     **/
    fun createRemoteParticipant(remoteRefId: String) {
        prefs.projectID?.let { tenantID ->
            m2MPeerConnectionClients.put(
                remoteRefId, PeerConnectionClient(
                    this,
                    false,
                    arrayListOf(),
                    remoteRefId,
                    context,
                    intent,
                    tenantID,
                    callParams,
                    streamCallback,
                    isSameUser = false
                )
            )
        }
    }
}