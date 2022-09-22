package com.vdotok.streaming.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


/**
 * Created By: VdoTok
 * Date & Time: On 12/08/2021 At 1:03 PM in 2021
 */

@Parcelize
data class SessionStateInfo(
    var sessionKey: String?,
    var refID: String?,
    var audioState: Int?,
    var videoState: Int?,
    var isScreenShare: Boolean?,
): Parcelable
