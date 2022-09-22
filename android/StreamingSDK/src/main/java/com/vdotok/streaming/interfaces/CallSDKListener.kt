package com.vdotok.streaming.interfaces

import android.media.projection.MediaProjection
import com.vdotok.streaming.commands.CallInfoResponse
import com.vdotok.streaming.commands.RegisterResponse
import com.vdotok.streaming.enums.EnumConnectionStatus
import com.vdotok.streaming.enums.PermissionType
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo
import com.vdotok.streaming.stats.StatsInterface

/**
 * Created By: VdoTok
 * Date & Time: On 8/4/21 At 11:01 AM in 2021
 *
 * Main interface for the Call Sdk callbacks used for connection and
 * call related callbacks.
 * Inherits StreamCallback for stream callbacks
 * Inherits StatsInterface for call logging callbacks
 * */

interface CallSDKListener: StreamCallback, StatsInterface {
    fun connectionStatus(enumConnectionStatus: EnumConnectionStatus)
    fun onClose(reason: String)
    fun audioVideoState(sessionStateInfo: SessionStateInfo)
    fun incomingCall(callParams: CallParams)
    fun onSessionReady(mediaProjection: MediaProjection?)
    fun callStatus(callInfoResponse: CallInfoResponse)
    fun registrationStatus(registerResponse: RegisterResponse)
    fun onError(cause: String)
    fun onPublicURL(publicURL: String)
    fun sessionHold(sessionUUID: String)
    fun multiSessionCreated(sessionIds: Pair<String, String>)
    fun permissionError(permissionErrorList: ArrayList<PermissionType>) {}
}