package com.devlomi.record_view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes

/**
 * Created by Devlomi on 13/12/2017.
 */
class RecordButton : AppCompatImageView, View.OnClickListener {
    private var scaleAnim: ScaleAnim? = null
    private var recordView: RecordView? = null
    var isListenForRecord: Boolean = true
    private var onRecordClickListener: OnRecordClickListener? = null
    private var sendClickListener: OnRecordClickListener? = null
    private var isInLockMode = false
    private var micIcon: Drawable? = null
    private var sendIcon: Drawable? = null

    fun setRecordView(recordView: RecordView) {
        this.recordView = recordView
        recordView.setRecordButton(this)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        var scaleUpTo = 1f
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.RecordButton) {
                val imageResource = getResourceId(R.styleable.RecordButton_mic_icon, -1)
                val sendResource = getResourceId(R.styleable.RecordButton_send_icon, -1)
                scaleUpTo = getFloat(R.styleable.RecordButton_scale_up_to, -1f)

                if (imageResource != -1) {
                    setTheImageResource(imageResource)
                }

                if (sendResource != -1) {
                    sendIcon = AppCompatResources.getDrawable(getContext(), sendResource)
                }

            }
        }

        scaleAnim = ScaleAnim(this)
        if (scaleUpTo > 1) {
            scaleAnim!!.setScaleUpTo(scaleUpTo)
        }

        this.setOnClickListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setClip(this)
    }

    fun setClip(view: View) {
        if (view.parent == null) {
            return
        }
        if (view is ViewGroup) {
            view.setClipChildren(false)
            view.clipToPadding = false
        }
        (view.parent as? View)?.let {
            setClip(it)
        }
    }


    private fun setTheImageResource(imageResource: Int) {
        val image = AppCompatResources.getDrawable(getContext(), imageResource)
        setImageDrawable(image)
        micIcon = image
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (this.isListenForRecord) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> recordView!!.onActionDown(this)
                MotionEvent.ACTION_MOVE -> recordView!!.onActionMove(this, event)
                MotionEvent.ACTION_UP -> recordView!!.onActionUp()
            }
        }

        return this.isListenForRecord
    }


    fun startScale() {
        scaleAnim!!.start()
    }

    fun stopScale() {
        scaleAnim!!.stop()
    }

    fun setOnRecordClickListener(onRecordClickListener: OnRecordClickListener?) {
        this.onRecordClickListener = onRecordClickListener
    }

    fun setSendClickListener(sendClickListener: OnRecordClickListener?) {
        this.sendClickListener = sendClickListener
    }

    fun setInLockMode(inLockMode: Boolean) {
        isInLockMode = inLockMode
    }

    override fun onClick(v: View?) {
        if (isInLockMode && sendClickListener != null) {
            sendClickListener!!.onClick(v)
        } else if (onRecordClickListener != null) {
            onRecordClickListener!!.onClick(v)
        }
    }

    fun changeIconToSend() {
        if (sendIcon != null) {
            setImageDrawable(sendIcon)
        }
    }

    fun changeIconToRecord() {
        if (micIcon != null) {
            setImageDrawable(micIcon)
        }
    }
}
