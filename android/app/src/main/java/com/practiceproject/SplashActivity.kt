package com.practiceproject

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class SplashActivity : AppCompatActivity() {
//    private var message: TextView? = null
//    private var counter = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        askForPermissions()

//        callClient = CallClient(applicationContext)
//        CallClient.getInstance(this)?.let {
//            callClient = it
//            callClient?.setListener(this)
//        }
//        callClient?.connect("https://q-signalling.vdotok.dev:8443", "call")
//
//        callClient?.register(
//            authToken ="8368ee5510834865faf20e4d805cb5e5" ,
//            refId = "c7aae7c0f2fcb45ffba00c7b9ce017d6",0
//        )






        val send = findViewById<Button>(R.id.appCompatImageView)
        val call = findViewById<Button>(R.id.button)
    send.setOnClickListener {
        call.setOnClickListener {
            Log.e("click1","click11")

        }
//        callClient = CallClient(applicationContext!!)
//        callClient?.connect("http://q-signalling.vdotok.dev:8443", "call")
//        Timer().schedule(
//            object : TimerTask() {
//                override fun run() {
//                    val params = Arguments.createMap().apply {
//                        putString("eventProperty", callClient?.isConnected()?.toString())
//                    }
//                    Toast.makeText(this@SplashActivity, callClient?.isConnected().toString(), Toast.LENGTH_SHORT).show()
//
////                    println(callClient?.isConnected())
////                    successCallback.invoke(callClient?.isConnected())
//                }
//            }, 5000
//        )
//        callClient?.connect("https://q-signalling.vdotok.dev:8443", "call")
//
//        callClient?.register(
//            authToken ="8368ee5510834865faf20e4d805cb5e5" ,
//            refId = "c7aae7c0f2fcb45ffba00c7b9ce017d6",0
//        )
//        Toast.makeText(this@SplashActivity, callClient?.isConnected().toString(), Toast.LENGTH_SHORT).show()
//        dialOneToOneCall(MediaType.VIDEO)

    }






    }





    private fun askForPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) &&
            (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                102
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                100
            )
        }
    }



}

