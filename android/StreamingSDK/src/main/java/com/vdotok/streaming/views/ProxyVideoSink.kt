package com.vdotok.streaming.views

import android.util.Log
import org.webrtc.VideoFrame
import org.webrtc.VideoSink


/**
 * Created By: VdoTok
 * Date & Time: On 30/10/2021 At 11:01 PM in 2021
 */
class ProxyVideoSink : VideoSink {
    private var target: VideoSink? = null

    @Synchronized
    override fun onFrame(frame: VideoFrame) {
        if (target == null) {
            Log.d("ProxyVideoSink", "Dropping frame in proxy because target is null.")

            return
        }
        target?.onFrame(frame)
    }

    @Synchronized
    fun setTarget(target: VideoSink) {
        this.target = target
    }
}
