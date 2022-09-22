package com.vdotok.streaming.enums


/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 1:26 PM in 2021
 */

enum class EnumConnectionStatus(value: Int) {

    NOT_CONNECTED(0),
    CONNECTING(1),
    CONNECTED(2),
    ERROR(3),
    CLOSING(4),
    CLOSED(5),
    OPEN(6),
    SOCKET_PING(7);

    var value: Int
        internal set

    init {
        this.value = value
    }
}