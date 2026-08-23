package com.copytrading.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ConfettiButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {

    private data class Particle(
        val view: View,
        val angle: Double,
        val distance: Float,
        val rotation: Float,
        val size: Float
    )

    fun burstConfetti(onEnd: () -> Unit = {}) {
        val parent = parent as? FrameLayout ?: return
        val colors = intArrayOf(
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1"),
            Color.parseColor("#96CEB4"),
            Color.parseColor("#FFEAA7"),
            Color.parseColor("#DDA0DD"),
            Color.parseColor("#98D8C8"),
            Color.parseColor("#F7DC6F")
        )

        val particles = mutableListOf<Particle>()
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val density = resources.displayMetrics.density

        for (i in 0 until 16) {
            val angle = (i * 360.0 / 16) + Random.nextDouble(-15.0, 15.0)
            val distance = (60 + Random.nextFloat() * 80) * density
            val size = (4 + Random.nextFloat() * 4) * density
            val rotation = Random.nextFloat() * 720f - 360f
            val color = colors[Random.nextInt(colors.size)]

            val particle = View(context).apply {
                setBackgroundColor(color)
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
            }

            val lp = FrameLayout.LayoutParams(size.toInt(), size.toInt())
            lp.leftMargin = cx.toInt() - size.toInt() / 2
            lp.topMargin = cy.toInt() - size.toInt() / 2
            parent.addView(particle, lp)

            particles.add(Particle(particle, Math.toRadians(angle), distance, rotation, size))
        }

        // Animate each particle
        val animators = particles.map { p ->
            val dx = (cos(p.angle) * p.distance).toFloat()
            val dy = (sin(p.angle) * p.distance).toFloat()

            val moveX = ObjectAnimator.ofFloat(p.view, "translationX", 0f, dx).apply {
                duration = 900
                interpolator = DecelerateInterpolator(1.5f)
            }
            val moveY = ObjectAnimator.ofFloat(p.view, "translationY", 0f, dy).apply {
                duration = 900
                interpolator = DecelerateInterpolator(1.5f)
            }
            val rot = ObjectAnimator.ofFloat(p.view, "rotation", 0f, p.rotation).apply {
                duration = 900
            }
            val fade = ObjectAnimator.ofFloat(p.view, "alpha", 1f, 0f).apply {
                duration = 900
            }
            val shrinkX = ObjectAnimator.ofFloat(p.view, "scaleX", 1f, 0.2f).apply {
                duration = 900
            }
            val shrinkY = ObjectAnimator.ofFloat(p.view, "scaleY", 1f, 0.2f).apply {
                duration = 900
            }

            AnimatorSet().apply {
                playTogether(moveX, moveY, rot, fade, shrinkX, shrinkY)
            }
        }

        val set = AnimatorSet()
        set.playTogether(animators.map { it })
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                particles.forEach { parent.removeView(it.view) }
                onEnd()
            }
        })
        set.start()
    }
}
