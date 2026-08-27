package com.copytrading.config

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class ChannelTagView(context: Context) : LinearLayout(context) {

    private val channels = mutableListOf<String>()
    private val chipContainer: LinearLayout
    private val inputField: EditText
    private val container: FrameLayout

    init {
        orientation = VERTICAL

        container = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(2), Color.parseColor("#2A2A4A"))
            }
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        chipContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        inputField = EditText(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            hint = "Ajouter canal..."
            setHintTextColor(Color.parseColor("#555577"))
            setPadding(dp(4), dp(2), dp(4), dp(2))
            minWidth = dp(120)
            // MULTI_LINE so Enter inserts \n, maxLines=1 to keep single line
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 1
        }

        // Catch Enter via TextWatcher (most reliable across ALL keyboards)
        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                if (text.contains("\n") || text.contains("\r")) {
                    inputField.removeTextChangedListener(this)
                    val clean = text.replace("\n", "").replace("\r", "").trim()
                    inputField.setText("")
                    inputField.addTextChangedListener(this)
                    if (clean.isNotEmpty()) {
                        addChannels(clean)
                    }
                }
            }
        })

        // Focus highlight
        inputField.setOnFocusChangeListener { _, hasFocus ->
            (container.background as? GradientDrawable)?.setStroke(
                dp(2), Color.parseColor(if (hasFocus) "#6C63FF" else "#2A2A4A")
            )
        }

        chipContainer.addView(inputField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER_VERTICAL })

        container.addView(chipContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun setChannels(list: List<String>) {
        channels.clear()
        channels.addAll(list)
        rebuildChips()
    }

    fun getChannels(): List<String> = channels.toList()

    private fun addChannels(text: String) {
        val parts = text.replace(Regex("\\s+-\\s+"), ",").split(Regex("[,;/.]+"))
        for (ch in parts.map { it.trim() }.filter { it.isNotEmpty() }) {
            channels.add(ch)
            addChipAnimated(ch)
        }
    }

    private fun removeChannel(index: Int) {
        if (index !in channels.indices) return
        val chipView = chipContainer.getChildAt(index) ?: return
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(chipView, "scaleX", 1f, 0.4f),
                ObjectAnimator.ofFloat(chipView, "scaleY", 1f, 0.4f),
                ObjectAnimator.ofFloat(chipView, "alpha", 1f, 0f)
            )
            duration = 200
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    channels.removeAt(index)
                    rebuildChips()
                }
            })
            start()
        }
    }

    private fun rebuildChips() {
        chipContainer.removeAllViews()
        for ((i, ch) in channels.withIndex()) {
            chipContainer.addView(createChip(ch, i), i, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(6); gravity = Gravity.CENTER_VERTICAL })
        }
        chipContainer.addView(inputField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER_VERTICAL })
    }

    private fun addChipAnimated(text: String) {
        val chip = createChip(text, channels.size - 1)
        val insertIndex = chipContainer.childCount - 1
        chipContainer.addView(chip, insertIndex, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = dp(6); gravity = Gravity.CENTER_VERTICAL })
        chip.scaleX = 0.4f; chip.scaleY = 0.4f; chip.alpha = 0f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(chip, "scaleX", 0.4f, 1f),
                ObjectAnimator.ofFloat(chip, "scaleY", 0.4f, 1f),
                ObjectAnimator.ofFloat(chip, "alpha", 0f, 1f)
            )
            duration = 350
            interpolator = OvershootInterpolator(1.5f)
            start()
        }
    }

    private fun createChip(text: String, index: Int): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#266C63FF"))
                cornerRadius = dp(6).toFloat()
            }
            setPadding(dp(8), dp(4), dp(6), dp(4))

            addView(TextView(context).apply {
                this.text = text
                setTextColor(Color.parseColor("#8B83FF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.MONOSPACE
            })
            addView(TextView(context).apply {
                this.text = " ×"
                setTextColor(Color.parseColor("#AAAAAA"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(4), 0, 0, 0)
                setOnClickListener { removeChannel(index) }
            })
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
}
