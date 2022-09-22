package com.vdotok.streaming.commands

import android.os.Parcelable
import com.vdotok.streaming.enums.RegistrationStatus
import kotlinx.android.parcel.Parcelize


/**
 * Created By: VdoTok
 * Date & Time: On 6/17/21 At 5:01 PM in 2021
 */

@Parcelize
data class RegisterResponse(
    var registrationStatus: RegistrationStatus,
    var mcToken: String? = null,
    var bytes_interval: Int? = null,
    var reConnectStatus: Int = 0,
    var responseMessage: String ?= null
): Parcelable
