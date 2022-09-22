package com.practiceproject

//import com.facebook.react.ReactPackage
//import com.facebook.react.bridge.NativeModule
//import com.facebook.react.bridge.ReactApplicationContext
//import com.facebook.react.uimanager.ViewManager


//class HelloWorldPackage : ReactPackage {
////    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
////        return emptyList()
////    }
//
//
//    override fun createNativeModules(
//        reactContext: ReactApplicationContext
//    ): List<NativeModule> {
//        val modules: MutableList<NativeModule> = ArrayList()
//        //this is where you register the module
//        modules.add(HelloWorldModule(reactContext))
//        return modules
//    }
//    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
//        return listOf<ViewManager<*, *>>(
//            BulbManager()
//        )
//    }
//}

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class HelloWorldPackage : ReactPackage {
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }

    override fun createNativeModules(
        reactContext: ReactApplicationContext
    ): List<NativeModule> {
        val modules: MutableList<NativeModule> = ArrayList()
        //this is where you register the module
        modules.add(HelloWorldModule(reactContext))
        return modules
    }
}