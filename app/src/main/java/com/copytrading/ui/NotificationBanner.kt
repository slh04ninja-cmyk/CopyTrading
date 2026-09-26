package com.copytrading.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView

class NotificationBanner @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val textView: TextView

    init {
        visibility = GONE
        translationY = -500f

        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#1E1E2E"))
            cornerRadius = 16f * resources.displayMetrics.density
            setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#333355"))
        }
        background = bg

        val pad = (16 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        textView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
        }
        addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun show(message: String, duration: Long = 2500) {
        textView.text = message
        visibility = VISIBLE

        // Drop in with overshoot
        val dropIn = ObjectAnimator.ofFloat(this, "translationY", -500f, 0f).apply {
            this.duration = 550
            interpolator = OvershootInterpolator(1.25f)
        }
        val fadeIn = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
            this.duration = 300
        }

        AnimatorSet().apply {
            playTogether(dropIn, fadeIn)
            start()
        }

        // Auto dismiss
        postDelayed({ dismiss() }, duration)
    }

    private fun dismiss() {
        val dropOut = ObjectAnimator.ofFloat(this, "translationY", 0f, -500f).apply {
            duration = 400
            interpolator = OvershootInterpolator(0.5f)
        }
        val fadeOut = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
            duration = 250
        }

        AnimatorSet().apply {
            playTogether(dropOut, fadeOut)
            start()
        }
    }
}
