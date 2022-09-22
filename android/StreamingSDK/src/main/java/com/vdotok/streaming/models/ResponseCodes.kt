package com.vdotok.streaming.models


/**
 * Created By: VdoTok
 * Date & Time: On 3/2/21 At 11:35 AM in 2021
 */
enum class ResponseCodes (value: String) {

//    register
    REQUEST_SUCCESS("200"),
    REQUEST_FAILURE("401"),

//    call
    LOCATING_TARGET("100"),
    TRYING_TO_CONNECT_TARGET("180"),
    TARGET_IS_ALERTING("183"),
    TARGET_NOT_FOUND("404"),
    INSUFFICIENT_BALANCE("402"),
    SERVICE_SUSPENDED("403"),
    SESSION_CANCELLED("487"),
    SESSION_CANCEL_SUCCESS("200"),
    NO_ANSWER_FROM_TARGET("408"),
    TARGET_BUSY("486"),
    PARTICIPANT_LEFT_CALL("410"),
    INVALID_AUTHENTICATION("407"),
    BAD_REQUEST("400"),
    CALL_REJECTED("496");

    var value: String
        internal set

    init {
        this.value = value
    }

}