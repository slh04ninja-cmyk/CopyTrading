package com.copytrading.config

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Tag/token input for channel list.
 * Matches prototype HTML: pill tags with × button, Enter to add, Backspace to remove last.
 */
class ChannelTagView(context: Context) : LinearLayout(context) {

    private val channels = mutableListOf<String>()
    private val chipContainer: FlowLayout
    private val inputField: EditText
    private val wrapper: FrameLayout

    init {
        orientation = VERTICAL

        // Outer wrapper with border (highlights on focus-within)
        wrapper = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(2), Color.parseColor("#2A2A4A"))
            }
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        // Flow layout for chips + input (like CSS flex-wrap)
        chipContainer = FlowLayout(context)

        // Input field
        inputField = object : EditText(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                // Catch Enter key BEFORE EditText processes it
                if (event.action == KeyEvent.ACTION_DOWN &&
                    (event.keyCode == KeyEvent.KEYCODE_ENTER ||
                     event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                     event.keyCode == KeyEvent.KEYCODE_SEARCH)) {
                    addFromInput()
                    return true
                }
                // Catch Backspace on empty field
                if (event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_DEL &&
                    text.isEmpty() && channels.isNotEmpty()) {
                    removeChannel(channels.size - 1)
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            hint = "Ajouter un canal…"
            setHintTextColor(Color.parseColor("#555577"))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            minWidth = dp(100)
            // Single line text, IME action = Done
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_DONE
            maxLines = 1
            isSingleLine = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        // IME_ACTION_DONE fallback (some keyboards use this)
        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_SEND) {
                addFromInput()
                true
            } else false
        }

        // TextWatcher fallback: detect newline inserted by keyboard
        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                if (text.contains("\n") || text.contains("\r")) {
                    inputField.removeTextChangedListener(this)
                    val clean = text.replace(Regex("[\\r\\n]"), "").trim()
                    inputField.setText(clean)
                    inputField.setSelection(clean.length)
                    inputField.addTextChangedListener(this)
                    if (clean.isNotEmpty()) addFromInput()
                }
            }
        })

        // Focus-within border highlight
        inputField.setOnFocusChangeListener { _, hasFocus ->
            (wrapper.background as? GradientDrawable)?.setStroke(
                dp(2), Color.parseColor(if (hasFocus) "#6C63FF" else "#2A2A4A")
            )
        }

        // Add input to flow layout
        chipContainer.addView(inputField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        wrapper.addView(chipContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        addView(wrapper, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
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
        val parts = text.replace(Regex("\\s+-\\s+"), ",").split(Regex("[,;/.]+"))
        for (ch in parts.map { it.trim() }.filter { it.isNotEmpty() }) {
            if (isValidChannel(ch)) {
                channels.add(ch)
            }
        }
        inputField.setText("")
        rebuildChips()
        inputField.requestFocus()
    }

    private fun isValidChannel(ch: String): Boolean {
        if (ch.startsWith("@")) {
            val username = ch.substring(1)
            if (username.length < 5 || username.length > 32) return false
            if (!username[0].isLetter()) return false
            if (username.startsWith("_") || username.endsWith("_")) return false
            if (username.contains("__")) return false
            if (!username.all { it.isLetterOrDigit() || it == '_' }) return false
            return true
        }
        if (ch.startsWith("-100") && ch.length == 14 && ch.substring(1).all { it.isDigit() }) return true
        return false
    }

    private fun removeChannel(index: Int) {
        if (index !in channels.indices) return
        val chipView = chipContainer.getChildAt(index) ?: return
        // Scale-down fade animation (like CSS .removing)
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(chipView, "scaleX", 1f, 0.4f),
                ObjectAnimator.ofFloat(chipView, "scaleY", 1f, 0.4f),
                ObjectAnimator.ofFloat(chipView, "alpha", 1f, 0f)
            )
            duration = 240
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
            val chip = createChip(ch) { removeChannel(i) }
            chipContainer.addView(chip, i)
        }
        // Re-add input at end
        chipContainer.addView(inputField)
    }

    private fun createChip(text: String, onRemove: () -> Unit): View {
        // Pill-shaped tag (matches prototype .tag CSS)
        val chip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#266C63FF")) // rgba(108,99,255,0.15)
                setStroke(dp(1), Color.parseColor("#596C63FF")) // rgba(108,99,255,0.35)
                cornerRadius = dp(99).toFloat() // pill shape
            }
            setPadding(dp(10), dp(4), dp(6), dp(4))
        }

        // Tag text
        chip.addView(TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
        })

        // × remove button (matches prototype .tag-remove-btn)
        chip.addView(TextView(context).apply {
            this.text = "\u00D7"
            setTextColor(Color.parseColor("#8888AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), 0, 0, 0)
            setOnClickListener { onRemove() }
        })

        return chip
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

    /**
     * Simple FlowLayout that wraps children like CSS flex-wrap
     */
    private class FlowLayout(context: Context) : LinearLayout(context) {
        init {
            orientation = LinearLayout.HORIZONTAL
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            var x = 0
            var y = 0
            var maxHeight = 0

            for (i in 0 until childCount) {
                val child = getChildAt(i)
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                val lp = child.layoutParams as? LayoutParams
                val childW = child.measuredWidth + (lp?.leftMargin ?: 0) + (lp?.rightMargin ?: 0)
                val childH = child.measuredHeight + (lp?.topMargin ?: 0) + (lp?.bottomMargin ?: 0)

                if (x + childW > width && x > 0) {
                    x = 0
                    y += maxHeight
                    maxHeight = 0
                }
                x += childW
                if (childH > maxHeight) maxHeight = childH
            }
            setMeasuredDimension(width, y + maxHeight + paddingTop + paddingBottom)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val width = r - l
            var x = paddingLeft
            var y = paddingTop
            var maxHeight = 0

            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val lp = child.layoutParams as? LayoutParams
                val childW = child.measuredWidth + (lp?.leftMargin ?: 0) + (lp?.rightMargin ?: 0)
                val childH = child.measuredHeight + (lp?.topMargin ?: 0) + (lp?.bottomMargin ?: 0)

                if (x + childW > width && x > paddingLeft) {
                    x = paddingLeft
                    y += maxHeight
                    maxHeight = 0
                }
                child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
                x += childW
                if (childH > maxHeight) maxHeight = childH
            }
        }
    }
}
