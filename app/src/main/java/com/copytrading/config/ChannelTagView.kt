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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class ChannelTagView(context: Context) : LinearLayout(context) {

    private val channels = mutableListOf<String>()
    private val chipContainer: LinearLayout
    private val inputField: EditText
    private val container: FrameLayout

    private val bgColor = Color.parseColor("#0F0F1A")
    private val borderColor = Color.parseColor("#2A2A4A")
    private val focusBorderColor = Color.parseColor("#6C63FF")
    private val chipBg = Color.parseColor("#266C63FF")
    private val chipText = Color.parseColor("#8B83FF")
    private val chipXColor = Color.parseColor("#666688")
    private val textColor = Color.parseColor("#E8E8F0")
    private val hintColor = Color.parseColor("#555577")

    init {
        orientation = VERTICAL

        container = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(10).toFloat()
                setStroke(dp(2), borderColor)
            }
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        chipContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        inputField = EditText(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            hint = "Ajouter canal..."
            setHintTextColor(hintColor)
            setPadding(dp(4), dp(2), dp(4), dp(2))
            minWidth = dp(120)
            // Single line but allow IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_DONE
            maxLines = 1
            isSingleLine = true
        }

        // Detect Enter via IME_ACTION_DONE (works on most keyboards)
        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                addFromInput()
                true
            } else false
        }

        // Detect Enter via TextWatcher (catches ALL keyboards including Samsung)
        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                if (text.contains("\n")) {
                    // Remove the newline and add
                    inputField.removeTextChangedListener(this)
                    inputField.setText(text.replace("\n", ""))
                    inputField.setSelection(inputField.text.length)
                    inputField.addTextChangedListener(this)
                    addFromInput()
                }
            }
        })

        // Focus highlight
        inputField.setOnFocusChangeListener { _, hasFocus ->
            (container.background as? GradientDrawable)?.setStroke(
                dp(2), if (hasFocus) focusBorderColor else borderColor
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

    private fun addFromInput() {
        val text = inputField.text.toString().trim()
        if (text.isEmpty()) return

        val parts = text.replace(Regex("\\s+-\\s+"), ",").split(Regex("[,;/.\\s]+"))
        val newChannels = parts.map { it.trim() }.filter { it.isNotEmpty() }

        for (ch in newChannels) {
            channels.add(ch)
            addChipAnimated(ch)
        }
        inputField.text.clear()
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
                override fun onAnimationEnd(animation: android.animation.Animator) {
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
            val chip = createChip(ch) { removeChannel(i) }
            chipContainer.addView(chip, i, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6)
                gravity = Gravity.CENTER_VERTICAL
            })
        }
        chipContainer.addView(inputField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER_VERTICAL })
    }

    private fun addChipAnimated(text: String) {
        val chip = createChip(text) {
            val idx = channels.indexOf(text)
            if (idx >= 0) removeChannel(idx)
        }
        val insertIndex = chipContainer.childCount - 1
        chipContainer.addView(chip, insertIndex, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = dp(6)
            gravity = Gravity.CENTER_VERTICAL
        })
        // Pop animation
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

    private fun createChip(text: String, onRemove: () -> Unit): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(chipBg)
                cornerRadius = dp(6).toFloat()
            }
            setPadding(dp(8), dp(4), dp(6), dp(4))

            addView(TextView(context).apply {
                this.text = text
                setTextColor(chipText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.MONOSPACE
            })
            addView(TextView(context).apply {
                this.text = "\u00D7"
                setTextColor(chipXColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(6), 0, 0, 0)
                setOnClickListener { onRemove() }
            })
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
}
