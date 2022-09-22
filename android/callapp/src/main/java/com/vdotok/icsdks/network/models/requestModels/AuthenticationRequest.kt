package com.vdotok.icsdks.network.models.requestModels

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthenticationRequest(
    var auth_token: String,
    var project_id: String
): Parcelable