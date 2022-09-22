package com.vdotok.icsdks.utils

import android.view.View
import java.util.concurrent.TimeUnit


/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 12:22 PM in 2021
 */
object ViewUtils {

    fun View.hide() {
        this.visibility = View.GONE
    }

    fun View.show() {
        this.visibility = View.VISIBLE
    }

    fun View.disableDoubleClick() {
        isClickable = false
        postDelayed({ isClickable = true }, TimeUnit.SECONDS.toMillis(2))
    }

    fun View.performSingleClick(method: () -> Unit) {
        setOnClickListener {
            disableDoubleClick()
            method.invoke()
        }
    }

}