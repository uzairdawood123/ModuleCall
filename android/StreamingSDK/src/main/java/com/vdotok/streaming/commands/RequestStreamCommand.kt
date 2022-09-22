package com.vdotok.streaming.commands

import android.os.Parcelable
import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


@Parcelize
data class RequestStreamCommand(
    private val from: String,
    private val to: String,
    private val mcToken: String,
    private val sessionUUID: String,
    private val referenceId: String,
    private val sdpOffer: String,
    private val projectID: String
) :
    Parcelable,
    CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val json = JSONObject()
        // Own Ref ID (Logged in user Ref ID)
        json.put("from", from)

        //Participant Ref id
        //you can get this id from remote participant peer connection class
        // with participant name or referenceID
        json.put("to", to)
        json.put("requestType", RequestType.TO_RECEIVE_STREAM.value)
        json.put("sdpOffer", sdpOffer)
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceId)
        )
        json.put("type", "request")
        json.put("sessionUUID", sessionUUID)
        json.put("mcToken", mcToken)
        return json.toString()
    }
}