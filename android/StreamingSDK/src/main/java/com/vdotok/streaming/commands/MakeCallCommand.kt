package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.utils.createMD5
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


data class MakeCallCommand(
    private val to: ArrayList<String>,
    private val referenceId: String,
    private val sdpOffer: String,
    private val projectID: String,
    private val callParams: CallParams
) :

    CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        //Initiator Ref ID
        json.put("from", referenceId)
        //Array of Participants Ref ID or IDs
        // "[userA,userB,userC]"
        json.put("to", JSONArray(to))
        json.put("type", "request")
        json.put("requestType", RequestType.SESSION_INVITE.value)
        json.put("session_type", callParams.sessionType.value)
        json.put("call_type", callParams.callType.value)
        json.put("media_type", callParams.mediaType.value)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceId)
        )
        json.put("sessionUUID", callParams.sessionUUID)
        json.put("mcToken", callParams.mcToken)
        json.put("sdpOffer", sdpOffer)
        json.put("associatedSessionUUID", callParams.associatedSessionUUID) // will only be used if it is a multi session of a screen share and video call
        json.put("isRecord", callParams.isRecord) // will be 1 if peer needs to initiate a recording for the session.
        json.put("broadcastType", callParams.isBroadcast) // (0 means Group and 1 means Public Call),
        callParams.customDataPacket?.let {
            json.put("data", JSONObject(it))
        }
        return json.toString()
    }
}

