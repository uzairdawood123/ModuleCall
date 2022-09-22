package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.jvm.Throws

data class RegisterCommand(
    val authorizationToken: String,
    val referenceID: String,
    val projectID: String,
    val reConnectStatus: Int = 0
) : CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        json.put("type", "request")
        json.put("projectID", projectID)
        json.put("requestType", RequestType.REGISTER.value)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceID)
        )
        json.put("referenceID", referenceID)
        json.put("authorizationToken", authorizationToken)
        json.put("reConnect", reConnectStatus)
        return json.toString()
    }
}