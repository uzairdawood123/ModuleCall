package com.vdotok.streaming.stats

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.vdotok.streaming.WebRtcClient
import com.vdotok.streaming.models.CallerType
import com.vdotok.streaming.models.SessionDataModel
import com.vdotok.streaming.models.Usage
import com.vdotok.streaming.models.WebRtcLoggingModel
import com.vdotok.streaming.utils.Prefs
import com.vdotok.streaming.utils.getDeviceName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * Created By: VdoTok
 * Date & Time: On 8/4/21 At 10:59 AM in 2021
 *
 * All commented code in this file is to be used later on and is customizable
 */
class WebRtcStatsLogger private constructor(private val context: Context) {

    private var listener: StatsInterface? = null
    private var bm: BatteryManager
    private var activeSessionsMaps: HashMap<String, WebRtcLoggingModel> = HashMap()
    lateinit var networkStatsManager: NetworkStatsManager
    var applicationUID = 0
    private val prefs: Prefs = Prefs(context)

    init {
        bm = context.getSystemService(AppCompatActivity.BATTERY_SERVICE) as BatteryManager
        applicationUID = context.applicationInfo.uid

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkStatsManager =
                context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        }
    }

    fun startWebRTCSessionLogging(webRtcClient: WebRtcClient?) {
        val executor = ScheduledThreadPoolExecutor(1)
        val initialDelay = 2L

        val key = webRtcClient?.callParams?.sessionUUID

        if (!activeSessionsMaps.containsKey(key)) {
            activeSessionsMaps[webRtcClient?.callParams?.sessionUUID.toString()] =
                WebRtcLoggingModel(
                    startTime = System.currentTimeMillis(),
                    initialBatteryUsage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                    executor = executor
                )

            prefs.timeInterval.let { timeInterval ->
                executor.scheduleWithFixedDelay(
                    { calculateStatsData(webRtcClient) },
                    initialDelay,
                    timeInterval.toLong(),
                    TimeUnit.SECONDS
                )
            }
        } else {
            Log.e("WebRTCLogger", "startWebRTCSessionLogging: Session is already being logged!")
        }
    }

    private fun calculateStatsData(webRtcClient: WebRtcClient?) {
        var sentData = 0L
        var receivedData = 0L
        val iterator = activeSessionsMaps.iterator()

        CoroutineScope(Dispatchers.Default).launch() {
            while (iterator.hasNext()) {
                var sessionModel: Map.Entry<String, WebRtcLoggingModel> = iterator.next()
                var currentUsage = Usage()
                webRtcClient?.peerConnectionClient?.getPeerConnection()
                    ?.getStats { RTCStatsReport ->
                        for (i in RTCStatsReport?.statsMap!!) {
                            if (i.value.type.equals("outbound-rtp")) {
                                if (i.value.members["mediaType"] == "video") {
                                    currentUsage.sentVideoBytes =
                                        i.value.members["bytesSent"].toString().toLong()
                                    sentData += i.value.members["bytesSent"].toString().toLong()
                                }
                                if (i.value.members["mediaType"] == "audio") {
                                    currentUsage.sentAudioBytes =
                                        i.value.members["bytesSent"].toString().toLong()
                                    sentData += i.value.members["bytesSent"].toString().toLong()
                                }
                                currentUsage.currentSentBytes = sentData
//                                Log.e(
//                                    "WebRTCLogs", "outbound-rtp: ${sentData}"
//                                )
                            }

                            if (i.value.type.equals("inbound-rtp")) {
                                if (i.value.members["mediaType"] == "video") {
                                    currentUsage.receivedVideoBytes =
                                        i.value.members["bytesReceived"].toString().toLong()
                                    receivedData += i.value.members["bytesReceived"].toString()
                                        .toLong()
                                }
                                if (i.value.members["mediaType"] == "audio") {
                                    currentUsage.receivedAudioBytes =
                                        i.value.members["bytesReceived"].toString().toLong()
                                    receivedData += i.value.members["bytesReceived"].toString()
                                        .toLong()
                                }
                                currentUsage.currentReceivedBytes = receivedData
//                                Log.e(
//                                    "WebRTCLogs", "inbound-rtp: ${receivedData}"
//                                )
                            }
                        }
                    }

//            val currentSentAudioBytes = currentUsage.sentAudioBytes - sessionModel.value.realtimeUsage.sentAudioBytes
//            val currentSentVideoBytes = currentUsage.sentVideoBytes - sessionModel.value.realtimeUsage.sentVideoBytes
//            val currentReceivedAudioBytes = currentUsage.receivedAudioBytes - sessionModel.value.realtimeUsage.receivedAudioBytes
//            val currentReceivedVideoBytes = currentUsage.receivedVideoBytes - sessionModel.value.realtimeUsage.receivedVideoBytes

                Handler(Looper.getMainLooper()).postDelayed({
                    if (sentData >= 0 && receivedData >= 0) {
                        val currentSentBytes =
                            sentData - sessionModel.value.realtimeUsage.currentSentBytes
                        val currentReceivedBytes =
                            receivedData - sessionModel.value.realtimeUsage.currentReceivedBytes

                        Log.e(
                            "WebRTCLogs",
                            "currentSentBytes: ${currentSentBytes}, currentReceivedBytes: $currentReceivedBytes"
                        )

//            Log.e("WebRTCLogs", "currentSentBytes: $currentSentBytes, currentReceivedBytes: $currentReceivedBytes" )
                        sessionModel.value.realtimeUsage.currentSentBytes = sentData
                        sessionModel.value.realtimeUsage.currentReceivedBytes = receivedData
                        activeSessionsMaps[sessionModel.key] = sessionModel.value
                        listener?.sendCurrentDataUsage(
                            sessionModel.key,
                            Usage(
//                    sentAudioBytes = currentSentAudioBytes,
//                    sentVideoBytes = currentSentVideoBytes,
//                    receivedAudioBytes = currentReceivedAudioBytes,
//                    receivedVideoBytes = currentReceivedVideoBytes,
                                currentSentBytes = currentSentBytes,
                                currentReceivedBytes = currentReceivedBytes,
                            )
                        )
                    }
                }, 100)
            }
        }
    }

    fun getEndSessionLogs(webRtcClient: WebRtcClient?): Usage {
        val endData = Usage()
        var sentData = 0L
        var receivedData = 0L
        webRtcClient?.peerConnectionClient?.getPeerConnection()?.getStats { RTCStatsReport ->
            for (i in RTCStatsReport?.statsMap!!) {
                if (i.value.type.equals("outbound-rtp")) {
                    if (i.value.members["mediaType"] == "video") {
                        sentData += i.value.members["bytesSent"].toString().toLong()
                    }
                    if (i.value.members["mediaType"] == "audio") {
                        sentData += i.value.members["bytesSent"].toString().toLong()
                    }
                    endData.totalSentBytes = sentData
//                    Log.e("WebRTCLogs", "endData outbound: $sentData")
                    Log.e(
                        "WebRTCLogs",
                        "endData outbound: ${i.value.members["bytesSent"].toString().toLong()}"
                    )
                }

                if (i.value.type.equals("inbound-rtp")) {
                    if (i.value.members["mediaType"] == "video") {
                        receivedData += i.value.members["bytesReceived"].toString()
                            .toLong()
                    }
                    if (i.value.members["mediaType"] == "audio") {
                        receivedData += i.value.members["bytesReceived"].toString()
                            .toLong()
                    }
                    endData.totalReceivedBytes = receivedData
//                    Log.e("WebRTCLogs", "endData inbound: $receivedData")
                    Log.e(
                        "WebRTCLogs",
                        "endData inbound: ${i.value.members["bytesReceived"].toString().toLong()}"
                    )
                }
            }
        }
        return endData
    }

    fun sendEndDataLogs(webRtcClient: WebRtcClient, endDataResult: Usage) {
        val sessionModel = activeSessionsMaps[webRtcClient?.callParams?.sessionUUID]
        val endBatteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        sessionModel?.executor?.let { executor ->
            if (!executor.isShutdown || executor.isTerminated) executor.shutdownNow()
        }
        sessionModel?.let { internalSessionModel ->
            var ipsList = webRtcClient.peerConnectionClient.IpsList
            val session = SessionDataModel(
                user_type = if (webRtcClient.isInitiator == true) CallerType.CALLER.value else CallerType.CALLEE.value,
                join_time = internalSessionModel.startTime.toString(),
                left_time = System.currentTimeMillis().toString(),
                device_model = getDeviceName(),
                device_os = Build.VERSION.SDK_INT.toString(),
                local_ip = ipsList[0],//getLocalIp(context),
                public_ip = ipsList[ipsList.size - 1],//apiCall.currentPublicIp,
                battery_usage = if (internalSessionModel.initialBatteryUsage - endBatteryLevel == 0) "0"
                else (internalSessionModel.initialBatteryUsage - endBatteryLevel).toString(),
                total_uploaded_bytes = endDataResult.totalSentBytes.toString(),
                total_downloaded_bytes = endDataResult.totalReceivedBytes.toString()
            )
            listener?.sendEndDataUsage(
                webRtcClient.callParams.sessionUUID.toString(),
                session
            )
        }
//        remove the session model from the map
        activeSessionsMaps.remove(webRtcClient.callParams.sessionUUID)
    }

    /**
     * These functions are for later usage and are important
     * */
//    fun startOne2OneWebRtcLogging(webRtcClient: WebRtcClient?) {
//        val executor = ScheduledThreadPoolExecutor(1)
//        val initialDelay = 2L
//
//        activeSessionsMaps[webRtcClient?.callParams?.sessionUUID.toString()] = WebRtcLoggingModel(
//            startTime = System.currentTimeMillis(),
//            initialBatteryUsage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
//            executor = executor
//        )
//
//        prefs.timeInterval.let { timeInterval ->
//            executor.scheduleWithFixedDelay (
//                {calculateStatsData(webRtcClient)},
//                initialDelay,
//                timeInterval.toLong(),
//                TimeUnit.SECONDS)
//        }
//    }
//
//    fun startMany2ManyWebRtcLogging(webRtcClient: WebRtcClient?) {
//        webRtcLogsExecutor = ScheduledThreadPoolExecutor(1)
//        webRtcLogsExecutor?.scheduleWithFixedDelay(
//            {
//                webRtcClient?.let { client ->
//                    client.m2MPeerConnectionClients.let { pcClientsMap ->
//                        for (i in pcClientsMap) {
//                            i.value.getPeerConnection().getStats { RTCStatsReport ->
//                                for (j in RTCStatsReport?.statsMap!!) {
//
//                                    if (j.value.type.equals("outbound-rtp")) {
//                                        Log.e(
//                                            "WebRTCLogs", "key: ${i.value.refId}," +
//                                                    " bytesSent: ${j.value.members["bytesSent"]}"
//                                        )
//                                    }
//
//                                    if (j.value.type.equals("inbound-rtp")) {
//                                        Log.e(
//                                            "WebRTCLogs", "key: ${i.value.refId}," +
//                                                    " bytesReceived: ${j.value.members["bytesReceived"]}"
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            },
//            0L,
//            1,
//            TimeUnit.SECONDS
//        )
//    }
//
//    fun startOne2ManyWebRtcLogging(webRtcClient: WebRtcClient?) {
//        webRtcLogsExecutor = ScheduledThreadPoolExecutor(1)
//        webRtcLogsExecutor?.scheduleWithFixedDelay(
//            {
//                webRtcClient?.let { client ->
//
//                    if (client.isInitiator) {
//                        client.peerConnectionClient.getPeerConnection().getStats { RTCStatsReport ->
//                            for (i in RTCStatsReport?.statsMap!!) {
//                                if (i.value.type.equals("outbound-rtp")) {
//                                    Log.e(
//                                        "WebRTCLogs", "key: ${i.key}," +
//                                                " bytesSent: ${i.value.members["bytesSent"]}"
//                                    )
//                                }
//                            }
//                        }
//                    } else {
//                        client.m2MPeerConnectionClients.let { pcClientsMap ->
//                            for (i in pcClientsMap) {
//                                i.value.getPeerConnection().getStats { RTCStatsReport ->
//                                    for (j in RTCStatsReport?.statsMap!!) {
//                                        if (j.value.type.equals("transport")) {
//                                            Log.e(
//                                                "WebRTCLogs", "key: ${i.value.refId}," +
//                                                        " bytesReceived: ${j.value.members}"
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            },
//            0L,
//            1,
//            TimeUnit.SECONDS
//        )
//    }

    fun setListener(statsCallbacks: StatsInterface) {
        this.listener = statsCallbacks
    }

    fun removeListener() {
        this.listener = null
    }


    companion object : SingletonHolder<WebRtcStatsLogger, Context>(::WebRtcStatsLogger)

}

open class SingletonHolder<out T : Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator

    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T? {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator?.let { it(arg) }
                instance = created
                creator = null
                created
            }
        }
    }
}

