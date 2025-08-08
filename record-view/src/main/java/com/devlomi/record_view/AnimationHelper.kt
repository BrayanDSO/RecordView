package com.devlomi.record_view

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.vectordrawable.graphics.drawable.AnimatorInflaterCompat

class AnimationHelper(
    context: Context,
    basketImg: ImageView,
    smallBlinkingMic: ImageView?
) {
    private val context: Context?
    private val animatedVectorDrawable: AnimatedVectorDrawableCompat?
    private val basketImg: ImageView?
    private val smallBlinkingMic: ImageView?
    private var alphaAnimation: AlphaAnimation? = null
    private var onBasketAnimationEndListener: OnBasketAnimationEnd? = null
    private var isBasketAnimating = false
    private var isStartRecorded = false
    private var micX = 0f
    private var micY = 0f
    private var micAnimation: AnimatorSet? = null
    private var translateAnimation1: TranslateAnimation? = null
    private var translateAnimation2: TranslateAnimation? = null
    private var handler1: Handler? = null
    private var handler2: Handler? = null


    init {
        this.context = context
        this.smallBlinkingMic = smallBlinkingMic
        this.basketImg = basketImg
        animatedVectorDrawable =
            AnimatedVectorDrawableCompat.create(context, R.drawable.recv_basket_animated)
    }

    @SuppressLint("RestrictedApi")
    fun animateBasket(basketInitialY: Float) {
        isBasketAnimating = true

        clearAlphaAnimation(false)

        //save initial x,y values for mic icon
        if (micX == 0f) {
            micX = smallBlinkingMic!!.x
            micY = smallBlinkingMic.y
        }


        micAnimation = AnimatorInflaterCompat.loadAnimator(
            context,
            R.animator.delete_mic_animation
        ) as AnimatorSet
        micAnimation!!.setTarget(smallBlinkingMic) // set the view you want to animate


        translateAnimation1 = TranslateAnimation(0f, 0f, basketInitialY, basketInitialY - 90)
        translateAnimation1!!.setDuration(250)

        translateAnimation2 = TranslateAnimation(0f, 0f, basketInitialY - 130, basketInitialY)
        translateAnimation2!!.setDuration(350)


        micAnimation!!.start()
        basketImg!!.setImageDrawable(animatedVectorDrawable)

        handler1 = Handler()
        handler1!!.postDelayed({
            basketImg.setVisibility(View.VISIBLE)
            basketImg.startAnimation(translateAnimation1)
        }, 350)

        translateAnimation1!!.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                animatedVectorDrawable!!.start()
                handler2 = Handler()
                handler2!!.postDelayed({
                    basketImg.startAnimation(translateAnimation2)
                    smallBlinkingMic!!.setVisibility(View.INVISIBLE)
                    basketImg.setVisibility(View.INVISIBLE)
                }, 450)
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })


        translateAnimation2!!.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                basketImg.setVisibility(View.INVISIBLE)

                isBasketAnimating = false

                //if the user pressed the record button while the animation is running
                // then do NOT call on Animation end
                if (onBasketAnimationEndListener != null && !isStartRecorded) {
                    onBasketAnimationEndListener!!.onAnimationEnd()
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
    }


    //if the user started a new Record while the Animation is running
    // then we want to stop the current animation and revert views back to default state
    fun resetBasketAnimation() {
        if (isBasketAnimating) {
            translateAnimation1!!.reset()
            translateAnimation1!!.cancel()
            translateAnimation2!!.reset()
            translateAnimation2!!.cancel()

            micAnimation!!.cancel()

            smallBlinkingMic!!.clearAnimation()
            basketImg!!.clearAnimation()


            if (handler1 != null) handler1!!.removeCallbacksAndMessages(null)
            if (handler2 != null) handler2!!.removeCallbacksAndMessages(null)

            basketImg.setVisibility(View.INVISIBLE)
            smallBlinkingMic.x = micX
            smallBlinkingMic.y = micY
            smallBlinkingMic.setVisibility(View.GONE)

            isBasketAnimating = false
        }
    }

    fun clearAlphaAnimation(hideView: Boolean) {
        if (alphaAnimation != null) {
            alphaAnimation!!.cancel()
            alphaAnimation!!.reset()
        }
        if (smallBlinkingMic != null) {
            smallBlinkingMic.clearAnimation()
            if (hideView) {
                smallBlinkingMic.setVisibility(View.GONE)
            }
        }
    }

    fun animateSmallMicAlpha() {
        alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation!!.setDuration(500)
        alphaAnimation!!.repeatMode = Animation.REVERSE
        alphaAnimation!!.setRepeatCount(Animation.INFINITE)
        smallBlinkingMic!!.startAnimation(alphaAnimation)
    }

    fun moveRecordButtonAndSlideToCancelBack(
        recordBtn: RecordButton,
        slideToCancelLayout: LinearLayout,
        initialX: Float,
        initialY: Float,
        difX: Float,
        setY: Boolean
    ) {
        val positionAnimator =
            ValueAnimator.ofFloat(recordBtn.x, initialX)

        positionAnimator.interpolator = AccelerateDecelerateInterpolator()
        positionAnimator.addUpdateListener { animation ->
            val x = animation.getAnimatedValue() as Float
            recordBtn.x = x
            if (setY) {
                recordBtn.y = initialY
            }
        }

        recordBtn.stopScale()
        positionAnimator.setDuration(0)
        positionAnimator.start()


        // if the move event was not called ,then the difX will still 0 and there is no need to move it back
        if (difX != 0f) {
            val x = initialX - difX
            slideToCancelLayout.animate()
                .x(x)
                .setDuration(0)
                .start()
        }
    }

    fun resetSmallMic() {
        smallBlinkingMic!!.setAlpha(1.0f)
        smallBlinkingMic.scaleX = 1.0f
        smallBlinkingMic.scaleY = 1.0f
    }

    fun setOnBasketAnimationEndListener(onBasketAnimationEndListener: OnBasketAnimationEnd?) {
        this.onBasketAnimationEndListener = onBasketAnimationEndListener
    }

    fun onAnimationEnd() {
        if (onBasketAnimationEndListener != null) onBasketAnimationEndListener!!.onAnimationEnd()
    }

    //check if the user started a new Record by pressing the RecordButton
    fun setStartRecorded(startRecorded: Boolean) {
        isStartRecorded = startRecorded
    }
}
