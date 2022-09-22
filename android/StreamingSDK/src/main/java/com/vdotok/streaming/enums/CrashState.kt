package com.vdotok.streaming.enums


/**
 * Created By: VdoTok
 * Date & Time: On 8/24/21 At 5:28 PM in 2021
 */
enum class CrashState(value: String) {

    NOTHING("0"),
    ONE_2_ONE_CALL("1"),
    ONE_2_MANY_CALL("2"),
    ONE_2_MANY_MULTI_SESSION("3"),
    MANY_2_MANY("4");

    var value: String
        internal set

    init {
        this.value = value
    }
}