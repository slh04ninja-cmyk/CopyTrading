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
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
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

    // Colors
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

        // Container with border (highlights on focus)
        container = FrameLayout(context).apply {
            val bg = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(10).toFloat()
                setStroke(dp(2), borderColor)
            }
            background = bg
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        // Flow layout for chips + input
        chipContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Input field
        inputField = EditText(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            hint = "Ajouter canal..."
            setHintTextColor(hintColor)
            setPadding(0, 0, 0, 0)
            minWidth = dp(100)
            inputType = InputType.TYPE_CLASS_TEXT
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE
        }

        // Enter key listener (the reliable method)
        inputField.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    addFromInput()
                    true
                }
                KeyEvent.KEYCODE_DEL -> {
                    if (inputField.text.isEmpty() && channels.isNotEmpty()) {
                        removeChannel(channels.size - 1)
                        true
                    } else false
                }
                else -> false
            }
        }

        // Also catch IME_ACTION_DONE for soft keyboards
        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addFromInput()
                true
            } else false
        }

        // Focus highlight
        inputField.setOnFocusChangeListener { _, hasFocus ->
            val bg = container.background as? GradientDrawable
            bg?.setStroke(dp(2), if (hasFocus) focusBorderColor else borderColor)
        }

        // Build layout
        chipContainer.addView(inputField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        })

        container.addView(chipContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        addView(container, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ))
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

        // Split by separators: , ; . / and " - " (space-dash-space)
        val parts = text.replace(Regex("\\s+-\\s+"), ",").split(Regex("[,;/.]+"))
        val newChannels = parts.map { it.trim() }.filter { it.isNotEmpty() }

        for (ch in newChannels) {
            channels.add(ch)
            addChipAnimated(ch, channels.size - 1)
        }

        inputField.text.clear()
    }

    private fun removeChannel(index: Int) {
        if (index !in channels.indices) return
        val chipView = chipContainer.getChildAt(index)
        if (chipView != null) {
            // Scale-down fade animation
            val scaleDown = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(chipView, "scaleX", 1f, 0.4f),
                    ObjectAnimator.ofFloat(chipView, "scaleY", 1f, 0.4f),
                    ObjectAnimator.ofFloat(chipView, "alpha", 1f, 0f)
                )
                duration = 200
            }
            scaleDown.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    channels.removeAt(index)
                    rebuildChips()
                }
            })
            scaleDown.start()
        } else {
            channels.removeAt(index)
            rebuildChips()
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
        // Re-add input at the end
        chipContainer.addView(inputField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        })
    }

    private fun addChipAnimated(text: String, index: Int) {
        val chip = createChip(text) {
            val idx = channels.indexOf(text)
            if (idx >= 0) removeChannel(idx)
        }
        // Insert before input field
        val insertIndex = chipContainer.childCount - 1
        chipContainer.addView(chip, insertIndex, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = dp(6)
            gravity = Gravity.CENTER_VERTICAL
        })
        // Pop animation: scale from 0.4 with overshoot
        chip.scaleX = 0.4f
        chip.scaleY = 0.4f
        chip.alpha = 0f
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
        val chip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                setColor(chipBg)
                cornerRadius = dp(6).toFloat()
            }
            background = bg
            setPadding(dp(8), dp(4), dp(6), dp(4))
        }

        val label = TextView(context).apply {
            this.text = text
            setTextColor(chipText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
        }
        chip.addView(label)

        val xBtn = TextView(context).apply {
            this.text = "\u00D7"
            setTextColor(chipXColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), 0, 0, 0)
            setOnClickListener { onRemove() }
        }
        chip.addView(xBtn)

        return chip
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
