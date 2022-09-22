package com.vdotok.icsdks.network.models.responseModels

import android.os.Parcelable
import android.view.View
import com.vdotok.streaming.enums.CallType
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.views.CallViewRenderer
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue


/**
 * Created By: VdoTok
 * Date & Time: On 6/14/21 At 1:26 PM in 2021
 */

@Parcelize
data class SessionModel(
    var sessionId: String,
    var refIds: ArrayList<String>?,
    var remoteViewMap: @RawValue HashMap<String, CallViewRenderer>?,
    var remoteView: @RawValue CallViewRenderer?,
    var mediaType: MediaType?,
    var callType: CallType?,
    var sessionType: SessionType?
): Parcelable
