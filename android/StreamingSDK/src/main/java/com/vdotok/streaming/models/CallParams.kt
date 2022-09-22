package com.vdotok.streaming.models

import android.os.Parcelable
import com.vdotok.streaming.enums.CallType
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONObject

/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 1:38 PM in 2021
 */

@Parcelize
data class CallParams(
    // logged in user's ref_id but in some cases it will be of the opponent to keep it generic
    var refId: String = "",
    var toRefIds: ArrayList<String> = ArrayList(),
    var mcToken: String = "",
    var sessionUUID: String = "",
    var requestId: String = "",
    var isInitiator: Boolean = false,
    var callType: CallType = CallType.DEFAULT,
    var sessionType: SessionType = SessionType.DEFAULT,
    var mediaType: MediaType = MediaType.DEFAULT,
    var isAppAudio: Boolean = false,
    var associatedSessionUUID: String = "",
    var isBroadcast: Int = 0,
    var isReInvite: Boolean = false,
    var isRecord: Int = 0,
    var participantCount: Int = 0,
    var customDataPacket: String? = null
): Parcelable