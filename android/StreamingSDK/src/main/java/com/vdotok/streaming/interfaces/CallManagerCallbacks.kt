package com.vdotok.streaming.interfaces

import com.vdotok.streaming.enums.EnumAudioRoute
import com.vdotok.streaming.enums.EnumConnectionStatus
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.Credentials


/**
 * Created By: VdoTok
 * Date & Time: On 2/23/21 At 4:11 PM in 2021
 */
interface CallManagerCallbacks {

    fun connect(credentials: Credentials)

    fun connectionStatus(enumConnectionStatus: EnumConnectionStatus)

    fun registrationStatus(isRegister: Boolean, response: String)

    fun incomingCall(from: String)

    fun callConnectionStatus(isConnected: Boolean)

    fun sessionRejected(reason: String, accepted: Boolean)

    fun invalidResponse(message: String)

    fun responseMessages(message: String)

    // pre-defined
    fun disconnect()

    fun connectScreenShare(sessionParam: Int)

    fun endSession(sessionId: Int)

    fun endAllSession()

    fun screenShareSessionStatus(sessionId: Int)

    fun mute()

    fun unMute()

    fun isMuted(isMuted: Boolean)

    fun audioRoute(audioRoute: EnumAudioRoute)

    fun dialCall(callParams: CallParams)

    fun endCall()

    //    need confirmation 14.	setDeaultAudioInput
    fun setDefaultAudioInput()

    fun setDefaultCam()

    fun switchCam()

}