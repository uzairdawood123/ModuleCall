package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 6/17/21 At 4:06 PM in 2021
 */
data class PongCommand(
    var mcToken: String
): CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("requestType", RequestType.PONG.value)
        json.put("mcToken", mcToken)
        return json.toString()
    }
}