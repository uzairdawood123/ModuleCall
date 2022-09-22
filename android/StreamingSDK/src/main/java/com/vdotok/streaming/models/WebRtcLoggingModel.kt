package com.vdotok.streaming.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.util.concurrent.ScheduledThreadPoolExecutor


/**
 * Created By: VdoTok
 * Date & Time: On 8/3/21 At 6:34 PM in 2021
 */
@Parcelize
data class WebRtcLoggingModel(
    var startUsage: Usage = Usage(currentSentBytes = 0L, currentReceivedBytes = 0L),
    var realtimeUsage: Usage = Usage(currentSentBytes = 0L, currentReceivedBytes = 0L),
    var startTime: Long = 0L,
    var initialBatteryUsage: Int = 0,
    var executor: @RawValue ScheduledThreadPoolExecutor? = null
): Parcelable
