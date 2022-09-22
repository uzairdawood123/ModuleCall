package com.vdotok.streaming.models

import com.google.gson.JsonObject
import com.vdotok.callsdk.models.idModel
import com.vdotok.streaming.enums.EnumCrashLogging

data class ServerResponse(
    var response: String,
    var sdpAnswer: String,
    var referenceID: String,
    var referenceIDs: ArrayList<String>,
    var candidate: CandidateModel,
    var message: String,
    var success: String,
    var from: String,
    var mcToken: String,
    var requestID: String,
    var responseCode: String,
    var call_type: String,
    var media_type: String,
    var session_type: String,
    var responseMessage: String,
    var type: String,
    var requestType: String,
    var sessionUUID: String,
    var associatedSessionUUID: String = "",
    var audioInformation: Int,
    var videoInformation: Int,
    var total_participants: Int,
    var reConnect: Int,
    var bytes_interval: Int, // this is in seconds
    var ping_interval: Int, // this is in seconds
    var url: String,
    var isLoggingEnable: Int = EnumCrashLogging.DISABLED.value,
    var data: JsonObject? = null,
    override var id: String?,
) : idModel() {

    fun getIdRes(): IdResponse? {
        return id?.let { IdResponse.getIdRes(it) }
    }

    fun getTypeRes(): TypeResponse? {
        return TypeResponse.getType(response)
    }

    fun getTypeResMsg(): TypeResponse? {
        return TypeResponse.getType(responseMessage)
    }


    fun getType(): IdResponse? {
        return IdResponse.getIdRes(type)
    }

}

