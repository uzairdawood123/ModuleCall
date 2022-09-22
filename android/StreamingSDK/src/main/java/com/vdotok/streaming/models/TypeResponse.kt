package com.vdotok.streaming.models

enum class TypeResponse(val id: String) {
    ACCEPTED("accepted"), REJECTED("rejected"), RESPONSE("response");

    companion object {
        fun getType(type: String): TypeResponse? {
            for (typeResponse in TypeResponse.values()) {
                if (type == typeResponse.id) {
                    return typeResponse
                }
            }
            return REJECTED
        }
    }
}
