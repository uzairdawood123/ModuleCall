package com.vdotok.streaming.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 1:22 PM in 2021
 */

@Parcelize
data class Credentials(
    var mcToken: String
): Parcelable