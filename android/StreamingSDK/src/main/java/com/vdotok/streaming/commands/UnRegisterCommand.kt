package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


data class UnRegisterCommand(
    private val referenceId: String,
    private val projectID: String,
    private val mcToken: String
) :

    CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("type", "request")
        json.put("requestType", RequestType.UN_REGISTER.value)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceId)
        )
        json.put("mcToken", mcToken)
        return json.toString()
    }
}