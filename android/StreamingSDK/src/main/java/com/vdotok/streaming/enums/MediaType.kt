package com.vdotok.streaming.enums

enum class MediaType(value: String) {
    VIDEO("video"),
    AUDIO("audio"),
    DEFAULT("default");

    var value: String
        internal set

    init {
        this.value = value
    }
}