package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


data class AcceptCallCommand(
    private val from: String,
    private val sdpOffer: String,
    private val projectID: String,
    private val callParams: CallParams
) :

    CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("type", "request")
        json.put("requestType", RequestType.SESSION_INVITE.value)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + from)
        )
        json.put("sessionUUID", callParams.sessionUUID)
        json.put("responseCode", 200)
        json.put("responseMessage", "accepted")
        json.put("sdpOffer", sdpOffer)
        json.put("isRecord", callParams.isRecord)
        json.put("mcToken", callParams.mcToken)
        return json.toString()
    }
}