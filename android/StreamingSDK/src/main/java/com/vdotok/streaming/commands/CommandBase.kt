package com.vdotok.streaming.commands

import org.json.JSONException
import kotlin.jvm.Throws

abstract class CommandBase {
    @Throws(JSONException::class)
    abstract fun compile(): String
}
