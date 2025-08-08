package com.devlomi.record_view

/**
 * Created by Devlomi on 24/08/2017.
 */
interface OnRecordListener {
    fun onStart()
    fun onCancel()
    fun onFinish(recordTime: Long, limitReached: Boolean)
    fun onLessThanSecond()
    fun onLock()
}
