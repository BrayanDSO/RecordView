package com.devlomi.record_view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Created by Devlomi on 13/12/2017.
 */
class ScaleAnim(private val view: View) {
    private var scaleUpTo = 2.0f

    fun setScaleUpTo(scaleUpTo: Float) {
        this.scaleUpTo = scaleUpTo
    }

    fun start() {
        val set = AnimatorSet()

        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", scaleUpTo)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", scaleUpTo)

        set.setDuration(150)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.playTogether(scaleY, scaleX)
        set.start()
    }

    fun stop() {
        val set = AnimatorSet()

        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f)

        set.setDuration(150)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.playTogether(scaleY, scaleX)
        set.start()
    }
}
