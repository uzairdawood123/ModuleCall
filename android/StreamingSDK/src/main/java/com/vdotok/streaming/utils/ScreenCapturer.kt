package com.vdotok.streaming.utils

import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import org.webrtc.AudioTrack
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoTrack


var screenCapturer: ScreenCapturerAndroid? = null
var ssVideoTrack: VideoTrack? = null
var ssAudioTrack: AudioTrack? = null

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
fun createScreenCapture(mediaProjectionData: Intent): ScreenCapturerAndroid {
    return ScreenCapturerAndroid(mediaProjectionData, object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
        }
    })
}

fun stopScreenCapturer() {
    screenCapturer?.stopCapture()
    screenCapturer?.dispose()
    ssVideoTrack?.dispose()
    ssAudioTrack?.dispose()
    ssVideoTrack = null
    ssAudioTrack = null
    screenCapturer = null

}