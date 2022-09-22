package com.vdotok.streaming.websocket

import com.vdotok.streaming.enums.EnumConnectionStatus


/**
 * Created By: VdoTok
 * Date & Time: On 6/23/21 At 11:28 PM in 2021
 */
interface CustomSocketCallBacks {
    fun onConnect()
    fun onDisconnect()
    fun onMessage(response: String)
    fun connectionStatus(status: EnumConnectionStatus)
    fun onError(cause: String)
}