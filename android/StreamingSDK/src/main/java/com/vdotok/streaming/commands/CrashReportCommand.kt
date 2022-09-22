package com.vdotok.streaming.commands

import android.os.Parcelable
import com.google.gson.Gson
import com.vdotok.streaming.enums.CrashState
import com.vdotok.streaming.enums.RequestType
import com.vdotok.streaming.utils.createMD5
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 8/24/21 At 5:10 PM in 2021
 */
data class CrashReportCommand(
    private val projectID: String,
    private val referenceId: String,
    private val mcToken: String,
    private val report: @RawValue Any,
) :

    CommandBase() {
    @Throws(JSONException::class)
    override fun compile(): String {
        val gson = Gson()
        val jsonObject = JSONObject(gson.toJson(report))
        val json = JSONObject()
        json.put("type", "request")
        json.put(
            "requestID",
            createMD5(System.currentTimeMillis().toString() + projectID + referenceId)
        )
        json.put("requestType", RequestType.SDK_CRASH_REPORT.value)
        json.put("referenceID", referenceId)
        json.put("mcToken", mcToken)
        json.put("report", jsonObject)
        return json.toString()
    }
}

@Parcelize
data class CrashReport(
    var type: String = "crash",
    var sdk_version: String = "1.02 beta",
    var crash_state: String = CrashState.NOTHING.value, // this is enum
    var device_type: String = "android",
    var device_category: String = "mobile",
    var device_model: String = "",
    var device_os: String = "",
    var crash_detail: String = "",
): Parcelable




