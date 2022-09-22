package com.vdotok.icsdks.network.models.responseModels

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaServerMap(
    @SerializedName("complete_address")
    val completeAddress: String,
    @SerializedName("end_point")
    val endPoint: String,
    @SerializedName("host")
    val host: String,
    @SerializedName("port")
    val port: String,
    @SerializedName("protocol")
    val protocol: String
):Parcelable


@Parcelize
data class MessagingServerMap(
    @SerializedName("complete_address")
    val completeAddress: String,
    @SerializedName("host")
    val host: String,
    @SerializedName("port")
    val port: String,
    @SerializedName("protocol")
    val protocol: String,
    @SerializedName("end_point")
    val end_point: String
):Parcelable


//{
//    "media_server_map":{
//    "complete_address":"wss://vtkt1.vdotok.com:8443/call",
//    "end_point":"call",
//    "host":"vtkt1.vdotok.com",
//    "port":"8443",
//    "protocol":"wss"
//},
//    "message":"Success",
//    "messaging_server_map":{
//    "complete_address":"wss://vte3.vdotok.com:443",
//    "host":"vte3.vdotok.com",
//    "port":"443",
//    "protocol":"wss"
//},
//    "process_time":3,
//    "status":200
//}