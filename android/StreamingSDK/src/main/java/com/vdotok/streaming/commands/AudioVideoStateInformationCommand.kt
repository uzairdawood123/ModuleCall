package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 6/10/21 At 12:06 PM in 2021
 */
data class AudioVideoStateInformationCommand(
    private val sessionId: String,
    private val mcToken: String?,
    private val refId: String?,
    private val projectID: String,
    private val audioState: Int?,
    private val videoState: Int?
) :
    CommandBase() {

    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("type", "request")
        json.put("requestType", RequestType.STATE_INFORMATION.value)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + refId)
        )
        json.put("sessionUUID", sessionId)
        json.put("mcToken", mcToken)
        json.put("referenceID", refId)
        json.put("audioInformation", audioState)
        json.put("videoInformation", videoState)

        return json.toString()
    }
}