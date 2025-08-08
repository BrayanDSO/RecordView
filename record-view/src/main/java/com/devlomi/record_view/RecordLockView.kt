package com.devlomi.record_view

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.util.TypedValueCompat
import androidx.core.graphics.toColorInt

class RecordLockView : View {
    private var bottomLockDrawable: Drawable? = null
    private var topLockDrawable: Drawable? = null
    private var context: Context? = null
    var recordLockViewListener: RecordLockViewListener? = null

    private var defaultCircleColor = "#0A81AB".toColorInt()
    private var circleLockedColor = "#314E52".toColorInt()
    private var circleColor = defaultCircleColor
    private var recordLockAlpha = 255
    private var lockColor = Color.WHITE

    private var topLockTop = 0f
    private var topLockBottom = 0f
    private var initialTopLockTop = 0f
    private var initialTopLockBottom = 0f

    private var bottomLockRect: Rect? = null

    //reduce calling DpUtil.toDp on onDraw
    private var fiveDp = 0f
    private var fourDp = 0f
    private var twoDp = 0f

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr, 0)
    }


    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        this.context = context
        bottomLockDrawable = AppCompatResources.getDrawable(context, R.drawable.recv_lock_bottom)
        topLockDrawable = AppCompatResources.getDrawable(context, R.drawable.recv_lock_top)

        fiveDp = TypedValueCompat.dpToPx(5f, context.resources.displayMetrics)
        fourDp = TypedValueCompat.dpToPx(4f, context.resources.displayMetrics)
        twoDp = TypedValueCompat.dpToPx(2f, context.resources.displayMetrics)

        if (attrs != null && defStyleAttr == 0 && defStyleRes == 0) {
            val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.RecordLockView,
                defStyleAttr, defStyleRes
            )


            val circleColor = typedArray.getColor(R.styleable.RecordLockView_circle_color, -1)
            val circleLockedColor =
                typedArray.getColor(R.styleable.RecordLockView_circle_locked_color, -1)
            val lockColor = typedArray.getColor(R.styleable.RecordLockView_lock_color, -1)

            if (circleColor != -1) {
                this.circleColor = circleColor
            }
            if (circleLockedColor != -1) {
                this.circleLockedColor = circleLockedColor
            }

            if (lockColor != -1) {
                this.lockColor = lockColor
                bottomLockDrawable!!.colorFilter = PorterDuffColorFilter(
                    lockColor,
                    PorterDuff.Mode.SRC_IN
                )
                topLockDrawable!!.colorFilter = PorterDuffColorFilter(
                    lockColor,
                    PorterDuff.Mode.SRC_IN
                )
            }
        }
    }


    private fun animateAlpha() {
        val valueAnimator = ValueAnimator.ofInt(255, 0)
        valueAnimator.addUpdateListener { animation ->
            val animatedValue = animation.getAnimatedValue() as Int
            recordLockAlpha = animatedValue
            invalidate()
        }
        valueAnimator.setDuration(700)
        valueAnimator.interpolator = AnticipateInterpolator()
        valueAnimator.start()
    }

    fun reset() {
        recordLockAlpha = 255
        circleColor = defaultCircleColor
        topLockTop = initialTopLockTop
        topLockBottom = initialTopLockBottom
        invalidate()
    }

    /*
    this will animate ONLY the top part of the lock 'R.drawable.recv_lock_top'
    we will move its top and bottom so it goes inside the bottom lock 'R.drawable.recv_lock_bottom'
     */
    fun animateLock(fraction: Float) {
        if (bottomLockRect == null) {
            return
        }

        val topLockFraction = (fraction + 0.25).toFloat()

        //resize topLock
        val topLockDrawableHeight = (topLockDrawable!!.intrinsicHeight / 2.0).toInt()

        val startTop = initialTopLockTop
        val endTop = (bottomLockRect!!.top - topLockDrawableHeight).toFloat()

        val startBottom = initialTopLockBottom
        val endBottom = (bottomLockRect!!.top + topLockDrawableHeight).toFloat()


        val differenceTop = endTop - startTop
        val differenceBottom = endBottom - startBottom

        val newTop = differenceTop + (startTop * topLockFraction)
        val newBottom = differenceBottom + (startBottom * topLockFraction)


        if (fraction >= 0.85) {
            recordLockViewListener!!.onFractionReached()
            animateAlpha()
            circleColor = circleLockedColor
        } else {
            circleColor = defaultCircleColor
        }

        //start animating lock (top and bottom) ONLY if gets above 0.2 and if it gets to 1.0
        if (topLockFraction <= 1.0f && fraction > 0.2) {
            startValueAnimators(newTop, newBottom)
        }

        invalidate()
    }


    private fun startValueAnimators(newTop: Float, newBottom: Float) {
        val topLockTopAnimator = ValueAnimator.ofFloat(newTop)
        topLockTopAnimator.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator? ->
            val animatedValue = animation!!.getAnimatedValue() as Float
            topLockTop = animatedValue
        })
        topLockTopAnimator.setDuration(0)

        val topLockBottomAnimator = ValueAnimator.ofFloat(newBottom)
        topLockBottomAnimator.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator? ->
            val animatedValue = animation!!.getAnimatedValue() as Float
            topLockBottom = animatedValue
        })
        topLockBottomAnimator.setDuration(0)

        val animatorSet = AnimatorSet()
        animatorSet.setDuration(0).setInterpolator(DecelerateInterpolator())
        animatorSet.playTogether(topLockTopAnimator, topLockBottomAnimator)
        animatorSet.start()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        val cx = width / 2
        val cy = height / 2

        val height = getHeight()


        val paint = Paint()
        paint.setColor(circleColor)
        paint.setAlpha(recordLockAlpha)
        paint.isAntiAlias = true

        canvas.drawCircle(cx.toFloat(), cy.toFloat(), measuredWidth / 2 + fourDp, paint)


        val drawableWidth = (bottomLockDrawable!!.intrinsicWidth / 1.5).toInt()
        val drawableHeight = (bottomLockDrawable!!.intrinsicHeight / 2.0).toInt()


        val bottomLockRect = Rect(
            cx - drawableWidth / 2,
            (cy + fiveDp - drawableHeight / 2).toInt(),
            cx + drawableWidth / 2,
            (height - fiveDp).toInt()
        )

        if (this.bottomLockRect == null) {
            this.bottomLockRect = bottomLockRect
        }

        bottomLockDrawable!!.bounds = bottomLockRect


        val topLockDrawableHeight = (topLockDrawable!!.intrinsicHeight / 1.3).toInt()


        if (topLockTop == 0f) {
            topLockTop = -twoDp
            topLockBottom = topLockDrawableHeight.toFloat()
            initialTopLockTop = topLockTop
            initialTopLockBottom = topLockBottom
        }

        val topLockRect = Rect(
            bottomLockRect.left,
            topLockTop.toInt(),
            bottomLockRect.right,
            topLockBottom.toInt()
        )


        topLockDrawable!!.setBounds(topLockRect)

        topLockDrawable!!.setAlpha(recordLockAlpha)
        bottomLockDrawable!!.setAlpha(recordLockAlpha)

        topLockDrawable!!.draw(canvas)
        bottomLockDrawable!!.draw(canvas)
    }

    fun setDefaultCircleColor(defaultCircleColor: Int) {
        this.defaultCircleColor = defaultCircleColor
        invalidate()
    }

    fun setCircleLockedColor(circleLockedColor: Int) {
        this.circleLockedColor = circleLockedColor
        invalidate()
    }

    fun setLockColor(lockColor: Int) {
        this.lockColor = lockColor
        bottomLockDrawable!!.colorFilter = PorterDuffColorFilter(
            lockColor,
            PorterDuff.Mode.SRC_IN
        )
        topLockDrawable!!.colorFilter = PorterDuffColorFilter(lockColor, PorterDuff.Mode.SRC_IN)
        invalidate()
    }
}
