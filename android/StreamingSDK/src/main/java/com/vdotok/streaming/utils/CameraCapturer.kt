package com.vdotok.streaming.utils

import android.content.Context
import android.util.Log
import org.webrtc.*


var videoCapturer: VideoCapturer? = null
var videoTrack: VideoTrack? = null
var audioTrack: AudioTrack? = null


fun createCameraCapturer(context: Context): VideoCapturer? {
    if (videoCapturer != null)
        return videoCapturer
    val enumerator: CameraEnumerator =
        if (false/*Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP*/) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(false)
        }
    val deviceNames = enumerator.deviceNames

    // Try to find front facing camera
    for (deviceName in deviceNames) {
        if (enumerator.isFrontFacing(deviceName)) {
            videoCapturer = enumerator.createCapturer(deviceName, null)
            if (videoCapturer != null) {
                return videoCapturer
            }
        }
    }
    // Front facing camera not found, try something else
    for (deviceName in deviceNames) {
        if (!enumerator.isFrontFacing(deviceName)) {
            videoCapturer = enumerator.createCapturer(deviceName, null)
            if (videoCapturer != null) {
                return videoCapturer
            }
        }
    }
    return null
}

fun stopAudioTrack() {
    audioTrack?.dispose()
    audioTrack = null
    Log.e("audioTrackStatus", "stopAudioTrack: $audioTrack" )
}

fun stopCameraCapturer() {
    videoTrack?.dispose()
    videoTrack = null
    videoCapturer?.stopCapture()
    videoCapturer = null
}
