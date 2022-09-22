package com.vdotok.streaming

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.vdotok.streaming.commands.*
import com.vdotok.streaming.crashReporting.TopExceptionHandler
import com.vdotok.streaming.enums.*
import com.vdotok.streaming.interfaces.CallSDKListener
import com.vdotok.streaming.models.*
import com.vdotok.streaming.stats.WebRtcStatsLogger
import com.vdotok.streaming.utils.*
import com.vdotok.streaming.utils.FileUtils.crashFileExists
import com.vdotok.streaming.utils.FileUtils.readCrashFile
import com.vdotok.streaming.utils.FileUtils.removeFile
import com.vdotok.streaming.websocket.CustomSocketCallBacks
import com.vdotok.streaming.websocket.CustomWebSocket
import kotlinx.coroutines.*
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


class CallClient private constructor(private val context: Context) {

    val CAPTURE_PERMISSION_REQUEST_CODE = 10001

    private var socketService: CustomWebSocket? = null
    private var sessions: LinkedHashMap<String, WebRtcClient> = LinkedHashMap()

    private var listener: CallSDKListener? = null

    private val gson = Gson()
    private val prefs: Prefs = Prefs(context)
    private var mediaProjection: MediaProjection? = null
    private var isAppAudioEnabled: Boolean = false
    private var mediaIntent: Intent? = null
    var micAudioRecorder: AudioRecord? = null
    private var audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private lateinit var webRtcStatsLogger: WebRtcStatsLogger

    /**
     * kotlin init block called when the class is formed and webRTCLogger is initiated inside it
     **/
    init {
        WebRtcStatsLogger.getInstance(context)?.let {
            webRtcStatsLogger = it
        }
    }

    /**
     * Method used for initiating the socket service for performing communication
     * @param streamHostURL -> host url to which the socket service will connect
     * @param streamEndPoint -> the endpoint for the host url
     **/
    fun connect(streamHostURL: String, streamEndPoint: String) {
        socketService =
            CustomWebSocket(context, streamHostURL, streamEndPoint, object : CustomSocketCallBacks {
                override fun onConnect() {
                    listener?.connectionStatus(EnumConnectionStatus.OPEN)
                }

                override fun onDisconnect() {
                    listener?.onClose("Successfully Disconnected ")
                    clearCallSession()
                }

                override fun onMessage(response: String) {
                    handleSocketResponse(response)
                }

                override fun connectionStatus(status: EnumConnectionStatus) {
                    Log.e("Connection Status", status.name)
                    listener?.connectionStatus(status)
                    if (status == EnumConnectionStatus.CLOSED)
                        clearCallSession()
                }

                override fun onError(cause: String) {
                    listener?.onError(cause)

                }
            })
        socketService?.connect()
    }


    /**
     * method to clear all sessions data from the client local map
     **/
    private fun clearCallSession() {
        sessions.forEach {
            if (it.value.getCallType() == CallType.MANY_TO_MANY) {
                disposePcClients(it.value)
                it.value.peerConnectionClient.dispose()
            } else it.value.peerConnectionClient.dispose()
            it.value.socketService?.disconnect()
            it.value.socketService = null
            stopCommunicationDevices(it.value.getSessionType())
            listener?.sessionHold(it.key)
        }
    }

    /**
     * method to handle the socket response
     * @param response -> string response or JSON response from the server to a sent command
     **/
    private fun handleSocketResponse(response: String) {
        Log.e("Socket Response ", response)
        val serverResponse = gson.fromJson(response, ServerResponse::class.java)

//        this is temporary fix till backend puts ping inside request type param
//        if (serverResponse.id.equals("pong")) {
//            prefs.mcToken?.let { token ->
//                sendMessage(
//                    PongCommand(
//                        token
//                    )
//                )
//                listener?.connectionStatus(EnumConnectionStatus.SOCKET_PING)
//            }
//        }

        serverResponse?.requestType?.let { requestType ->
            when (requestType) {
                IdResponse.REGISTER.id,
                IdResponse.UN_REGISTER.id -> {
                    Log.e("Socket Response ", response)
                    handleRegisterResponse(serverResponse)
                }

                IdResponse.STATE_INFORMATION.id -> {
                    Log.e("Socket Response ", response)
                    val currentSession = sessions[serverResponse.sessionUUID]
                    currentSession?.let { session ->
                        listener?.audioVideoState(
                            SessionStateInfo(
                                sessionKey = serverResponse.sessionUUID,
                                refID = serverResponse.referenceID,
                                audioState = serverResponse.audioInformation,
                                videoState = serverResponse.videoInformation,
                                isScreenShare = session.getSessionType() == SessionType.SCREEN
                            )
                        )
                    }
                }

                IdResponse.START_COMMUNICATION.id -> {
                    val responseSession = sessions[serverResponse.sessionUUID]
                    val sdp =
                        SessionDescription(SessionDescription.Type.ANSWER, serverResponse.sdpAnswer)
                    responseSession?.let {
                        onRemoteDescription(sdp, responseSession, serverResponse.referenceID)
                    }
                }

                IdResponse.CALL_RESPONSE.id -> {
                    val responseSession = sessions[serverResponse.sessionUUID]
                    val sdp =
                        SessionDescription(SessionDescription.Type.ANSWER, serverResponse.sdpAnswer)
                    responseSession?.let {
                        onRemoteDescription(sdp, responseSession, serverResponse.referenceID)
                    }
                }

                IdResponse.SDK_CRASH_REPORT.id -> {
                    when (serverResponse.responseCode) {
                        ResponseCodes.REQUEST_SUCCESS.value -> removeFile(context)
                        else -> {
                        }
                    }
                }

                IdResponse.STOP_COMMUNICATION.id -> {
                    handleSessionCancelResponse(serverResponse)
                }

                IdResponse.SESSION_INVITE.id -> {
                    handleSessionInviteResponse(serverResponse)
                }

                IdResponse.SESSION_TIMEOUT.id -> {
                    handleSessionTimeoutResponse(serverResponse)
                }

                IdResponse.SESSION_BUSY.id -> {
                    handleSessionBusyResponse(serverResponse)
                }

                IdResponse.SESSION_REJECTED.id -> {
                    handleSessionRejected(serverResponse)
                }

                IdResponse.PUBLIC_URL.id -> {
                    listener?.onPublicURL(serverResponse.url)
                }

                IdResponse.SESSION_BREAK.id -> {
                    listener?.sessionHold(serverResponse.sessionUUID)
                }

                // Create a object with name Call params and return all the required data in single object of callParams
                // call params object will also used in dial call if dial call parameters is more then 3
                IdResponse.INCOMING_CALL.id -> {
                    serverResponse.from.let {
                        listener?.incomingCall(
                            callParams = CallParams(
                                refId = it,
                                sessionUUID = serverResponse.sessionUUID,
                                requestId = serverResponse.requestID,
                                callType = CallType.valueOf(
                                    serverResponse.call_type.toUpperCase(
                                        Locale.ROOT
                                    )
                                ),
                                mediaType = MediaType.valueOf(
                                    serverResponse.media_type.toUpperCase(
                                        Locale.ROOT
                                    )
                                ),
                                sessionType = SessionType.valueOf(
                                    serverResponse.session_type.toUpperCase(
                                        Locale.ROOT
                                    )
                                ),
                                associatedSessionUUID = serverResponse.associatedSessionUUID ?: "",
                                customDataPacket = serverResponse.data.toString()
                            )
                        )
                    }
                }

                //Need To check this logic at least once before actually working
                // in many to many we have to set ice candidates with refID
                IdResponse.ICE_CANDIDATE.id -> {
                    val candidate = IceCandidate(
                        serverResponse.candidate.sdpMid,
                        serverResponse.candidate.sdpMLineIndex,
                        serverResponse.candidate.candidate
                    )
                    onRemoteIceCandidate(
                        OnIceCandidate(
                            serverResponse.referenceID,
                            serverResponse.sessionUUID,
                            candidate
                        )
                    )
                }

                IdResponse.NEW_PARTICIPANT_ARRIVED.id -> {
                    val responseSession = sessions[serverResponse.sessionUUID]
                    responseSession?.let { client ->
                        if (client.callParams.callType != CallType.ONE_TO_MANY) {
                            client.createRemoteParticipant(serverResponse.referenceID)
                        }
                    }
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.NEW_PARTICIPANT_ARRIVED,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(
                                toRefIds = arrayListOf(serverResponse.referenceID),
                                participantCount = serverResponse.total_participants ?: 0
                            )
                        )
                    )
                }

                IdResponse.EXISTING_PARTICIPANTS.id -> {
                    val responseSession = sessions[serverResponse.sessionUUID]
                    for (refId in serverResponse.referenceIDs) {
                        if (refId != prefs.ownRefId) responseSession?.createRemoteParticipant(refId)
                    }
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.EXISTING_PARTICIPANTS,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(
                                toRefIds = serverResponse.referenceIDs,
                                participantCount = serverResponse.total_participants ?: 0
                            )
                        )
                    )
                }

                IdResponse.HV_INFO.id -> {}

                IdResponse.PONG.id -> {}

                else -> {
                }
            }
        }

    }

    /**
     * method to handle the session's REJECTED response
     * @param serverResponse -> string response or JSON response from the server to a sent command
     **/
    private fun handleSessionRejected(serverResponse: ServerResponse) {
        when (serverResponse.responseCode) {
            ResponseCodes.CALL_REJECTED.value -> {
                listener?.callStatus(
                    CallInfoResponse(
                        callParams = CallParams(
                            refId = serverResponse.referenceID,
                            callType = CallType.valueOf(
                                serverResponse.call_type.toUpperCase(
                                    Locale.ROOT
                                )
                            ),
                            sessionType = SessionType.valueOf(
                                serverResponse.session_type.toUpperCase(
                                    Locale.ROOT
                                )
                            ),
                            sessionUUID = serverResponse.sessionUUID
                        ),
                        callStatus = CallStatus.CALL_REJECTED,
                        responseMessage = serverResponse.responseMessage
                    )
                )
            }

            else -> {
            }
        }
    }

    /**
     * method to handle the session's CANCEL procedure
     * @param webRtcClient -> webrtc connection client containing session configurations and data
     * @param serverResponse -> string response or JSON response from the server to a sent command
     **/
    private fun sessionCancel(webRtcClient: WebRtcClient, serverResponse: ServerResponse) {
        if (webRtcClient.getCallType() == CallType.MANY_TO_MANY) {
            disposePcClients(webRtcClient)
            webRtcClient.peerConnectionClient.dispose()
        } else webRtcClient.peerConnectionClient.dispose()
        sessions.remove(webRtcClient.callParams.sessionUUID)

//        listener?.sessionEndedData(
//            CallParams(
//                sessionUUID = webRtcClient.callParams.sessionUUID,
//                isInitiator = webRtcClient.isInitiator,
//                callType = webRtcClient.getCallType(),
//                mediaType = webRtcClient.getSessionMediaType(),
//                sessionType = webRtcClient.getSessionType()
//            )
//        )

        listener?.callStatus(
            CallInfoResponse(
                callStatus = CallStatus.OUTGOING_CALL_ENDED,
                responseMessage = serverResponse.responseMessage,
                callParams = CallParams(
                    sessionUUID = webRtcClient.callParams.sessionUUID,
                    isInitiator = webRtcClient.isInitiator,
                    callType = webRtcClient.getCallType(),
                    mediaType = webRtcClient.getSessionMediaType(),
                    sessionType = webRtcClient.getSessionType()
                )
            )
        )
        stopCommunicationDevices(webRtcClient.getSessionType())
    }

    /**
     * method to handle the session's CANCEL response
     * @param serverResponse -> string response or JSON response from the server to a sent command
     **/
    private fun handleSessionCancelResponse(serverResponse: ServerResponse) {
        when (serverResponse.responseCode) {
            ResponseCodes.SESSION_CANCEL_SUCCESS.value -> {
                val currentSession = sessions[serverResponse.sessionUUID]
                currentSession?.let {
                    CoroutineScope(Dispatchers.Default).launch() {
                        if (checkPermissionGranted(null)) {
                            val endDataResult = async {
                                webRtcStatsLogger.getEndSessionLogs(it)
                            }
                            webRtcStatsLogger.sendEndDataLogs(it, endDataResult.await())
                        }
                        sessionCancel(it, serverResponse)
                    }

                } ?: kotlin.run {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.NO_SESSION_EXISTS,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(sessionUUID = serverResponse.sessionUUID)
                        )
                    )
                }
            }

            ResponseCodes.INSUFFICIENT_BALANCE.value -> {
                val currentSession = sessions[serverResponse.sessionUUID]
                currentSession?.let {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.INSUFFICIENT_BALANCE,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(sessionUUID = it.callParams.sessionUUID)
                        )
                    )
                    it.peerConnectionClient.dispose()
                    sessions.remove(it.callParams.sessionUUID)
                    stopCommunicationDevices(it.getSessionType())
                }
            }

            ResponseCodes.SERVICE_SUSPENDED.value -> {
                val currentSession = sessions[serverResponse.sessionUUID]
                currentSession?.let {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.SERVICE_SUSPENDED,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(sessionUUID = it.callParams.sessionUUID)
                        )
                    )
                    it.peerConnectionClient.dispose()
                    sessions.remove(it.callParams.sessionUUID)
                    stopCommunicationDevices(it.getSessionType())
                }
            }

            ResponseCodes.SESSION_CANCELLED.value -> {
                listener?.callStatus(
                    CallInfoResponse(
                        callStatus = CallStatus.CALL_MISSED,
                        responseMessage = serverResponse.responseMessage,
                        callParams = CallParams(sessionUUID = serverResponse.sessionUUID)
                    )
                )
            }
            ResponseCodes.PARTICIPANT_LEFT_CALL.value -> {
                val responseSession = sessions[serverResponse.sessionUUID]
                responseSession?.m2MPeerConnectionClients?.get(serverResponse.referenceID)?.let {
                    it.dispose()
                }
                listener?.callStatus(
                    CallInfoResponse(
                        callStatus = CallStatus.PARTICIPANT_LEFT_CALL,
                        responseMessage = serverResponse.responseMessage,
                        callParams = CallParams(
                            toRefIds = arrayListOf(serverResponse.referenceID),
                            sessionUUID = serverResponse.sessionUUID
                        )
                    )
                )
            }

            ResponseCodes.INVALID_AUTHENTICATION.value -> {
                listener?.callStatus(
                    CallInfoResponse(
                        callStatus = CallStatus.INVALID_AUTHENTICATION,
                        responseMessage = serverResponse.responseMessage,
                        callParams = CallParams(
                            sessionUUID = serverResponse.sessionUUID
                        )
                    )
                )
            }

            else -> {
            }
        }
    }

    /**
     * method to dispose off the peer connection clients formed in the sessions
     * @param client -> webrtc connection client containing session configurations and data
     **/
    private fun disposePcClients(client: WebRtcClient) {
        val thread = Thread {
            try {
                client.m2MPeerConnectionClients.forEach {
                    if (it.value.toPeerName.size == 0)
                        it.value.dispose()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
    }

    /**
     * method to handle the session's TIMEOUT response
     * @param serverResponse -> string response or JSON response from the server to a sent command
     **/
    private fun handleSessionTimeoutResponse(serverResponse: ServerResponse) {
        when (serverResponse.responseCode) {
            ResponseCodes.REQUEST_SUCCESS.value -> {
                listener?.callStatus(
                    CallInfoResponse(
                        callStatus = CallStatus.SESSION_TIMEOUT,
                        responseMessage = serverResponse.responseMessage,
                        callParams = CallParams(
                            sessionUUID = serverResponse.sessionUUID
                        )
                    )
                )
            }

            ResponseCodes.NO_ANSWER_FROM_TARGET.value -> {
                val currentSession = sessions[serverResponse.sessionUUID]
                currentSession?.let {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.NO_ANSWER_FROM_TARGET,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(sessionUUID = it.callParams.sessionUUID)
                        )
                    )
                    it.peerConnectionClient.dispose()
                    sessions.remove(it.callParams.sessionUUID)
                    stopCommunicationDevices(it.getSessionType())
                }
            }
        }
    }

    /**
     * method to handle the session's BUSY response
     * @param serverResponse -> string response or JSON response from the server to a sent command
     **/
    private fun handleSessionBusyResponse(serverResponse: ServerResponse) {
        when (serverResponse.responseCode) {
            ResponseCodes.REQUEST_SUCCESS.value -> {
                listener?.callStatus(
                    CallInfoResponse(
                        callStatus = CallStatus.SESSION_BUSY,
                        responseMessage = serverResponse.responseMessage,
                        callParams = CallParams(
                            sessionUUID = serverResponse.sessionUUID
                        )
                    )
                )
            }

            ResponseCodes.TARGET_BUSY.value -> {
                listener?.callStatus(
                    CallInfoResponse(
                        callStatus = CallStatus.TARGET_IS_BUSY,
                        responseMessage = serverResponse.responseMessage,
                        callParams = CallParams(
                            sessionUUID = serverResponse.sessionUUID
                        )
                    )
                )
            }
        }
    }

    /**
     * method to handle the session's INVITE response
     * @param serverResponse -> string response or JSON response from the server to a sent command
     **/
    private fun handleSessionInviteResponse(serverResponse: ServerResponse) {
        when (serverResponse.responseCode) {

            ResponseCodes.REQUEST_SUCCESS.value -> {
                if (serverResponse.session_type == SessionType.CALL.value
                    || serverResponse.session_type == SessionType.SCREEN.value
                ) {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.CALL_CONNECTED,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(
                                sessionUUID = serverResponse.sessionUUID,
                                mediaType = MediaType.valueOf(
                                    serverResponse.media_type.toUpperCase(
                                        Locale.ROOT
                                    )
                                ),
                                callType = CallType.valueOf(
                                    serverResponse.call_type.toUpperCase(
                                        Locale.ROOT
                                    )
                                ),
                                sessionType = SessionType.valueOf(
                                    serverResponse.session_type.toUpperCase(
                                        Locale.ROOT
                                    )
                                )
                            )
                        )
                    )
                    if (checkPermissionGranted(null)) startWebRtcSessionLogging(serverResponse.sessionUUID)
                } else {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.CALL_NOT_CONNECTED,
                            responseMessage = serverResponse.responseMessage
                        )
                    )
                }
            }

            ResponseCodes.INSUFFICIENT_BALANCE.value -> {
                val currentSession = sessions[serverResponse.sessionUUID]
                currentSession?.let {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.INSUFFICIENT_BALANCE,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(sessionUUID = it.callParams.sessionUUID)
                        )
                    )
                    it.peerConnectionClient.dispose()
                    sessions.remove(it.callParams.sessionUUID)
                    stopCommunicationDevices(it.getSessionType())
                }
            }

            ResponseCodes.SERVICE_SUSPENDED.value -> {
                val currentSession = sessions[serverResponse.sessionUUID]
                currentSession?.let {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.SERVICE_SUSPENDED,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(sessionUUID = it.callParams.sessionUUID)
                        )
                    )
                    it.peerConnectionClient.dispose()
                    sessions.remove(it.callParams.sessionUUID)
                    stopCommunicationDevices(it.getSessionType())
                }
            }

            ResponseCodes.LOCATING_TARGET.value,
            ResponseCodes.TRYING_TO_CONNECT_TARGET.value,
            ResponseCodes.TARGET_IS_ALERTING.value -> {
                listener?.callStatus(
                    CallInfoResponse(
                        callStatus = CallStatus.TRYING_TO_LOCATE_CONNECT_ALERTING_TARGET,
                        responseMessage = serverResponse.responseMessage,
                    )
                )
            }

            ResponseCodes.TARGET_NOT_FOUND.value -> {
                listener?.callStatus(
                    CallInfoResponse(
                        callStatus = CallStatus.TARGET_NOT_FOUND,
                        responseMessage = serverResponse.responseMessage
                    )
                )
            }

            ResponseCodes.BAD_REQUEST.value -> {
                val currentSession = sessions[serverResponse.sessionUUID]
                currentSession?.let {
                    listener?.callStatus(
                        CallInfoResponse(
                            callStatus = CallStatus.BAD_REQUEST,
                            responseMessage = serverResponse.responseMessage,
                            callParams = CallParams(sessionUUID = it.callParams.sessionUUID)
                        )
                    )
                    it.peerConnectionClient.dispose()
                    sessions.remove(it.callParams.sessionUUID)
                    stopCommunicationDevices(it.getSessionType())
                }
            }

            else -> {
            }
        }
    }

    /**
     * method to handle the user's REGISTER response
     * @param serverResponse -> string response or JSON response from the server to a sent command
     **/
    private fun handleRegisterResponse(serverResponse: ServerResponse) {
        when (serverResponse.responseCode) {

            ResponseCodes.REQUEST_SUCCESS.value -> {
                if (serverResponse.requestType == RequestType.REGISTER.value) {
                    prefs.mcToken = serverResponse.mcToken
                    prefs.timeInterval = serverResponse.bytes_interval
                    prefs.pingInterval = serverResponse.ping_interval

                    listener?.registrationStatus(
                        RegisterResponse(
                            registrationStatus = RegistrationStatus.REGISTER_SUCCESS,
                            mcToken = serverResponse.mcToken,
                            bytes_interval = serverResponse.bytes_interval,
                            reConnectStatus = serverResponse.reConnect,
                            responseMessage = serverResponse.responseMessage
                        )
                    )

                    initCrashReporting(serverResponse.isLoggingEnable)
                    startServerPing()
                } else if (serverResponse.requestType == RequestType.UN_REGISTER.value) {
                    listener?.registrationStatus(
                        RegisterResponse(
                            registrationStatus = RegistrationStatus.UN_REGISTER,
                            responseMessage = serverResponse.responseMessage
                        )
                    )
                    stopServerPing()
                }
            }

            ResponseCodes.REQUEST_FAILURE.value -> {
                listener?.registrationStatus(
                    RegisterResponse(
                        registrationStatus = RegistrationStatus.REGISTER_FAILURE,
                        responseMessage = serverResponse.responseMessage
                    )
                )
                stopServerPing()
            }

            else -> {
                listener?.registrationStatus(
                    RegisterResponse(
                        registrationStatus = RegistrationStatus.INVALID_REGISTRATION,
                        responseMessage = serverResponse.responseMessage
                    )
                )
                stopServerPing()
            }
        }
    }

    private fun startServerPing() {
        socketService?.let {
            prefs.mcToken?.let { mcToken ->
                prefs.ownRefId?.let { ownRefId ->
                    prefs.projectID?.let { projectID ->
                        it.setSocketValues(mcToken, ownRefId, projectID)
                        it.startPingMessageHandler(prefs.pingInterval)
                    }
                }
            }
        }
    }

    private fun stopServerPing() {
        socketService?.let {
            it.stopPingMessageHandler()
        }
    }

    /**
     * Method to be used when initiated a public broadcast to save session info on server
     * This info will be provided to the joining participants by the server
     * @param storeInfo -> 1 if info is to be saved for the session on server otherwise 0
     * @param callParams -> session info mainly sessionId, referenceId, mcToken
     * and customDataPacket(channelKey, channelName, groupId) is to be provided
     **/
    fun sendPublicBroadcastInfoToServer(storeInfo: String, callParams: CallParams) {
        prefs.projectID?.let{ projectID ->
            val hvInfoCommand = HvInfoCommand(projectID, storeInfo, callParams.apply {
                mcToken = prefs.mcToken.toString()
                refId = prefs.ownRefId.toString()
            })
            sendMessage(hvInfoCommand)
        }
    }

    /**
     * method to initiate crash reporting
     * @param loggingEnabled -> param from server to check logging is to be enabled or not
     **/
    private fun initCrashReporting(loggingEnabled: Int) {
        if (loggingEnabled == EnumCrashLogging.ENABLED.value)
            Thread.setDefaultUncaughtExceptionHandler(TopExceptionHandler(context))

        sendCrashReport()
    }

    /**
     * method to send the crash report if there is any immediately after registration
     **/
    private fun sendCrashReport() {
        if (crashFileExists(context)) {
            val data = readCrashFile(context)
            if (data.isNotEmpty()) {
                prefs.ownRefId?.let { refID ->
                    prefs.projectID?.let { projectID ->
                        prefs.mcToken?.let { mcToken ->
                            sendMessage(
                                CrashReportCommand(
                                    projectID = projectID,
                                    referenceId = refID,
                                    mcToken = mcToken,
                                    report = CrashReport(
                                        device_model = getDeviceName(),
                                        device_os = Build.VERSION.SDK_INT.toString(),
                                        crash_detail = data
                                    )
                                )
                            )
                        }
                    }
                }
            } else {
                Log.e("Crash Data >>>", "sendCrashReport: Crash Data is Empty")
            }
        }
    }

    /**
     * method to set the participants remote description for the session
     * @param sdp -> session description
     * @param responseSession -> webrtc connection client containing session configurations and data
     * @param referenceID -> reference id of the remote participant
     **/
    private fun onRemoteDescription(
        sdp: SessionDescription,
        responseSession: WebRtcClient,
        referenceID: String
    ) {
        val sdpDescription = sdp.description
        val sdpRemote = SessionDescription(sdp.type, sdpDescription)

        if (responseSession.getCallType() == CallType.MANY_TO_MANY) {
            responseSession.m2MPeerConnectionClients[referenceID]?.let {
                it.pc.setRemoteDescription(it, sdpRemote)
            }
        } else {
            responseSession.peerConnectionClient.pc.setRemoteDescription(
                responseSession.peerConnectionClient,
                sdpRemote
            )
            responseSession.peerConnectionClient.pc.createAnswer(
                responseSession.peerConnectionClient,
                responseSession.pcConstraints
            )
        }

    }

    /**
     * method called when remote Ice candidates are received for local peer to save and respond to
     * @param receivedCandidate -> Ice Candidate model containing remote's ice candidate information
     **/
    private fun onRemoteIceCandidate(receivedCandidate: OnIceCandidate) {
        val activeConnectionClient = getActiveSessionClient(receivedCandidate.sessionUUID)

        val peerConnection: PeerConnection? =
            if (activeConnectionClient?.callParams?.refId == receivedCandidate.referenceID) {
                activeConnectionClient.peerConnectionClient.pc
            } else {
                activeConnectionClient?.m2MPeerConnectionClients?.get(receivedCandidate.referenceID)?.pc
            }
        when (peerConnection?.signalingState()) {
            PeerConnection.SignalingState.CLOSED -> Log.e(
                "saveIceCandidate Error",
                "PeerConnection object is closed"
            )
            PeerConnection.SignalingState.STABLE -> {
                if (peerConnection.remoteDescription != null) {
                    peerConnection.addIceCandidate(receivedCandidate.candidate)
                } else {
                    getActiveSessionClient(receivedCandidate.sessionUUID)?.peerConnectionClient?.iceCandidatesList?.add(
                        receivedCandidate.candidate
                    )
                }
            }
            else -> getActiveSessionClient(receivedCandidate.sessionUUID)?.peerConnectionClient?.iceCandidatesList?.add(
                receivedCandidate.candidate
            )
        }
    }

    /**
     * method to get the webrtc session information
     * @param sessionKey -> session key identifier for the session
     * @return webrtc connection client containing session configurations and data
     **/
    fun getActiveSessionClient(sessionKey: String) =
        sessions[sessionKey]// manage it in WebRTCClient

    /**
     * method to get the session key of the last active session
     * @return session Id of the last active session
     **/
    fun recentSession(): String? {
        if (sessions.isNotEmpty()) {
            return sessions.entries.last().key
        }
        return null
    }


    /**
     * method to register the user to the server for further communication
     * @param authToken -> auth token of the user got after login
     * @param refId -> user's reference id
     * @param reconnectStatus -> status is 1 if user is trying to reconnect after facing disconnection 0 otherwise
     **/
    fun register(authToken: String, refId: String, reconnectStatus: Int) {
        prefs.ownRefId = refId
        prefs.projectID?.let {
            sendMessage(
                RegisterCommand(
                    authorizationToken = authToken,
                    referenceID = refId,
                    projectID = it,
                    reConnectStatus = reconnectStatus
                )
            )
        }
    }

    /**
     * method to unregister user from the server
     * @param ownRefId -> user's own reference id
     **/
    fun unRegister(ownRefId: String) {
        prefs.projectID?.let { tenantID ->
            prefs.mcToken?.let { mcToken ->
                sendMessage(
                    UnRegisterCommand(ownRefId, tenantID, mcToken)
                )
            }
        }
        prefs.clearAll()
    }

    /**
     * method to check if the socket service is connected or not
     * @return is socket service connected?
     **/
    fun isConnected(): Boolean {
        return socketService?.let {
            it.isConnected()
        } ?: kotlin.run {
            false
        }
    }

    /**
     * method to send the message to the server
     * @param message -> class of JSON format to be sent
     **/
    private fun sendMessage(message: CommandBase) {
        socketService?.let {
            if (it.isConnected()) {
                Log.e("Socket Command ", message.compile())
                it.sendMessage(message.compile())
            } else {
                Log.e("Connection error ", "Closed")
            }
        }
    }

    /**
     * method to mute or unMute the microphone of a session
     * @param refId -> user's own reference id
     * @param sessionKey -> session key identifier for the session
     **/
    fun muteUnMuteMic(refId: String, sessionKey: String) {
        val session = sessions[sessionKey]

        prefs.projectID?.let { tenantID ->
            prefs.mcToken?.let { mcToken ->
                sendMessage(
                    AudioVideoStateInformationCommand(
                        sessionKey,
                        mcToken,
                        refId,
                        tenantID,
                        if (isAudioEnabled(sessionKey)) 0 else 1,
                        if (isVideoEnabled(sessionKey)) 1 else 0
                    )
                )
            }
        }

        if (session?.getSessionType() == SessionType.CALL)
            session.peerConnectionClient.disableEnableMic()
        else
            session?.peerConnectionClient?.disableEnableInternalAudio()
    }

    /**
     * method to only mute and unMute an active session
     * @param sessionKey session key to perform actions on a specific session
     * */
    @Deprecated(
        "this method is not to be used as it is of only general purpose usage" +
                "and will not notify the other participants of the session!"
    )
    fun muteUnMuteMic(sessionKey: String) {
        val session = sessions[sessionKey]

        if (session?.getSessionType() == SessionType.CALL)
            session.peerConnectionClient.disableEnableMic()
        else
            session?.peerConnectionClient?.disableEnableInternalAudio()
    }

    /**
     * method to check if the session's audio is enabled or not
     * @param sessionKey -> session key identifier for the session
     * @return the current enabled status of the session's audio
     **/
    fun isAudioEnabled(sessionKey: String): Boolean {
        val session = sessions[sessionKey]
        session?.peerConnectionClient?.let { pc ->
            return if (session.getSessionType() == SessionType.SCREEN) {
                pc.isInternalAudioEnabled()
            } else {
                pc.isAudioEnabled()
            }
        } ?: kotlin.run {
            return false
        }
    }

    /**
     * method to check if the session's video is enabled or not
     * @param sessionKey -> session key identifier for the session
     * @return the current enabled status of the session's video
     **/
    fun isVideoEnabled(sessionKey: String): Boolean {
        val session = sessions[sessionKey]
        session?.peerConnectionClient?.let { pc ->
            return if (session.getSessionType() == SessionType.SCREEN) {
                pc.isScreenCapturerEnabled()
            } else {
                pc.isVideoCapturerEnabled()
            }
        } ?: kotlin.run {
            return false
        }
    }

    /**
     * method to switch the active session's camera from front to back and vice versa
     * @param sessionKey -> session key identifier for the session
     **/
    fun switchCamera(sessionKey: String) {
        val session = sessions[sessionKey]
        session?.let {
            if (it.getSessionType() == SessionType.CALL && it.getSessionMediaType() == MediaType.VIDEO)
                it.peerConnectionClient.switchCamera()
        }
    }

    /**
     * method to toggle speaker on/off
     **/
    @Deprecated("This function is marked deprecated due to irregular states for speaker use modified enable/disable speaker functions")
    fun toggleSpeakerOnOff() {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.let {
            it.isSpeakerphoneOn = !it.isSpeakerphoneOn
        }
    }

    /**
     * method to check if the speaker is enabled or not
     * @return the current enabled status of the speaker
     **/
    fun isSpeakerEnabled(): Boolean {
        audioManager?.let {
            return it.isSpeakerphoneOn
        } ?: kotlin.run {
            return false
        }
    }

    /**
     * method to set toggle the speaker on/off
     * @param turnSpeakerOn -> boolean to switch speaker on and off
     **/
    fun setSpeakerEnable(turnSpeakerOn: Boolean) {
        audioManager?.let {
            it.isSpeakerphoneOn = turnSpeakerOn
        }
    }

    /**
     * method to pause a session's video stream
     * @param refId -> user's own reference Id
     * @param sessionKey -> session key identifier for the session
     **/
    fun pauseVideo(
        refId: String, sessionKey: String
    ) {
        val session = sessions[sessionKey]

        if (session?.getSessionType() == SessionType.CALL)
            session.peerConnectionClient.pauseVideo()
        else
            session?.peerConnectionClient?.pauseSSVideo()

        prefs.projectID?.let { tenantID ->
            prefs.mcToken?.let { mcToken ->
                sendMessage(
                    AudioVideoStateInformationCommand(
                        sessionKey,
                        mcToken,
                        refId,
                        tenantID,
                        if (isAudioEnabled(sessionKey)) 1 else 0,
                        0
                    )
                )
            }
        }
    }

    /**
     * method to resume a session's video stream
     * @param refId -> user's own reference Id
     * @param sessionKey -> session key identifier for the session
     **/
    fun resumeVideo(
        refId: String, sessionKey: String
    ) {
        val session = sessions[sessionKey]
        if (session?.getSessionType() == SessionType.CALL)
            session.peerConnectionClient.resumeVideo()
        else
            session?.peerConnectionClient?.resumeSSVideo()

        prefs.projectID?.let { tenantID ->
            prefs.mcToken?.let { mcToken ->
                sendMessage(
                    AudioVideoStateInformationCommand(
                        sessionKey,
                        mcToken,
                        refId,
                        tenantID,
                        if (isAudioEnabled(sessionKey)) 1 else 0,
                        1
                    )
                )
            }
        }
    }

    /**
     * method to make a fake socket factory for connection testing
     * @return returns a SocketFactory object to be passed during socket connection
     **/
    private fun fakeSocketFactory(): SocketFactory? {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {

            }

            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls(0)
            }

        }
        var sslContext: SSLContext? = null
        try {
            sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(trustManager), SecureRandom())
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return sslContext?.socketFactory
    }

    /**
     * method to initiate the reInvite procedure of the ongoing sessions
     **/
    fun initiateReInviteProcess() {
        if (sessions.isNotEmpty()) {
            sessions.forEach { mapValue ->
                initReInviteClient(mapValue.value.callParams)
            }
        }
    }

    /**
     * method to make the webrtc client objects of the session's reInvite procedure
     * @param callParams -> parameters of the session to be made
     **/
    private fun initReInviteClient(
        callParams: CallParams
    ) {
        prefs.mcToken?.let { mcToken ->
            listener?.let { sdkCallbacks ->
                callParams.mcToken = mcToken
                callParams.isReInvite = true

                if (callParams.sessionType == SessionType.CALL) {
                    val session = WebRtcClient(
                        socketService = socketService,
                        audioRecorder = getMicRecorder(),
                        context = context,
                        intent = mediaIntent,
                        isInitiator = callParams.isInitiator,
                        callParams = callParams,
                        streamCallback = sdkCallbacks
                    )
                    sessions[callParams.sessionUUID] = session
                } else if (callParams.sessionType == SessionType.SCREEN) {
                    val session = WebRtcClient(
                        socketService = socketService,
                        audioRecorder = getSSRecorder(callParams.isAppAudio, mediaProjection),
                        context = context,
                        intent = mediaIntent,
                        isInitiator = callParams.isInitiator,
                        callParams = callParams,
                        streamCallback = sdkCallbacks
                    )
                    sessions[callParams.sessionUUID] = session
                }
            }
        }
    }

    /**
     * method to initiate screen share procedure
     * @param mediaProjectionResult -> media projection intent data for screen share
     * @param resultCode -> result code of the intent
     * @param context -> context from the activity
     * @param isInternalAudioIncluded -> boolean if user enabled internal audio sharing or not
     **/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun initSession(
        mediaProjectionResult: Intent?,
        resultCode: Int,
        context: Context,
        isInternalAudioIncluded: Boolean
    ) {
        this.isAppAudioEnabled = isInternalAudioIncluded
        if (this.mediaProjection == null) {
            var mediaProjection: MediaProjection? = null

            if (isInternalAudioIncluded and (Build.VERSION.SDK_INT > 28)) {
                val mediaProjectionManager =
                    context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = mediaProjectionResult?.let {
                    mediaProjectionManager.getMediaProjection(
                        resultCode,
                        it
                    )
                }
            }

            saveMediaProjection(mediaProjection, mediaProjectionResult)
            listener?.onSessionReady(mediaProjection)
        } else {
            listener?.onSessionReady(this.mediaProjection)
        }
    }

    /**
     * method to start a screen share session
     * @param callParams -> session parameters
     * @param mediaProjection -> media projection got from the media projection intent
     * @return returns a session Id
     **/
    fun startSession(
        callParams: CallParams,
        mediaProjection: MediaProjection?
    ): String? {
        return if (checkPermissionGranted(callParams)) {
            prefs.projectID?.let { tenantId ->
                val sessionKey =
                    createMD5(System.currentTimeMillis().toString() + tenantId + callParams.refId)
                callParams.sessionUUID = sessionKey.toString()

                return initWebClientForSS(
                    callParams = callParams.apply {
                        isInitiator = true
                        isAppAudio = isAppAudioEnabled
                    },
                    mediaProjection = mediaProjection,
                    isMultiSession = callParams.isBroadcast == 1
                )
            } ?: kotlin.run {
                return null
            }
        } else null
    }

    /**
     * method to start a one2one call session
     * @param callParams -> session parameters
     * @return returns a session Id
     **/
    fun dialOne2OneCall(
        callParams: CallParams
    ): String? {
        return if (checkPermissionGranted(callParams)) {
            initWebClient(
                callParams = callParams.apply { isInitiator = true },
                isMultiSession = false,
            )
        } else null
    }

    /**
     * method to start a one2many call session
     * @param callParams -> session parameters
     * @return returns a session Id
     **/
    fun dialOne2ManyCall(
        callParams: CallParams
    ): String? {
        return if (checkPermissionGranted(callParams)) {
            prefs.projectID?.let { tenantId ->
                val sessionKey =
                    createMD5(
                        System.currentTimeMillis().toString() + tenantId + callParams.refId
                    )
                callParams.sessionUUID = sessionKey.toString()

                return initWebClient(
                    callParams = callParams.apply { isInitiator = true },
                    isMultiSession = callParams.isBroadcast == 1
                )
            } ?: kotlin.run {
                return null
            }
        } else null
    }

    /**
     * method to start a many2many call session
     * @param callParams -> session parameters
     * @return returns a session Id
     **/
    fun dialMany2ManyCall(
        callParams: CallParams
    ): String? {
        return if (checkPermissionGranted(callParams)) {
            initWebClient(
                callParams = callParams.apply { isInitiator = true },
                isMultiSession = false
            )
        } else null
    }


    var sessionUUID = ""
    var associatedSessionUUID = ""

    /**
     * method to form a pair of session keys for a multi session (video call + screen share)
     * @param refId -> user's own reference Id
     **/
    private fun makePublicBroadcastKeys(refId: String) {
        prefs.projectID?.let { tenantId ->
            sessionUUID =
                createMD5(System.currentTimeMillis().toString() + tenantId + refId).toString()

            associatedSessionUUID =
                createMD5(
                    System.currentTimeMillis().plus(999).toString() + tenantId + refId
                ).toString()
        }
    }

    /**
     * method to start a multi session
     * @param callParams -> session parameters
     * @param mediaProjection -> media projection got from the media projection intent
     * @param isGroupSession -> boolean to check if this multi session is a public broadcast or group broadcast
     * @return returns a pair of session Ids of the two created sessions
     **/
    @Deprecated(
        "this method is not to be used unless some UI related delay is present initiating a dual session " +
                "startMultiSessionV2() instead"
    )
    fun startMultiSession(
        callParams: CallParams,
        mediaProjection: MediaProjection?,
        isGroupSession: Boolean
    ): Pair<String, String>? {
//        these are a copy for checking permissions as in multi session we need only check against video call session.
        val permissionParams = callParams.copy()
        permissionParams.sessionType = SessionType.CALL
        permissionParams.mediaType = MediaType.VIDEO

        return if (checkPermissionGranted(permissionParams)) {
            if (isGroupSession) callParams.isBroadcast = 0 else callParams.isBroadcast = 1
            makePublicBroadcastKeys(callParams.refId)

            initVideoCallMultiSession(callParams.copy())

            Handler(Looper.getMainLooper()).postDelayed({
                initScreenShareMultiSession(callParams.copy(), mediaProjection)
            }, 5000)

            return Pair(sessionUUID, associatedSessionUUID)
        } else null

    }

    /**
     * method to start a multi session
     * @param callParams -> session parameters
     * @param mediaProjection -> media projection got from the media projection intent
     * @param isGroupSession -> boolean to check if this multi session is a public broadcast or group broadcast
     * @return returns a pair of session Ids of the two created sessions
     **/
    fun startMultiSessionV2(
        callParams: CallParams,
        mediaProjection: MediaProjection?,
        isGroupSession: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            initMultiSession(callParams, mediaProjection, isGroupSession)
            listener?.multiSessionCreated(Pair(sessionUUID, associatedSessionUUID))
        }
    }

    private suspend fun initMultiSession(
        callParams: CallParams,
        mediaProjection: MediaProjection?,
        isGroupSession: Boolean
    ) {
        val permissionParams = callParams.copy()
        permissionParams.sessionType = SessionType.CALL
        permissionParams.mediaType = MediaType.VIDEO

        if (checkPermissionGranted(permissionParams)) {
            if (isGroupSession) callParams.isBroadcast = 0 else callParams.isBroadcast = 1
            makePublicBroadcastKeys(callParams.refId)

            val videoSession = CoroutineScope(Dispatchers.IO).launch {
                initVideoCallMultiSession(callParams.copy())
            }
            val ssSession = CoroutineScope(Dispatchers.IO).launch {
                delay(1000L)
                initScreenShareMultiSession(callParams.copy(), mediaProjection)
            }

            val downloadJobs = listOf(
                videoSession,
                ssSession
            )
            downloadJobs.joinAll()
        }
    }

    /**
     * method to init video call for mutli session usage
     * @param callParams -> session parameters
     * @return returns a session Id
     **/
    private fun initVideoCallMultiSession(callParams: CallParams): String? {
        callParams.callType = CallType.ONE_TO_MANY
        callParams.mediaType = MediaType.VIDEO
        callParams.sessionType = SessionType.CALL
        callParams.isInitiator = true
        callParams.sessionUUID = sessionUUID
        callParams.associatedSessionUUID = associatedSessionUUID
        return initWebClient(callParams, true)
    }

    /**
     * method to init screen share for mutli session usage
     * @param callParams -> session parameters
     * @param mediaProjection -> media projection got from the media projection intent
     * @return returns a session Id
     **/
    private fun initScreenShareMultiSession(
        callParams: CallParams,
        mediaProjection: MediaProjection?
    ): String? {
        callParams.callType = CallType.ONE_TO_MANY
        callParams.mediaType = MediaType.VIDEO
        callParams.sessionType = SessionType.SCREEN
        callParams.isInitiator = true
        callParams.sessionUUID = associatedSessionUUID
        callParams.associatedSessionUUID = sessionUUID
        callParams.isAppAudio = this.isAppAudioEnabled
        return initWebClientForSS(callParams, mediaProjection, true)
    }

    /**
     * method to init webrtc client for calling sessions
     * @param callParams -> session parameters
     * @param isMultiSession -> boolean to check if the session to be made is a multi session
     * @return returns a session Id
     **/
    private fun initWebClient(
        callParams: CallParams,
        isMultiSession: Boolean
    ): String? {
        prefs.projectID.let { tenantId ->
            prefs.mcToken?.let { mcToken ->
                listener?.let { sdkCallbacks ->
                    val sessionKey =
                        createMD5(
                            System.currentTimeMillis().toString() + tenantId + callParams.refId
                        )

                    callParams.sessionUUID =
                        if (!isMultiSession) sessionKey.toString() else callParams.sessionUUID
                    callParams.mcToken = mcToken

                    val session = WebRtcClient(
                        socketService = socketService,
                        audioRecorder = getMicRecorder(),
                        context = context,
                        intent = mediaIntent,
                        callParams = callParams,
                        streamCallback = sdkCallbacks
                    )
                    return if (!isMultiSession) {
                        sessions[sessionKey.toString()] = session
                        sessionKey.toString()
                    } else {
                        sessions[callParams.sessionUUID] = session
                        callParams.sessionUUID
                    }
                }
            } ?: kotlin.run {
                return null
            }
        }
    }

    /**
     * method to init webrtc client for screen share sessions
     * @param callParams -> session parameters
     * @param mediaProjection -> media projection got from the media projection intent
     * @param isMultiSession -> boolean to check if the session to be made is a multi session
     * @return returns a session Id
     **/
    private fun initWebClientForSS(
        callParams: CallParams,
        mediaProjection: MediaProjection?,
        isMultiSession: Boolean
    ): String? {
        prefs.projectID.let { tenantId ->
            prefs.mcToken?.let { mcToken ->
                listener?.let { sdkCallbacks ->
                    val sessionKey =
                        createMD5(
                            System.currentTimeMillis().toString() + tenantId + callParams.refId
                        )

                    callParams.sessionUUID =
                        if (!isMultiSession) sessionKey.toString() else callParams.sessionUUID
                    callParams.mcToken = mcToken

                    val session = WebRtcClient(
                        socketService = socketService,
                        audioRecorder = getSSRecorder(callParams.isAppAudio, mediaProjection),
                        context = context,
                        intent = mediaIntent,
                        callParams = callParams,
                        streamCallback = sdkCallbacks
                    )
                    isAppAudioEnabled = false
                    setSpeakerEnable(true)
                    return if (!isMultiSession) {
                        sessions[sessionKey.toString()] = session
                        sessionKey.toString()
                    } else {
                        sessions[callParams.sessionUUID] = session
                        callParams.sessionUUID
                    }
                }
            } ?: kotlin.run {
                return null
            }
        }
    }

    /**
     * method to init microphone recorder of the device for the session
     * @return returns an audio record instance
     **/
    private fun getMicRecorder(): AudioRecord {
        return createMicRecorder(
            AUDIO_SOURCE_MIC,
            SAMPLE_RATE_IN_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
    }

    /**
     * method to init microphone or internal audio recorder of the device for the session
     * @return returns an audio record instance
     **/
    private fun getSSRecorder(isAppAudio: Boolean, mediaProjection: MediaProjection?): AudioRecord {
        return if (isAppAudio && Build.VERSION.SDK_INT >= 29)
            createAppAudioRecorder(
                mediaProjection!!,
                SAMPLE_RATE_IN_HZ
            )
        else
            getMicRecorder()
    }

    /**
     * method to end one or many ongoing sessions
     * @param sessionIdList -> list of the session keys that need to be ended
     * @return returns an audio record instance
     **/
    fun endCallSession(sessionIdList: ArrayList<String>) {
        sessionIdList.forEach { sessionId ->
            prefs.projectID?.let { tenantId ->
                sessions[sessionId]?.let {
                    it.peerConnectionClient.resetCapturerVariables(it.getSessionType())
                    it.peerConnectionClient.stopOngoingCall(tenantId)
                }
            }
        }
    }

    /**
     * This function will be called upon a session end
     * -> If screen_share session then dispose of the audio/video tracks only if its the last screen sessions in the list
     * -> If audio_video_call session then dispose of the audio/video tracks only if its the last screen sessions in the list
     * @param sessionType -> the type of session i.e. CALL,SCREEN
     * */
    private fun stopCommunicationDevices(sessionType: SessionType) {
        if (sessionType == SessionType.SCREEN) {
            if (!getActiveExistingSession(sessionType)) {
                stopScreenCapturer()
                mediaProjection?.stop()
                mediaProjection = null
                mediaIntent = null
            }
        } else {
            if (!getActiveExistingSession(SessionType.CALL) || !getActiveExistingSession(MediaType.VIDEO)) {
                stopCameraCapturer()
            }
            if (!getActiveExistingSession(MediaType.AUDIO) && !getActiveExistingSession(MediaType.VIDEO)) {
                stopAudioTrack()
            }
        }

        if (!getActiveExistingSession(SessionType.CALL) && !getActiveExistingSession(SessionType.SCREEN))
            if (isSpeakerEnabled()) setSpeakerEnable(false)
    }

    /**
     * method used to search if any session exists inside activeSessionMap with the given MEDIA_TYPE
     * @param mediaType -> media type of the session i.e. AUDIO,VIDEO
     * @return returns true if session exists inside map else false
     **/
    fun getActiveExistingSession(mediaType: MediaType): Boolean {
        return sessions.any { it.value.getSessionMediaType() == mediaType }
    }

    /**
     * method used to search if any session exists inside activeSessionMap with the given SESSION_TYPE
     * @param sessionType -> media type of the session i.e. CALL,SCREEN
     * @return returns true if session exists inside map else false
     **/
    fun getActiveExistingSession(sessionType: SessionType): Boolean {
        return sessions.any { it.value.getSessionType() == sessionType }
    }

    /**
     * method used to accept an incoming session request from the initiator, this method is for the receiver end
     * @param ownRefId -> receiver's reference Id
     * @param callParams -> session parameters
     * @return returns a session Id
     **/
    fun acceptIncomingCall(
        ownRefId: String,
        callParams: CallParams
    ): String {
        prefs.mcToken?.let { mcToken ->
            listener?.let { sdkCallbacks ->
                callParams.toRefIds = arrayListOf(callParams.refId)
                callParams.refId = ownRefId
                callParams.mcToken = mcToken
                callParams.isInitiator = false

                val session = WebRtcClient(
                    socketService = socketService,
                    audioRecorder = createMicRecorder(
                        AUDIO_SOURCE_MIC,
                        SAMPLE_RATE_IN_HZ,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT
                    ),
                    context = context,
                    intent = mediaIntent,
                    isInitiator = callParams.isInitiator,
                    callParams = callParams,
                    streamCallback = sdkCallbacks
                )
                if (callParams.sessionType == SessionType.SCREEN) {
                    setSpeakerEnable(true)
                }
                sessions[callParams.sessionUUID] = session
                if (checkPermissionGranted(null)) startWebRtcSessionLogging(callParams.sessionUUID)
                return callParams.sessionUUID
            }
        } ?: kotlin.run {
            return ""
        }
    }


    /**
     * method used to reject an incoming session request from the initiator, this method is for the receiver end
     * @param refId -> receiver's reference Id
     * @param sessionUUID -> incoming session's Id
     **/
    fun rejectIncomingCall(refId: String, sessionUUID: String) {
        prefs.projectID?.let { tenantId ->
            sendMessage(
                RejectCallCommand(
                    refId,
                    sessionUUID,
                    tenantId
                )
            )
        }
    }

    /**
     * method used to send a timeOut to an incoming session from the initiator,
     * this method is for the receiver end and can be called after a time interval of an incoming session
     * @param refId -> receiver's reference Id
     * @param sessionUUID -> incoming session's Id
     **/
    fun callTimeout(refId: String, sessionUUID: String) {
        prefs.projectID?.let { tenantId ->
            prefs.mcToken?.let { mcToken ->
                sendMessage(
                    CallTimeOutCommand(
                        mcToken,
                        sessionUUID,
                        refId,
                        tenantId
                    )
                )
            }
        }
    }

    /**
     * method used to send a BUSY response to an incoming session from the initiator,
     * this method is for the receiver end and can be called if the receiver is currently
     * in a session and a new session request arrives
     * @param refId -> receiver's reference Id
     **/
    fun sessionBusy(refId: String, sessionUUID: String) {
        prefs.projectID?.let { tenantId ->
            prefs.mcToken?.let { mcToken ->
                sendMessage(
                    SessionBusyCommand(
                        mcToken,
                        sessionUUID,
                        refId,
                        tenantId
                    )
                )
            }
        }
    }

    /**
     * method used to send the logging data of the session when the session ends
     * @param refId -> receiver's reference Id
     * @param sessionKey -> session key of the session that ended
     * @param stats -> "Any" type object that will be converted to a JSON object to send in the command
     **/
    fun sendEndCallLogs(refId: String, sessionKey: String, stats: Any) {
        prefs.projectID?.let { tenantId ->
            prefs.mcToken?.let { mcToken ->
                sendMessage(
                    CallLogCommand(
                        mcToken = mcToken,
                        projectID = tenantId,
                        referenceId = refId,
                        sessionUUID = sessionKey,
                        stats = stats
                    )
                )
            }
        }
    }

    /**
     * method used to start the data logging for a session
     * @param sessionKey -> session key of the session that ended
     **/
    private fun startWebRtcSessionLogging(sessionKey: String) {
        val session: WebRtcClient? = getActiveSessionClient(sessionKey)

        webRtcStatsLogger.startWebRTCSessionLogging(session)
//        when(session?.getCallType()) {
//
//            CallType.ONE_TO_ONE -> {
//                webRtcStatsLogger.startOne2OneWebRtcLogging(session)
//            }
//
//            CallType.MANY_TO_MANY -> {
//                webRtcStatsLogger.startMany2ManyWebRtcLogging(session)
//            }
//
//            CallType.ONE_TO_MANY -> {
//                webRtcStatsLogger.startOne2ManyWebRtcLogging(session)
//            }
//
//            CallType.DEFAULT -> {}
//
//        }
    }

    /**
     * method used to disConnect the existing active socket service
     **/
    fun disConnectSocket() {
        val thread = Thread {
            try {
                socketService?.disconnect()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
    }

    /**
     * method used to set the listeners for the session callbacks from the CallSDK
     * @param listenerSocketCallBacks -> interface to be implemented and listened from the application level and passed here
     **/
    fun setListener(listenerSocketCallBacks: CallSDKListener) {
        this.listener = listenerSocketCallBacks
        webRtcStatsLogger.setListener(listenerSocketCallBacks)

    }

    /**
     * method used to remove the listeners for the session callbacks from the CallSDK
     **/
    fun removeListener() {
        this.listener = null
        webRtcStatsLogger.removeListener()
    }

    /**
     * method used to set the media projection locally for further use while initiating and removing
     * screen share session
     * @param mediaProjection -> media projection got from the media projection intent
     * @param intent -> media projection intent
     **/
    private fun saveMediaProjection(mediaProjection: MediaProjection?, intent: Intent?) {
        this.mediaProjection = mediaProjection
        this.mediaIntent = intent
    }

    /**
     * method used to set and store the PROJECT_ID in prefs for further
     * many functions in the StreamingSDK won't work if PROJECT_ID is not provided on start
     * @param project_id -> project_id of the forming application to run the StreamingSDK
     **/
    fun setConstants(project_id: String) {
        prefs.projectID = project_id
    }

    /**
     * method used to check required permissions availability before initiating a session
     * NOTE: This function only check permissions not asks
     * @param callParams -> session parameters
     * @return returns true if permissions granted false otherwise
     **/
    private fun checkPermissionGranted(callParams: CallParams? = null): Boolean {
        callParams?.let { params ->
            when (params.sessionType) {
                SessionType.CALL -> {
                    when (params.mediaType) {
                        MediaType.AUDIO -> {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                return true
                            } else {
                                listener?.permissionError(arrayListOf(PermissionType.MIC_PERMISSION))
                            }
                        }
                        MediaType.VIDEO -> {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) ==
                                PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                return true
                            } else {
                                listener?.permissionError(
                                    arrayListOf(
                                        PermissionType.MIC_PERMISSION,
                                        PermissionType.CAMERA_PERMISSION
                                    )
                                )
                            }
                        }
                        else -> {
                        }
                    }
                }
                SessionType.SCREEN -> {
                    when (params.mediaType) {
                        MediaType.VIDEO -> {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) ==
                                PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                return true
                            } else {
                                listener?.permissionError(
                                    arrayListOf(
                                        PermissionType.MIC_PERMISSION,
                                        PermissionType.CAMERA_PERMISSION
                                    )
                                )
                            }
                        }
                        else -> {
                        }
                    }
                }
                else -> {
                }
            }
        } ?: kotlin.run {
//            this will check network permissions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                return true
            } else {
                listener?.permissionError(
                    arrayListOf(
                        PermissionType.WIFI_STATE_PERMISSION
                    )
                )
            }
        }

        return false
    }

    companion object : SingletonHolder<CallClient, Context>(::CallClient)

}


open class SingletonHolder<out T : Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator

    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T? {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator?.let { it(arg) }
                instance = created
                creator = null
                created
            }
        }
    }
}
