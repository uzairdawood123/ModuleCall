package com.vdotok.streaming.enums

enum class CallType(value: String) {
    ONE_TO_ONE("one_to_one"),
    ONE_TO_MANY("one_to_many"),
    MANY_TO_MANY("many_to_many"),
    DEFAULT("default");

    var value: String
        internal set

    init {
        this.value = value
    }

    fun getEnumValue(value: String): CallType {
        return when (value) {
            ONE_TO_ONE.value -> {
                ONE_TO_ONE
            }
            ONE_TO_MANY.value -> {
                ONE_TO_MANY
            }
            MANY_TO_MANY.value -> {
                MANY_TO_MANY
            }
            else -> ONE_TO_ONE
        }
    }
}