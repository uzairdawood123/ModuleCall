package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 6/3/21 At 3:48 PM in 2021
 */
data class StopCallCommand(
    // Next Person RefID
    private val referenceId: String,
    // Current Session ID
    private val sessionId: String,
    private val mcToken: String?,
    private val projectID: String
) : CommandBase() {

    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("type", "request")
        json.put("requestType", RequestType.SESSION_CANCEL.value)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceId)
        )
        json.put("sessionUUID", sessionId)
        json.put("mcToken", mcToken)
        return json.toString()
    }
}