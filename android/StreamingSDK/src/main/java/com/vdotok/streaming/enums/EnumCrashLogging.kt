package com.vdotok.streaming.enums


/**
 * Created By: VdoTok
 * Date & Time: On 8/24/21 At 4:35 PM in 2021
 */
enum class EnumCrashLogging(value: Int) {

    DISABLED(0),
    ENABLED(1);

    var value: Int
        internal set

    init {
        this.value = value
    }

}