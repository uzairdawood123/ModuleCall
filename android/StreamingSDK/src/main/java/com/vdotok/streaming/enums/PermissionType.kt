package com.vdotok.streaming.enums

/**
 * Created By: VdoTok
 * Date & Time: On 12/13/21 At 5:08 PM in 2021
 */
enum class PermissionType(value: String) {

    //    generic permissions
    MIC_PERMISSION("mic_permission"),
    CAMERA_PERMISSION("video_permission"),
    WIFI_STATE_PERMISSION("wifi_state_permission");

    var value: String
        internal set

    init {
        this.value = value
    }

}
