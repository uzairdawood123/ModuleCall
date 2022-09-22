package com.vdotok.streaming.models

enum class IdResponse(val id: String) {
    REQUEST("request"),
    RESPONSE("response"),

    REGISTER("register"),
    UN_REGISTER("un_register"),
    PING("ping"),
    PONG("pong"),

    PRESENTER_RESPONSE("presenterResponse"),
    ICE_CANDIDATE("iceCandidate"),
    ON_ICE_CANDIDATE("onIceCandidate"),
    VIEWER_RESPONSE("viewerResponse"),
    STOP_COMMUNICATION("session_cancel"),
    SESSION_INVITE("session_invite"),
    SESSION_TIMEOUT("session_timeout"),
    SESSION_REJECTED("session_rejected"),
    SESSION_BUSY("session_busy"),
    INVALID_RESPONSE("invalid target"),
    CLOSE_ROOM_RESPONSE("closeRoomResponse"),
    INCOMING_CALL("incomingCall"),
    START_COMMUNICATION("startCommunication"),
    CALL_RESPONSE("callResponse"),
    PUBLIC_URL("publicURL"),
    SDK_CRASH_REPORT("sdk_report"),
    INCOMING_CALL_RESPONSE("incomingCallResponse"),
    STATE_INFORMATION("state_information"),
    RE_INVITE("re_invite"),
    SESSION_BREAK("session_break"),
    NEW_PARTICIPANT_ARRIVED("new_participant_arrived"),
    EXISTING_PARTICIPANTS("existing_participants"),
    HV_INFO("hv_info"),
    UN_KNOWN("unknown"),
    REJECTED("rejected"),
    ACCEPTED("accepted");

    companion object {
        fun getIdRes(idRes: String?): IdResponse {
            for (idResponse in IdResponse.values()) {
                if (idRes == idResponse.id) {
                    return idResponse
                }
            }
            return UN_KNOWN
        }
    }
}
