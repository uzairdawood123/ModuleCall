package com.vdotok.streaming.enums

enum class SessionType(value: String) {
    CALL("call"),
    SCREEN("screen"),
    DEFAULT("default");

    var value: String
        internal set

    init {
        this.value = value
    }
}