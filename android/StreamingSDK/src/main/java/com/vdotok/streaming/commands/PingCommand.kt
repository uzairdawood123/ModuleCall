package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 6/17/21 At 4:06 PM in 2021
 */
data class PingCommand(
    var mcToken: String,
    var projectID: String,
    var referenceId: String
): CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("mcToken", mcToken)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceId)
        )
        json.put("requestType", RequestType.PING.value)
        return json.toString()
    }
}