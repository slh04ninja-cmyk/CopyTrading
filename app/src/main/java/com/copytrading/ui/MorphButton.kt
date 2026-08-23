package com.copytrading.ui

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.google.android.material.button.MaterialButton

class MorphButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {

    enum class State { IDLE, LOADING, SUCCESS }

    var currentState = State.IDLE
        private set

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }

    private var ringAngle = 0f
    private var ringSweep = 90f
    private var checkProgress = 0f
    private var showRing = false
    private var showCheck = false

    private var ringAnimator: ValueAnimator? = null
    private var checkAnimator: ValueAnimator? = null

    private val idleColor = Color.parseColor("#E53935")     // red
    private val loadingColor = Color.parseColor("#FF9800")   // orange
    private val successColor = Color.parseColor("#4CAF50")   // green

    fun reset() {
        currentState = State.IDLE
        showRing = false
        showCheck = false
        ringAnimator?.cancel()
        checkAnimator?.cancel()
        text = "TOUT FERMER"
        setBackgroundColor(idleColor)
        isEnabled = true
        invalidate()
    }

    fun startMorph(onComplete: () -> Unit) {
        if (currentState != State.IDLE) return
        currentState = State.LOADING
        isEnabled = false

        // Crossfade to loading color
        animateColor(idleColor, loadingColor, 350)

        // Show spinning ring
        text = "Fermeture..."
        showRing = true
        ringAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                ringAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // After ~1.5s, transition to success
        postDelayed({
            transitionToSuccess(onComplete)
        }, 1500)
    }

    private fun transitionToSuccess(onComplete: () -> Unit) {
        currentState = State.SUCCESS
        ringAnimator?.cancel()
        showRing = false

        // Crossfade to success color
        animateColor(loadingColor, successColor, 350)

        // Show checkmark animation
        text = ""
        showCheck = true
        checkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener {
                checkProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        onComplete()

        // Auto-reset after 2s
        postDelayed({
            reset()
        }, 2000)
    }

    private fun animateColor(from: Int, to: Int, duration: Long) {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), from, to)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator(2f)
        animator.addUpdateListener {
            setBackgroundColor(it.animatedValue as Int)
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (showRing) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = 10f * resources.displayMetrics.density
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(rect, ringAngle, ringSweep, false, ringPaint)
        }

        if (showCheck && checkProgress > 0f) {
            val cx = width / 2f
            val cy = height / 2f
            val size = 12f * resources.displayMetrics.density

            // Checkmark path: down-right then up-right
            val startX = cx - size * 0.6f
            val startY = cy
            val midX = cx - size * 0.1f
            val midY = cy + size * 0.5f
            val endX = cx + size * 0.7f
            val endY = cy - size * 0.4f

            if (checkProgress <= 0.5f) {
                // First half: draw first segment
                val p = checkProgress * 2f
                canvas.drawLine(
                    startX, startY,
                    startX + (midX - startX) * p,
                    startY + (midY - startY) * p,
                    checkPaint
                )
            } else {
                // Second half: draw both segments
                canvas.drawLine(startX, startY, midX, midY, checkPaint)
                val p = (checkProgress - 0.5f) * 2f
                canvas.drawLine(
                    midX, midY,
                    midX + (endX - midX) * p,
                    midY + (endY - midY) * p,
                    checkPaint
                )
            }
        }
    }
}
