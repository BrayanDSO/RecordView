package com.devlomi.record_view

import android.content.Context
import android.util.DisplayMetrics

//convert from/to DP
object DpUtil {
    @JvmStatic
    fun toPixel(dp: Float, context: Context): Float {
        val resources = context.getResources()
        val metrics = resources.getDisplayMetrics()
        val px = dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        return px
    }

    fun toDp(px: Float, context: Context): Float {
        val resources = context.getResources()
        val metrics = resources.getDisplayMetrics()
        val dp = px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        return dp
    }
}
