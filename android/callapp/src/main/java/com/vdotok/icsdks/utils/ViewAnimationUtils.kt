package com.vdotok.icsdks.utils

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.GridLayout


/**
 * Created By: VDOTOK
 * Date & Time: On 11/18/21 At 4:12 PM in 2021
 */
object ViewAnimationUtils {

    fun expand(v: View) {
        v.measure(GridLayout.LayoutParams.MATCH_PARENT, GridLayout.LayoutParams.WRAP_CONTENT)
        val targetWidth: Int = v.measuredWidth
        val targetHeight: Int = v.measuredHeight
        v.layoutParams.height = 0
        v.visibility = View.VISIBLE
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
//                v.layoutParams.width =
//                    if (interpolatedTime == 1f) GridLayout.LayoutParams.WRAP_CONTENT
//                    else (targetWidth * interpolatedTime).toInt()
                v.layoutParams.height =
                    if (interpolatedTime == 1f) GridLayout.LayoutParams.WRAP_CONTENT
                    else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
//        val duration = ((targetWidth + targetHeight) / v.context.resources.displayMetrics.density)
        val duration = (targetHeight / v.context.resources.displayMetrics.density)
        a.duration = duration.toLong()
        v.startAnimation(a)
    }

    fun collapse(v: View) {
        val initialHeight: Int = v.measuredHeight
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
//                if (interpolatedTime == 1f) {
//                    v.visibility = View.GONE
//                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
//                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        val duration = (initialHeight / v.context.resources.displayMetrics.density)
        a.duration = duration.toLong()
        v.startAnimation(a)
    }

    fun expandV2(v: View, duration: Int, targetHeight: Int) {
        val prevHeight = v.height
        v.visibility = View.VISIBLE
        val valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight)
        valueAnimator.addUpdateListener { animation ->
            v.layoutParams.height = animation.animatedValue as Int
            v.requestLayout()
        }
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = duration.toLong()
        valueAnimator.start()
    }

    fun collapseV2(v: View, duration: Int, targetHeight: Int) {
        val prevHeight = v.height
        val valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight)
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.addUpdateListener { animation ->
            v.layoutParams.height = animation.animatedValue as Int
            v.requestLayout()
        }
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = duration.toLong()
        valueAnimator.start()
    }

}
