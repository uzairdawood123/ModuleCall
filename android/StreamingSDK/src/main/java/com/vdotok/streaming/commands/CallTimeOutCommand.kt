package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 7/7/21 At 12:50 PM in 2021
 */
data class CallTimeOutCommand(
    private val mcToken: String,
    private val sessionUUID: String,
    private val referenceId: String,
    private val projectID: String,
) :

    CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("requestType", RequestType.SESSION_TIMEOUT.value)
        json.put("mcToken", mcToken)
        json.put("sessionUUID", sessionUUID)
        json.put("referenceID", referenceId)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceId)
        )
        return json.toString()
    }
}

