package com.practiceproject;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
        import com.facebook.react.bridge.Callback;
        import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.IllegalViewOperationException;
import java.util.Timer;
import java.util.TimerTask;


public class HelloWorldModuleJava extends ReactContextBaseJavaModule {
    ReactContext reactContext;

    public HelloWorldModuleJava(ReactApplicationContext reactContext) {
        super(reactContext);//required by React Native
        this.reactContext = reactContext;
    }

    @Override
    //getName is required to define the name of the module represented in JavaScript
    public String getName() {
        return "HelloWorldJava";
    }


    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }



    @ReactMethod
    public void sayHi() {
        try {

            WritableMap params = Arguments.createMap();
            sendEvent(reactContext, "SomeEventName", params);
//            callClient = new CallClient(getCurrentActivity());
//            callClient.connect("https://q-signalling.vdotok.dev:8443","call");
//            new Timer().schedule(
//                    new TimerTask(){
//
//                        @Override
//                        public void run(){
//
//                            System.out.println(callClient.isConnected());
//                            successCallback.invoke( callClient.isConnected());                        }
//
//                    }, 5000);

//            callClient
//
//            CallClient.getInstance(this)?.let {
//                callClient = it
//                callClient.setListener(this)
//            }

        } catch (IllegalViewOperationException e) {
//            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void  numMulti(int a , int b, Callback successCallback) {
        successCallback.invoke(a + b);
    }
}
