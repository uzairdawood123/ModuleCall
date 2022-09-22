package com.vdotok.streaming.commands

import android.os.Parcelable
import com.vdotok.streaming.enums.CallStatus
import com.vdotok.streaming.models.CallParams
import kotlinx.android.parcel.Parcelize

/**
 * Created By: VdoTok
 * Date & Time: On 6/15/21 At 4:42 PM in 2021
 */
@Parcelize
data class CallInfoResponse(
    var callStatus: CallStatus,
    var responseMessage: String?,
    var callParams: CallParams? = null
): Parcelable
