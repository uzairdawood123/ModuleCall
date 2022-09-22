package com.vdotok.icsdks.network.models.responseModels

import android.os.Parcelable
import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize


class UserListModel {
    @SerializedName("min_android_verion")
    var minAndroidVersion: String? = null

    @SerializedName("min_ios_verion")
    var minIosVersion: String? = null

    @SerializedName("message")
    var message: String? = null

    @SerializedName("process_time")
    var processTime: String? = null

    @SerializedName("users")
    var users: List<UserModel> = ArrayList()

    @SerializedName("status")
    var status: String? = null
}

@Parcelize
data class UserModel(
    @SerializedName("email")
    var email: String? = null,
    @SerializedName("user_id")
    var id: String? = null,
    @SerializedName("full_name")
    var fullName: String? = null,
    @SerializedName("ref_id")
    var refID: String? = null,
    var isSelected: Boolean = false

) : Parcelable {
    val userName: String?
        get() {
            if (!TextUtils.isEmpty(fullName)) {
                return fullName
            } else
                return email
        }
}
