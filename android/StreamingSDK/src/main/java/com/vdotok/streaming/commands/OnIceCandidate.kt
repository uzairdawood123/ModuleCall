package com.vdotok.streaming.commands

import com.vdotok.streaming.enums.RequestType
import org.json.JSONObject
import org.webrtc.IceCandidate

data class OnIceCandidate(
    val referenceID: String,
    val sessionUUID: String,
    val candidate: IceCandidate

) : CommandBase() {
    override fun compile(): String {
        val obj = JSONObject()
        obj.put("requestType", RequestType.ON_ICE_CANDIDATE.value)
        obj.put("type", "request")
        obj.put("referenceID", referenceID)
        obj.put("sessionUUID", sessionUUID)
        val candidateModel = JSONObject()
        candidateModel.put("candidate", candidate.sdp)
        candidateModel.put("sdpMid", candidate.sdpMid)
        candidateModel.put("sdpMLineIndex", candidate.sdpMLineIndex)
        obj.put("candidate", candidateModel)
        return obj.toString()
    }
}