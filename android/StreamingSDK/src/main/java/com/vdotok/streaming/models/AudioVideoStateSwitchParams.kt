package com.vdotok.streaming.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


/**
 * Created By: VdoTok
 * Date & Time: On 6/28/21 At 7:52 PM in 2021
 */

@Parcelize
data class AudioVideoStateSwitchParams(
    val sessionKey: String,
    val ownRefId: String,
    val audioState: Int,
    val videoState: Int,
): Parcelable
