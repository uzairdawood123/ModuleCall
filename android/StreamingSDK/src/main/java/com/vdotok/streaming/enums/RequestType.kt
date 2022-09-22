package com.vdotok.streaming.enums

/**
 * Created By: VdoTok
 * Date & Time: On 5/25/21 At 6:30 PM in 2021
 */
enum class RequestType(value: String) {

    REGISTER("register"),
    UN_REGISTER("un_register"),
    PING("ping"),
    PONG("pong"),
    SESSION_INVITE("session_invite"),
    SESSION_TIMEOUT("session_timeout"),
    SESSION_BUSY("session_busy"),
    ON_ICE_CANDIDATE("onIceCandidate"),
    SESSION_CANCEL("session_cancel"),
    STATE_INFORMATION("state_information"),
    INCOMING_CALL_RESPONSE("incomingCallResponse"),
    NEW_PARTICIPANT_ARRIVED("new_participant_arrived"),
    TO_RECEIVE_STREAM("to_receive_stream"),
    STAT_INFORMATION("stats_information"),
    SDK_CRASH_REPORT("sdk_report"),
    RE_INVITE("re_invite"),
    HV_INFO("hv_info"),
    EXISTING_PARTICIPANTS("existing_participants");

    var value: String
        internal set

    init {
        this.value = value
    }
}
