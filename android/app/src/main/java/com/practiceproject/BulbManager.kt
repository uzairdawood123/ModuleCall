package com.practiceproject

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext


class BulbManager : SimpleViewManager<BulbView?>() {
    override fun getName(): String {
        return "Bulb"
    }

    override fun createViewInstance(reactContext: ThemedReactContext): BulbView {
        return BulbView(reactContext)
    }
}