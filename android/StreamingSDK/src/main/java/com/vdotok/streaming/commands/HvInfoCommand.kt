package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


data class HvInfoCommand(
    private val projectID: String,
    private val storeInfo: String, // 1 if info is to be saved for the session on server otherwise 0
    private val callParams: CallParams
) :

    CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("requestType", RequestType.HV_INFO.value)
        json.put("type", "request")
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + callParams.refId)
        )
        json.put("sessionUUID", callParams.sessionUUID)
        json.put("referenceID", callParams.refId)
        json.put("store", storeInfo)
        json.put("mcToken", callParams.mcToken)
        callParams.customDataPacket?.let {
            json.put("data", JSONObject(it))
        }
        return json.toString()
    }
}
