package com.vdotok.streaming.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


/**
 * Created By: VdoTok
 * Date & Time: On 7/19/21 At 12:17 PM in 2021
 */

@Parcelize
data class PartialCallLogs(
    var type: String = "bytes",
    var upload_bytes: String = "",
    var download_bytes: String = "",
): Parcelable
