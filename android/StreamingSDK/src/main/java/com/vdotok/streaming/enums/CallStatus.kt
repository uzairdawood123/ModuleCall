package com.vdotok.streaming.enums


/**
 * Created By: VdoTok
 * Date & Time: On 6/15/21 At 4:00 PM in 2021
 */
enum class CallStatus(value: String) {

    /* Is triggered to the initiator when searching the receiver */
    TRYING_TO_LOCATE_CONNECT_ALERTING_TARGET("trying to locate, connect and alerting target"),

    /* Is triggered to the initiator when the receiver does not accept session after a timeout */
    NO_ANSWER_FROM_TARGET("no answer from target"),

    /* Is triggered to the initiator when the receiver not found */
    TARGET_NOT_FOUND("target not found"),

    /* Is triggered to the initiator when the receiver is busy */
    TARGET_IS_BUSY("target is busy"),

    /* Is triggered on receiver side when call receiving timeout is reached */
    CALL_MISSED("call missed"),

    /* Is triggered when a participant in a session leaves */
    PARTICIPANT_LEFT_CALL("participant left call"),

    /* Is triggered when a participant in a session leaves */
    NEW_PARTICIPANT_ARRIVED("new participant arrived"),

    /* Is triggered when a participant in a session leaves */
    EXISTING_PARTICIPANTS("existing participants"),

    /* Is triggered on initiator side when receiver of a session rejects an incoming session */
    CALL_REJECTED("call rejected"),

    /* Is triggered when a session is connected */
    CALL_CONNECTED("call connected"),

    /* Is triggered when a session cannot be created because no balance exists */
    INSUFFICIENT_BALANCE("insufficient balance"),

    /* Is triggered when a session cannot be precessed due to service suspension from the admin */
    SERVICE_SUSPENDED("service suspended"),

    /* Is triggered when there a server timeout is triggered while connecting a session */
    SESSION_TIMEOUT("session timeout"),

    /* Is triggered when a respective session is busy */
    SESSION_BUSY("session busy"),

    /* Is triggered when call is not connected */
    CALL_NOT_CONNECTED("call not connected"),

    /* Is triggered when session is cancelled */
    OUTGOING_CALL_ENDED("outgoing call ended"),

    /* Is triggered when user tries to cancel a session that does not exist */
    NO_SESSION_EXISTS("no session exists"),

    /* Is triggered when session is started with an invalid user info */
    INVALID_AUTHENTICATION("invalid authentication"),

    /* Is triggered when a malformed RPC or incorrect params in the rpc is sent to the server */
    BAD_REQUEST("bad request");

    var value: String
        internal set

    init {
        this.value = value
    }
}