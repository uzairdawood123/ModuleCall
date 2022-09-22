package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.jvm.Throws


/**
 * Created By: Norgic
 * Date & Time: On 9/14/21 At 4:42 PM in 2021
 */
data class ReInviteCommand(
    val referenceID: String,
    val mcToken: String,
    val sessionUUID: String,
    val sdpOffer: String,
    val isConnected: Int = 0,
    private val projectID: String
) : CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("type", "request")
        json.put("requestType", RequestType.RE_INVITE.value)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceID)
        )
        json.put("referenceID", referenceID)
        json.put("sessionUUID", sessionUUID)
        json.put("sdpOffer", sdpOffer)
        json.put("mcToken", mcToken)
        json.put("isConnected", isConnected)
        return json.toString()
    }
}