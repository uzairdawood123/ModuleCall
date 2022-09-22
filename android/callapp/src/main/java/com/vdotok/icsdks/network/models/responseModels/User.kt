package com.vdotok.icsdks.network.models.responseModels

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    var active: String?,
    var app_id: String?,
    var auth_token: String,
    var authorization_token: String,
    var client_id: String?,
    var email: String?,
    var first_name: String?,
    var full_name: String?,
    var last_name: String?,
    var message: String?,
    var min_android_verion: String?,
    var min_ios_verion: String?,
    var passwordCount: String?,
    var phone_num: Long?,
    var process_time: Int?,
    var profile_pic: String?,
    var ref_id: String,
    var status: Int?,
    var userid: Int?,
    var username: String?,
    var requestID: String?,
    var mcToken: String,
    val id: Int?,
    var isSelected:Boolean = false,
    @SerializedName("media_server_map")
    val mediaServer: MessagingServerMap,
    @SerializedName("messaging_server_map")
    val messagingServer: MessagingServerMap,

) : Parcelable