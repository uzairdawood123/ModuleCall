package com.vdotok.streaming.websocket

import android.content.Context
import android.util.Log
import com.vdotok.streaming.commands.AcceptCallCommand
import com.vdotok.streaming.commands.CommandBase
import com.vdotok.streaming.commands.PingCommand
import com.vdotok.streaming.enums.EnumConnectionStatus
import com.vdotok.streaming.websocket.client.*
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.*
import kotlin.jvm.Throws


/**
 * Created By: VdoTok
 * Date & Time: On 6/23/21 At 10:53 PM in 2021
 */
class CustomWebSocket(
    private val context: Context,
    private val hostURL: String,
    private val hostEndPoint: String,
    private val listener: CustomSocketCallBacks
) :
    WebSocketListener {
    private val TAG = "CustomWebSocketListener"
    val executor = ScheduledThreadPoolExecutor(1)
    var mcToken: String = ""
    var projectId: String = ""
    var referenceId: String = ""
    private val trustManagers = object : X509TrustManager {
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {

        }

        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {

        }

        override fun getAcceptedIssuers(): Array<X509Certificate?> {
            return arrayOfNulls(0)
        }

    }


    private lateinit var websocket: WebSocket
    private var websocketCancelled = false


    fun sendMessage(method: String) {
        // can we synchronized void method?
        // this one is used as Send message to socket using our command
        // we have to write another function which will used to send raw data or json data
        websocket.sendText(method)
    }

    fun isConnected(): Boolean {
        return websocket.isOpen
    }

    override fun onTextMessage(websocket: WebSocket?, text: String) {
        Log.i(TAG, "Text Message $text")
        listener.onMessage(text)
    }

    fun setWebSocketCancelled(websocketCancelled: Boolean) {
        this.websocketCancelled = websocketCancelled
    }

    fun disconnect() {
        websocket.disconnect()
    }

    @Throws(Exception::class)
    override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState) {
        Log.i(TAG, "State changed: " + newState.name)
        when (newState) {
            WebSocketState.CREATED,
            WebSocketState.CONNECTING -> listener.connectionStatus(EnumConnectionStatus.CONNECTING)
            WebSocketState.OPEN -> listener.connectionStatus(EnumConnectionStatus.CONNECTED)
            WebSocketState.CLOSING -> listener.connectionStatus(EnumConnectionStatus.CLOSING)
            WebSocketState.CLOSED -> {
//                this will be in working when we get a close or error status in the socket connection
                listener.connectionStatus(EnumConnectionStatus.CLOSED)
            }
        }

    }

    @Throws(Exception::class)
    override fun onConnected(ws: WebSocket?, headers: Map<String?, List<String?>?>?) {
        Log.i(TAG, "Connected")
        listener.onConnect()

    }

    @Throws(Exception::class)
    override fun onConnectError(websocket: WebSocket?, cause: WebSocketException) {
        Log.e(TAG, "Connect error: $cause")
        cause.message?.let { listener.onError(it) }
    }

    @Throws(Exception::class)
    override fun onDisconnected(
        websocket: WebSocket?,
        serverCloseFrame: WebSocketFrame,
        clientCloseFrame: WebSocketFrame,
        closedByServer: Boolean
    ) {
        listener.onDisconnect()
        Log.e(
            TAG,
            "Disconnected " + serverCloseFrame.closeReason + " " + clientCloseFrame.closeReason + " " + closedByServer
        )
    }

    @Throws(Exception::class)
    override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Frame")
    }

    @Throws(Exception::class)
    override fun onContinuationFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Continuation Frame")
    }

    @Throws(Exception::class)
    override fun onTextFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Text Frame")
    }

    @Throws(Exception::class)
    override fun onBinaryFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Binary Frame")
    }

    @Throws(Exception::class)
    override fun onCloseFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Close Frame")
    }

    @Throws(Exception::class)
    override fun onPingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Ping Frame")
    }

    @Throws(Exception::class)
    override fun onPongFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Pong Frame")
    }

    @Throws(Exception::class)
    override fun onTextMessage(websocket: WebSocket?, data: ByteArray?) {

    }

    @Throws(Exception::class)
    override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
        Log.i(TAG, "Binary Message")
    }

    @Throws(Exception::class)
    override fun onSendingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Sending Frame")
    }

    @Throws(Exception::class)
    override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Frame sent")
    }

    @Throws(Exception::class)
    override fun onFrameUnsent(websocket: WebSocket?, frame: WebSocketFrame?) {
        Log.i(TAG, "Frame unsent")
    }

    @Throws(Exception::class)
    override fun onThreadCreated(websocket: WebSocket?, threadType: ThreadType?, thread: Thread?) {
        Log.i(TAG, "Thread created")
    }

    @Throws(Exception::class)
    override fun onThreadStarted(websocket: WebSocket?, threadType: ThreadType?, thread: Thread?) {
        Log.i(TAG, "Thread started")
    }

    @Throws(Exception::class)
    override fun onThreadStopping(websocket: WebSocket?, threadType: ThreadType?, thread: Thread?) {
        Log.i(TAG, "Thread stopping")
    }

    @Throws(Exception::class)
    override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
        Log.i(TAG, "Error!")
        cause?.let { it.message?.let { it1 -> listener.onError(it1) } }


    }

    @Throws(Exception::class)
    override fun onFrameError(
        websocket: WebSocket?,
        cause: WebSocketException?,
        frame: WebSocketFrame?
    ) {
        Log.i(TAG, "Frame error!")
    }

    @Throws(Exception::class)
    override fun onMessageError(
        websocket: WebSocket?,
        cause: WebSocketException,
        frames: List<WebSocketFrame?>?
    ) {
        Log.i(TAG, "Message error! $cause")
        cause.message?.let { listener.onError(it) }
    }

    @Throws(Exception::class)
    override fun onMessageDecompressionError(
        websocket: WebSocket?, cause: WebSocketException?,
        compressed: ByteArray?
    ) {
        Log.i(TAG, "Message decompression error!")
    }

    @Throws(Exception::class)
    override fun onTextMessageError(
        websocket: WebSocket?,
        cause: WebSocketException,
        data: ByteArray?
    ) {
        Log.i(TAG, "Text message error! $cause")
    }

    @Throws(Exception::class)
    override fun onSendError(
        websocket: WebSocket?,
        cause: WebSocketException,
        frame: WebSocketFrame?
    ) {
        Log.i(TAG, "Send error! $cause")
    }

    @Throws(Exception::class)
    override fun onUnexpectedError(websocket: WebSocket?, cause: WebSocketException) {
        Log.i(TAG, "Unexpected error! $cause")
    }

    @Throws(Exception::class)
    override fun handleCallbackError(websocket: WebSocket?, cause: Throwable) {
        Log.e(TAG, "Handle callback error! $cause")
    }

    @Throws(Exception::class)
    override fun onSendingHandshake(
        websocket: WebSocket?,
        requestLine: String?,
        headers: List<Array<String?>?>?
    ) {
        Log.i(TAG, "Sending Handshake! Hello!")
    }

    private fun getWebSocketAddress(): String {
        return try {
            val url = URL(hostURL)
            if (url.port > -1) "wss://" + url.host + ":" + url.port + "/$hostEndPoint" else "wss://" + url.host + "/$hostEndPoint"
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Wrong URL", e)
            e.printStackTrace()
            ""
        }
    }

    private fun fakeSocketFactory(): SocketFactory? {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {

            }

            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls(0)
            }

        }
        var sslContext: SSLContext? = null
        try {
            sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(trustManager), SecureRandom())
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return sslContext?.socketFactory
    }

    fun connect() {
        try {
            val factory = WebSocketFactory()
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManagers), SecureRandom())
            factory.sslContext = sslContext
            factory.sslSocketFactory = getSSLCert()
            factory.verifyHostname = false

            websocket = factory.createSocket(getWebSocketAddress())
            websocket.pingInterval = 5000
            websocket.addListener(this)
            websocket.connectAsynchronously()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun setSocketValues(mcToken: String, referenceId: String, projectId: String) {
        this.mcToken = mcToken
        this.referenceId = referenceId
        this.projectId = projectId
    }

    fun startPingMessageHandler(pingInterval: Int) {
        val initialDelay = 0L
        executor.scheduleWithFixedDelay(
            { sendPingRequest() },
            initialDelay,
            pingInterval.toLong(),
            TimeUnit.SECONDS
        )
    }

    fun stopPingMessageHandler() {
        executor.let { executor ->
            if (!executor.isShutdown || executor.isTerminated) executor.shutdownNow()
        }
    }

    private fun sendPingRequest() {
        val pingCommand = PingCommand(mcToken, projectId, referenceId).compile()
        Log.e("Sent Command: ", pingCommand)
        sendMessage(pingCommand)
    }

    private fun getCertificateFileName() = "vdotok_com.crt"
    private fun getSSLCert(): SSLSocketFactory? {
        // Load CAs from an InputStream
        var certificateFactory: CertificateFactory? = null
        try {
            certificateFactory = CertificateFactory.getInstance("X.509")
            val am = context.assets
            val inputStream = am.open(getCertificateFileName())
            val certificate = certificateFactory.generateCertificate(inputStream)
            inputStream.close()
            // Create a KeyStore containing our trusted CAs
            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType)
            keyStore.load(null, null)
            keyStore.setCertificateEntry("ca", certificate)
            // Create a TrustManager that trusts the CAs in our KeyStore.
            val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
            val trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm)
            trustManagerFactory.init(keyStore)
            val trustManagers = trustManagerFactory.trustManagers
            val x509TrustManager = trustManagers[0] as X509TrustManager

            // Create an SSLSocketFactory that uses our TrustManager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf<TrustManager>(x509TrustManager), SecureRandom())
            return sslContext.socketFactory
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
        return null
    }
}
