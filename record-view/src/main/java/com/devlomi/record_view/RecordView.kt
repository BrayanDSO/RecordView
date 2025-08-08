package com.devlomi.record_view

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.devlomi.record_view.DpUtil.toPixel
import io.supercharge.shimmerlayout.ShimmerLayout
import java.io.IOException
import androidx.core.content.withStyledAttributes

/**
 * Created by Devlomi on 24/08/2017.
 */
class RecordView : RelativeLayout, RecordLockViewListener {
    private var smallBlinkingMic: ImageView? = null
    private var basketImg: ImageView? = null
    private var counterTime: Chronometer? = null
    private var slideToCancel: TextView? = null
    private var cancelTextView: TextView? = null
    private var slideToCancelLayout: ShimmerLayout? = null
    private var arrow: ImageView? = null
    private var initialRecordButtonX = 0f
    private var initialRecordButtonY = 0f
    private var recordButtonYInWindow = 0f
    private var basketInitialY = 0f
    private var difX = 0f
    private var cancelBounds = DEFAULT_CANCEL_BOUNDS.toFloat()
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private val context: Context
    private var recordListener: OnRecordListener? = null
    private var recordPermissionHandler: RecordPermissionHandler? = null
    private var isSwiped = false
    private var isLessThanSecondAllowed = false
    private var isSoundEnabled = true
    private var RECORD_START = R.raw.record_start
    private var RECORD_FINISHED = R.raw.record_finished
    private var RECORD_ERROR = R.raw.record_error
    private var player: MediaPlayer? = null
    private var animationHelper: AnimationHelper? = null
    private var isRecordButtonGrowingAnimationEnabled = true
    var isShimmerEffectEnabled: Boolean = true
    private var timeLimit: Long = -1
    private var runnable: Runnable? = null
    private var handler: Handler? = null
    private var recordButton: RecordButton? = null

    private var canRecord = true

    private var recordLockView: RecordLockView? = null
    private var isLockEnabled = false
    var recordLockYInWindow: Float = 0f
    var recordLockXInWindow: Float = 0f
    private var fractionReached = false
    private var currentYFraction = 0f
    private var isLockInSameParent = false


    constructor(context: Context) : super(context) {
        this.context = context
        init(context, null, 0, 0)
    }


    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.context = context
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        this.context = context
        init(context, attrs, defStyleAttr, 0)
    }


    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val view = inflate(context, R.layout.record_view_layout, null)
        addView(view)


        val viewGroup = view.parent as ViewGroup
        viewGroup.setClipChildren(false)

        arrow = view.findViewById(R.id.arrow)
        slideToCancel = view.findViewById(R.id.slide_to_cancel)
        smallBlinkingMic = view.findViewById(R.id.glowing_mic)
        counterTime = view.findViewById(R.id.counter_tv)
        basketImg = view.findViewById(R.id.basket_img)
        slideToCancelLayout = view.findViewById(R.id.shimmer_layout)
        cancelTextView = view.findViewById(R.id.recv_tv_cancel)


        hideViews(true)


        if (attrs != null && defStyleAttr == 0 && defStyleRes == 0) {
            context.withStyledAttributes(
                attrs, R.styleable.RecordView,
                defStyleAttr, defStyleRes
            ) {
                val slideArrowResource =
                    getResourceId(R.styleable.RecordView_slide_to_cancel_arrow, -1)
                val slideToCancelText =
                    getString(R.styleable.RecordView_slide_to_cancel_text)
                val slideMarginRight =
                    getDimension(R.styleable.RecordView_slide_to_cancel_margin_right, 30f)
                        .toInt()
                val counterTimeColor =
                    getColor(R.styleable.RecordView_counter_time_color, -1)
                val arrowColor =
                    getColor(R.styleable.RecordView_slide_to_cancel_arrow_color, -1)

                val cancelText = getString(R.styleable.RecordView_cancel_text)
                val cancelMarginRight =
                    getDimension(R.styleable.RecordView_cancel_text_margin_right, 30f)
                        .toInt()
                val cancelTextColor = getColor(R.styleable.RecordView_cancel_text_color, -1)


                val cancelBounds =
                    getDimensionPixelSize(R.styleable.RecordView_slide_to_cancel_bounds, -1)

                if (cancelBounds != -1) setCancelBounds(
                    cancelBounds.toFloat(),
                    false
                ) //don't convert it to pixels since it's already in pixels


                if (slideArrowResource != -1) {
                    val slideArrow =
                        AppCompatResources.getDrawable(getContext(), slideArrowResource)
                    arrow!!.setImageDrawable(slideArrow)
                }

                if (slideToCancelText != null) slideToCancel!!.text = slideToCancelText

                if (counterTimeColor != -1) setCounterTimeColor(counterTimeColor)


                if (arrowColor != -1) setSlideToCancelArrowColor(arrowColor)

                if (cancelText != null) {
                    cancelTextView!!.text = cancelText
                }

                if (cancelTextColor != -1) {
                    cancelTextView!!.setTextColor(cancelTextColor)
                }

                setMarginRight(slideMarginRight, true)
                setCancelMarginRight(cancelMarginRight, true)

            }
        }


        animationHelper = AnimationHelper(
            context,
            basketImg!!,
            smallBlinkingMic,
            isRecordButtonGrowingAnimationEnabled
        )

        cancelTextView!!.setOnClickListener(OnClickListener { v: View? ->
            animationHelper!!.animateBasket(basketInitialY)
            cancelAndDeleteRecord()
        })
    }

    private fun cancelAndDeleteRecord() {
        if (this.isTimeLimitValid) {
            removeTimeLimitCallbacks()
        }


        isSwiped = true

        animationHelper!!.setStartRecorded(false)

        if (recordListener != null) {
            recordListener!!.onCancel()
        }

        resetRecord(recordButton!!)
    }

    private val isTimeLimitValid: Boolean
        get() = timeLimit > 0

    private fun initTimeLimitHandler() {
        handler = Handler()
        runnable = Runnable {
            if (recordListener != null && !isSwiped) recordListener!!.onFinish(
                elapsedTime,
                true
            )

            removeTimeLimitCallbacks()

            animationHelper!!.setStartRecorded(false)


            if (!isSwiped) playSound(RECORD_FINISHED)


            if (recordButton != null) {
                resetRecord(recordButton!!)
            }
            isSwiped = true
        }
    }


    private fun hideViews(hideSmallMic: Boolean) {
        slideToCancelLayout!!.setVisibility(GONE)
        counterTime!!.visibility = GONE
        cancelTextView!!.visibility = GONE
        if (isLockEnabled && recordLockView != null) {
            recordLockView!!.visibility = GONE
        }
        if (hideSmallMic) smallBlinkingMic!!.setVisibility(GONE)
    }

    private fun showViews() {
        slideToCancelLayout!!.setVisibility(VISIBLE)
        smallBlinkingMic!!.setVisibility(VISIBLE)
        counterTime!!.visibility = VISIBLE
        if (isLockEnabled && recordLockView != null) {
            recordLockView!!.visibility = VISIBLE
        }
    }


    private fun isLessThanOneSecond(time: Long): Boolean {
        return time <= 1000
    }


    private fun playSound(soundRes: Int) {
        if (isSoundEnabled) {
            if (soundRes == 0) return

            try {
                player = MediaPlayer()
                val afd = context.resources.openRawResourceFd(soundRes)
                if (afd == null) return
                player!!.setDataSource(
                    afd.fileDescriptor,
                    afd.startOffset,
                    afd.getLength()
                )
                afd.close()
                player!!.prepare()
                player!!.start()
                player!!.setOnCompletionListener { mp -> mp.release() }
                player!!.isLooping = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    fun onActionDown(recordBtn: RecordButton) {
        if (!this.isRecordPermissionGranted) {
            return
        }


        if (recordListener != null) recordListener!!.onStart()

        if (this.isTimeLimitValid) {
            removeTimeLimitCallbacks()
            handler!!.postDelayed(runnable!!, timeLimit)
        }

        animationHelper!!.setStartRecorded(true)
        animationHelper!!.resetBasketAnimation()
        animationHelper!!.resetSmallMic()


        if (isRecordButtonGrowingAnimationEnabled) {
            recordBtn.startScale()
        }

        if (this.isShimmerEffectEnabled) {
            slideToCancelLayout!!.startShimmerAnimation()
        }

        initialRecordButtonX = recordBtn.x


        val recordButtonLocation = IntArray(2)
        recordBtn.getLocationInWindow(recordButtonLocation)

        initialRecordButtonY = recordButton!!.y

        if (isLockEnabled && recordLockView != null) {
            isLockInSameParent = this.isLockAndRecordButtonHaveSameParent
            val recordLockLocation = IntArray(2)
            recordLockView!!.getLocationInWindow(recordLockLocation)
            recordLockXInWindow = recordLockLocation[0].toFloat()
            recordLockYInWindow =
                if (isLockInSameParent) recordLockView!!.y else recordLockLocation[1].toFloat()
            recordButtonYInWindow =
                if (isLockInSameParent) recordButton!!.y else recordButtonLocation[1].toFloat()
        }


        basketInitialY = basketImg!!.y + 90

        playSound(RECORD_START)

        showViews()

        animationHelper!!.animateSmallMicAlpha()
        counterTime!!.setBase(SystemClock.elapsedRealtime())
        startTime = System.currentTimeMillis()
        counterTime!!.start()
        isSwiped = false
        currentYFraction = 0f
    }


    fun onActionMove(recordBtn: RecordButton, motionEvent: MotionEvent) {
        if (!canRecord || fractionReached) {
            return
        }

        val time = System.currentTimeMillis() - startTime

        if (!isSwiped) {
            //Swipe To Cancel

            if (slideToCancelLayout!!.x != 0f && slideToCancelLayout!!.x <= counterTime!!.right + cancelBounds) {
                //if the time was less than one second then do not start basket animation

                if (isLessThanOneSecond(time)) {
                    hideViews(true)
                    animationHelper!!.clearAlphaAnimation(false)


                    animationHelper!!.onAnimationEnd()
                } else {
                    hideViews(false)
                    animationHelper!!.animateBasket(basketInitialY)
                }

                animationHelper!!.moveRecordButtonAndSlideToCancelBack(
                    recordBtn,
                    slideToCancelLayout!!,
                    initialRecordButtonX,
                    initialRecordButtonY,
                    difX,
                    isLockEnabled
                )

                counterTime!!.stop()
                if (this.isShimmerEffectEnabled) {
                    slideToCancelLayout!!.stopShimmerAnimation()
                }

                isSwiped = true


                animationHelper!!.setStartRecorded(false)

                if (recordListener != null) recordListener!!.onCancel()

                if (this.isTimeLimitValid) {
                    removeTimeLimitCallbacks()
                }
            } else {
                if (canMoveX(motionEvent)) {
                    recordBtn.animate()
                        .x(motionEvent.rawX)
                        .setDuration(0)
                        .start()


                    if (difX == 0f) difX = (initialRecordButtonX - slideToCancelLayout!!.x)


                    slideToCancelLayout!!.animate()
                        .x(motionEvent.rawX - difX)
                        .setDuration(0)
                        .start()
                }

                /*
                  if RecordLock was NOT inside the same parent as RecordButton
                   animate.y() OR view.setY() will setY value INSIDE its parent
                   we need a way to convert the inner value to outer value
                   since motionEvent.getRawY() returns Y's location onScreen
                   we had to get screen height and get the difference between motionEvent and screen height
                 */
                val newY =
                    if (isLockInSameParent) motionEvent.rawY else motionEvent.rawY - recordButtonYInWindow
                if (canMoveY(motionEvent, newY)) {
                    recordBtn.animate()
                        .y(newY)
                        .setDuration(0)
                        .start()

                    val currentY = motionEvent.rawY
                    val minY = recordLockYInWindow
                    val maxY = recordButtonYInWindow

                    var fraction = (currentY - minY) / (maxY - minY)
                    fraction = 1 - fraction
                    currentYFraction = fraction

                    recordLockView!!.animateLock(fraction)

                    if (isRecordButtonGrowingAnimationEnabled) {
                        //convert fraction to scale
                        //so instead of starting from 0 to 1, it will start from 1 to 0
                        val scale = 1 - fraction + 1
                        recordBtn.animate().scaleX(scale).scaleY(scale).setDuration(0).start()
                    }
                }
            }
        }
    }


    private fun canMoveX(motionEvent: MotionEvent): Boolean {
        //Prevent Swiping out of bounds
        if (motionEvent.rawX < initialRecordButtonX) {
            if (isLockEnabled) {
                //prevent swiping X if record button goes up
                return currentYFraction <= 0.3
            }
            return true
        }

        return false
    }

    private fun canMoveY(motionEvent: MotionEvent, dif: Float): Boolean {
        if (isLockEnabled) {
            /*
             1. prevent swiping below record button
             2. prevent swiping up if record button is NOT near record Lock's X
             */
            return if (isLockInSameParent) {
                motionEvent.rawY < initialRecordButtonY && motionEvent.rawX >= recordLockXInWindow
            } else {
                dif <= initialRecordButtonY && motionEvent.rawX >= recordLockXInWindow
            }
        }

        return false
    }

    fun onActionUp(recordBtn: RecordButton?) {
        if (!canRecord || fractionReached) {
            return
        }

        finishAndSaveRecord()
    }

    private fun finishAndSaveRecord() {
        elapsedTime = System.currentTimeMillis() - startTime

        if (!isLessThanSecondAllowed && isLessThanOneSecond(elapsedTime) && !isSwiped) {
            if (recordListener != null) recordListener!!.onLessThanSecond()

            removeTimeLimitCallbacks()
            animationHelper!!.setStartRecorded(false)

            playSound(RECORD_ERROR)
        } else {
            if (recordListener != null && !isSwiped) recordListener!!.onFinish(elapsedTime, false)

            removeTimeLimitCallbacks()

            animationHelper!!.setStartRecorded(false)


            if (!isSwiped) playSound(RECORD_FINISHED)
        }

        resetRecord(recordButton!!)
    }

    private fun switchToLockedMode() {
        cancelTextView!!.visibility = VISIBLE
        slideToCancelLayout!!.setVisibility(GONE)

        recordButton!!.animate()
            .x(initialRecordButtonX)
            .y(initialRecordButtonY)
            .setDuration(100)
            .start()

        if (isRecordButtonGrowingAnimationEnabled) {
            recordButton!!.stopScale()
        }

        recordButton!!.isListenForRecord = false
        recordButton!!.setInLockMode(true)
        recordButton!!.changeIconToSend()
    }

    private val isLockAndRecordButtonHaveSameParent: Boolean
        get() {
            if (recordLockView == null) {
                return false
            }

            val lockParent = recordLockView!!.parent
            val recordButtonParent = recordButton!!.parent
            if (lockParent == null || recordButtonParent == null) {
                return false
            }
            return lockParent === recordButtonParent
        }

    private fun resetRecord(recordBtn: RecordButton) {
        //if user has swiped then do not hide SmallMic since it will be hidden after swipe Animation
        hideViews(!isSwiped)
        fractionReached = false

        if (!isSwiped) animationHelper!!.clearAlphaAnimation(true)

        animationHelper!!.moveRecordButtonAndSlideToCancelBack(
            recordBtn,
            slideToCancelLayout!!,
            initialRecordButtonX,
            initialRecordButtonY,
            difX,
            isLockEnabled
        )
        counterTime!!.stop()
        if (this.isShimmerEffectEnabled) {
            slideToCancelLayout!!.stopShimmerAnimation()
        }

        if (isLockEnabled) {
            recordLockView!!.reset()
            recordBtn.changeIconToRecord()
        }

        cancelTextView!!.visibility = GONE
        recordBtn.isListenForRecord = true
        recordBtn.setInLockMode(false)
    }

    private fun removeTimeLimitCallbacks() {
        if (this.isTimeLimitValid) {
            handler!!.removeCallbacks(runnable!!)
        }
    }


    private val isRecordPermissionGranted: Boolean
        get() {
            canRecord = if (recordPermissionHandler == null) {
                true
            } else {
                recordPermissionHandler!!.isPermissionGranted
            }

            return canRecord
        }

    private fun setMarginRight(marginRight: Int, convertToDp: Boolean) {
        val layoutParams = slideToCancelLayout!!.layoutParams as LayoutParams
        if (convertToDp) {
            layoutParams.rightMargin = toPixel(marginRight.toFloat(), context).toInt()
        } else layoutParams.rightMargin = marginRight

        slideToCancelLayout!!.setLayoutParams(layoutParams)
    }

    private fun setCancelMarginRight(marginRight: Int, convertToDp: Boolean) {
        val layoutParams = slideToCancelLayout!!.layoutParams as LayoutParams
        if (convertToDp) {
            layoutParams.rightMargin = toPixel(marginRight.toFloat(), context).toInt()
        } else layoutParams.rightMargin = marginRight

        cancelTextView!!.setLayoutParams(layoutParams)
    }


    fun setOnRecordListener(recrodListener: OnRecordListener?) {
        this.recordListener = recrodListener
    }

    fun setRecordPermissionHandler(recordPermissionHandler: RecordPermissionHandler?) {
        this.recordPermissionHandler = recordPermissionHandler
    }

    fun setOnBasketAnimationEndListener(onBasketAnimationEndListener: OnBasketAnimationEnd?) {
        animationHelper!!.setOnBasketAnimationEndListener(onBasketAnimationEndListener)
    }

    fun setSoundEnabled(isEnabled: Boolean) {
        isSoundEnabled = isEnabled
    }

    fun setLessThanSecondAllowed(isAllowed: Boolean) {
        isLessThanSecondAllowed = isAllowed
    }

    fun setSlideToCancelText(text: String?) {
        slideToCancel!!.text = text
    }

    fun setSlideToCancelTextColor(color: Int) {
        slideToCancel!!.setTextColor(color)
    }

    fun setSmallMicColor(color: Int) {
        smallBlinkingMic!!.setColorFilter(color)
    }

    fun setSmallMicIcon(icon: Int) {
        smallBlinkingMic!!.setImageResource(icon)
    }

    fun setSlideMarginRight(marginRight: Int) {
        setMarginRight(marginRight, true)
    }


    fun setCustomSounds(startSound: Int, finishedSound: Int, errorSound: Int) {
        //0 means do not play sound
        RECORD_START = startSound
        RECORD_FINISHED = finishedSound
        RECORD_ERROR = errorSound
    }

    fun getCancelBounds(): Float {
        return cancelBounds
    }

    fun setCancelBounds(cancelBounds: Float) {
        setCancelBounds(cancelBounds, true)
    }

    //set Chronometer color
    fun setCounterTimeColor(color: Int) {
        counterTime!!.setTextColor(color)
    }

    fun setSlideToCancelArrowColor(color: Int) {
        arrow!!.setColorFilter(color)
    }


    private fun setCancelBounds(cancelBounds: Float, convertDpToPixel: Boolean) {
        val bounds = if (convertDpToPixel) toPixel(cancelBounds, context) else cancelBounds
        this.cancelBounds = bounds
    }

    fun isRecordButtonGrowingAnimationEnabled(): Boolean {
        return isRecordButtonGrowingAnimationEnabled
    }

    fun setRecordButtonGrowingAnimationEnabled(recordButtonGrowingAnimationEnabled: Boolean) {
        isRecordButtonGrowingAnimationEnabled = recordButtonGrowingAnimationEnabled
        animationHelper!!.setRecordButtonGrowingAnimationEnabled(recordButtonGrowingAnimationEnabled)
    }

    fun getTimeLimit(): Long {
        return timeLimit
    }

    fun setTimeLimit(timeLimit: Long) {
        this.timeLimit = timeLimit

        if (handler != null && runnable != null) {
            removeTimeLimitCallbacks()
        }
        initTimeLimitHandler()
    }

    fun setTrashIconColor(color: Int) {
        animationHelper!!.setTrashIconColor(color)
    }

    fun setRecordLockImageView(recordLockView: RecordLockView?) {
        this.recordLockView = recordLockView
        this.recordLockView!!.recordLockViewListener = this
        this.recordLockView!!.visibility = INVISIBLE
    }

    fun setLockEnabled(lockEnabled: Boolean) {
        isLockEnabled = lockEnabled
    }

    fun setRecordButton(recordButton: RecordButton?) {
        this.recordButton = recordButton
        this.recordButton!!.setSendClickListener { v: View? ->
            finishAndSaveRecord()
        }
    }

    override fun onFractionReached() {
        fractionReached = true
        switchToLockedMode()
        if (recordListener != null) {
            recordListener!!.onLock()
        }
    }

    companion object {
        const val DEFAULT_CANCEL_BOUNDS: Int = 8 //8dp
    }
}


