package com.practiceproject

import android.annotation.SuppressLint
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.facebook.react.uimanager.IllegalViewOperationException
import java.util.*


class HelloWorldModule(reactContext: ReactApplicationContext?) :
    ReactContextBaseJavaModule(reactContext)
{
    var reactContext: ReactContext? = reactContext
    var context: ReactApplicationContext = reactApplicationContext


    //getName is required to define the name of the module represented in JavaScript
    override fun getName(): String {
        return "HelloWorldModule"
    }

    @SuppressLint("QueryPermissionsNeeded")
    @ReactMethod
    fun NavigationNative() {
        val send: Intent = Intent(
            context,
            SplashActivity::class.java
        )
        if (send.resolveActivity(context.packageManager) != null) {
            send.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(send)

        }
    }
    @ReactMethod
    fun sayHi() {

//        callClient = CallClient(currentActivity!!)
//        callClient?.connect("https://q-signalling.vdotok.dev:8443", "call")
//        Timer().schedule(
//            object : TimerTask() {
//                override fun run() {
//                    val params = Arguments.createMap().apply {
//                        putString("eventProperty", callClient?.isConnected()?.toString())
//                    }
//                    sendEvent(reactContext!!, "SomeEventName", params)
//                    callClient?.register(
//                        authToken ="8368ee5510834865faf20e4d805cb5e5" ,
//                        refId = "c7aae7c0f2fcb45ffba00c7b9ce017d6", project_id = "115G1WZI",
//                        0
//                    )
//
////                    println(callClient?.isConnected())
////                    successCallback.invoke(callClient?.isConnected())
//                }
//            }, 5000
//        )
//        val params = Arguments.createMap().apply {
//            putString("eventProperty", "someValue")
//        }
//        sendEvent(reactContext!!, "SomeEventName", params)
//        try {
//            successCallback.invoke("Callback : Greetings from kotlin")
//        } catch (e: IllegalViewOperationException) {
//            errorCallback.invoke(e.message)
//        }
    }

    private fun sendEvent(
        reactContext: ReactContext,
        eventName: String,
        params: WritableMap?
    ) {
        reactContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
    @ReactMethod
    fun numMulti(a: Int, b: Int, successCallback: Callback) {
        successCallback.invoke(a + b)
    }

    @ReactMethod
    fun addTwo(successCallback: Callback, a: Int, b: Int): Int {
        try {
            println("as")
            successCallback.invoke("Callback : Greetings from Javaaa")
        } catch (e: IllegalViewOperationException) {
        }
        return 0
    }
}