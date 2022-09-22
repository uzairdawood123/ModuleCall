package com.vdotok.streaming.commands

import com.google.gson.Gson
import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import kotlinx.android.parcel.RawValue
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 7/16/21 At 7:13 PM in 2021
 */

data class CallLogCommand(
    private val mcToken: String,
    private val projectID: String,
    private val referenceId: String,
    private val sessionUUID: String,
    private val stats: @RawValue Any, // can be any class with "stats" as the key of the variable
) :

    CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val gson = Gson()
        val jsonObject = JSONObject(gson.toJson(stats))
        val json = JSONObject()
        json.put("mcToken", mcToken)
        json.put("referenceID", referenceId)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceId)
        )
        json.put("sessionUUID", sessionUUID)
        json.put("stats", jsonObject)
        json.put("type", "request")
        json.put("requestType", RequestType.STAT_INFORMATION.value)
        return json.toString()
    }
}
