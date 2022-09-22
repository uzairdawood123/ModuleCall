package com.vdotok.streaming.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Usage(
    var sentAudioBytes: Long = 0L,
    var sentVideoBytes: Long = 0L,
    var receivedAudioBytes: Long = 0L,
    var receivedVideoBytes: Long = 0L,
    var currentSentBytes: Long = 0L,
    var currentReceivedBytes: Long = 0L,
    var totalSentBytes: Long = 0L,
    var totalReceivedBytes: Long = 0L
): Parcelable