package com.vdotok.callingappdemo.models.requestModels

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserSignUp(
    var username: String? = null,
    var password: String? = "abc12345",
    var device_type: String = "android",
    var device_model: String? = null,
    var device_os_ver: String? = null,
    var app_version: String = "1.0.0",
    val project_id: String,
): Parcelable