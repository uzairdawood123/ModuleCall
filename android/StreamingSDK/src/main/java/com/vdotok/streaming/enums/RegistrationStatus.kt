package com.vdotok.streaming.enums


/**
 * Created By: VdoTok
 * Date & Time: On 6/17/21 At 4:46 PM in 2021
 */
enum class RegistrationStatus(value: String) {

    REGISTER_SUCCESS("register success"),
    UN_REGISTER("un register"),
    REGISTER_FAILURE("register failure"),
    INVALID_REGISTRATION("invalid registration");

    var value: String
        internal set

    init {
        this.value = value
    }

}