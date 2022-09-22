package com.vdotok.streaming.stats

import com.vdotok.streaming.models.SessionDataModel
import com.vdotok.streaming.models.Usage

/**
 * Created By: VdoTok
 * Date & Time: On 8/4/21 At 11:01 AM in 2021
 *
 * Interface provides callbacks for the logging performed for the active sessions
 * */

interface StatsInterface {
    fun sendCurrentDataUsage(sessionKey: String, usage: Usage)
    fun sendEndDataUsage(sessionKey: String, sessionDataModel: SessionDataModel)
    fun memoryUsageDetails(memoryUsage: Long) {}
}