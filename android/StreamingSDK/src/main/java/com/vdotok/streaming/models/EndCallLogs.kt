package com.vdotok.streaming.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


/**
 * Created By: VdoTok
 * Date & Time: On 6/21/21 At 1:30 PM in 2021
 */
@Parcelize
data class EndCallLogs(
    var type: String = "data",
    var user_type: String = CallerType.CALLER.value,
    var join_time: String = "",
    var left_time: String = "",
    var device_category: String = "mobile",
    var device_type: String = "android",
    var device_model: String = "",
    var device_os: String = "",
    var local_ip: String = "",
    var public_ip: String = "",
    var battery_usage: String = "0",
    var total_uploaded_bytes: String = "",
    var total_downloaded_bytes: String = "",
): Parcelable

enum class CallerType(value: String) {

    CALLER("CALLER"),
    CALLEE("CALLEE");

    var value: String
        internal set

    init {
        this.value = value
    }
}
