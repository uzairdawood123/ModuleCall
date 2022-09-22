package com.vdotok.streaming.interfaces

import org.webrtc.VideoTrack

/**
 * Created By: VdoTok
 * Date & Time: On 8/4/21 At 11:01 AM in 2021
 *
 * Interface provides stream callbacks i.e. the audio/video stream during audio/video call and
 * user's own camera stream
 * */
interface StreamCallback {
    fun onRemoteStream(stream: VideoTrack, refId: String, sessionID: String)
    fun onRemoteStream(refId: String, sessionID: String)
    fun onCameraStream(stream: VideoTrack)
}