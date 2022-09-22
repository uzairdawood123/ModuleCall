package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 6/15/21 At 2:22 PM in 2021
 */
data class RejectCallCommand(
    var fromRefId: String, // refID of the receiver
    var sessionUUID: String,
    var projectID: String,
): CommandBase() {

    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("type", "response")
        json.put("from", fromRefId)
        json.put("requestType", RequestType.SESSION_INVITE.value)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + fromRefId)
        )
        json.put("sessionUUID", sessionUUID)
        json.put("responseCode", 496)
        json.put("responseMessage", "rejected")
        return json.toString()
    }

}