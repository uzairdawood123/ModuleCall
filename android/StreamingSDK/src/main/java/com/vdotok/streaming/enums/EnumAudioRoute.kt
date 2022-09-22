package com.vdotok.streaming.enums


/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 1:36 PM in 2021
 */
enum class EnumAudioRoute(value: Int) {

    SPEAKER(0),
    EAR_PIECE(1);

    var value: Int
        internal set

    init {
        this.value = value
    }

}