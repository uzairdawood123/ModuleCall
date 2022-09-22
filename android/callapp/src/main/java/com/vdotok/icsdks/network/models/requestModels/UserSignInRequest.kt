package com.vdotok.icsdks.network.models.requestModels

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserSignInRequest(
    val email: String,
    val password: String,
    val project_id: String,
): Parcelable