package com.copytrading.config

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.copytrading.R

/**
 * §5.4 Composant tags/chips pour les canaux Telegram
 *
 * - Pastille par canal + champ de saisie à la fin
 * - Ajout via IME_ACTION_DONE (Entrée)
 * - Suppression via × ou Backspace sur champ vide
 * - § Split: virgule, point, point-virgule, slash, tiret entouré d'espaces
 */
class ChannelTagView(context: Context) : LinearLayout(context) {

    private val channels = mutableListOf<String>()
    private val chipContainer: LinearLayout
    private val inputField: EditText

    init {
        orientation = VERTICAL

        // Conteneur avec bordure
        val wrapper = FrameLayout(context).apply {
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(2), Color.parseColor("#2A2A4A"))
            }
            background = bg
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        // Layout qui wrap (FlowLayout-like)
        chipContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            // On utilise un FlexboxLayout-like approach avec gravity wrap
        }

        // Champ de saisie
        inputField = EditText(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            hint = "Ajouter canal..."
            setHintTextColor(Color.parseColor("#555577"))
            setPadding(0, 0, 0, 0)
            minWidth = dp(100)
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
        }

        // IME_ACTION_DONE listener (§5.4 — méthode fiable)
        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addFromInput()
                true
            } else {
                false
            }
        }

        // KEYCODE_ENTER listener (certains claviers n'envoient pas IME_ACTION_DONE)
        inputField.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                addFromInput()
                true
            } else if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                if (inputField.text.isEmpty() && channels.isNotEmpty()) {
                    removeChannel(channels.size - 1)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        // Layout horizontal pour chips + input
        val flowLayout = object : LinearLayout(context) {
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
                    val mlp = child.layoutParams as? MarginLayoutParams
                    val childWidth = child.measuredWidth + (mlp?.leftMargin ?: 0) + (mlp?.rightMargin ?: 0)
                    val childHeight = child.measuredHeight + (mlp?.topMargin ?: 0) + (mlp?.bottomMargin ?: 0)

                    if (x + childWidth > width && x > 0) {
                        x = 0
                        y += maxHeight
                        maxHeight = 0
                    }
                    x += childWidth
                    if (childHeight > maxHeight) maxHeight = childHeight
                }

                val totalHeight = y + maxHeight + paddingTop + paddingBottom
                setMeasuredDimension(width, totalHeight.coerceAtLeast(dp(40)))
            }

            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                val width = r - l
                var x = paddingLeft
                var y = paddingTop
                var maxHeight = 0

                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    val lp = child.layoutParams as? MarginLayoutParams
                    val childWidth = child.measuredWidth + (lp?.leftMargin ?: 0) + (lp?.rightMargin ?: 0)
                    val childHeight = child.measuredHeight + (lp?.topMargin ?: 0) + (lp?.bottomMargin ?: 0)

                    if (x + childWidth > width && x > paddingLeft) {
                        x = paddingLeft
                        y += maxHeight
                        maxHeight = 0
                    }

                    val marginLeft = lp?.leftMargin ?: 0
                    val marginTop = lp?.topMargin ?: 0
                    child.layout(
                        x + marginLeft,
                        y + marginTop,
                        x + marginLeft + child.measuredWidth,
                        y + marginTop + child.measuredHeight
                    )
                    x += childWidth
                    if (childHeight > maxHeight) maxHeight = childHeight
                }
            }
        }

        flowLayout.addView(chipContainer)
        flowLayout.addView(inputField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        })

        wrapper.addView(flowLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        addView(wrapper, LayoutParams(
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
        val text = inputField.text.toString()
        if (text.isBlank()) return

        val newChannels = splitInput(text)
        channels.addAll(newChannels)
        inputField.text.clear()
        rebuildChips()
    }

    /**
     * §5.4 Règle de découpage
     * Séparateurs: , . ; / et " - " (tiret entouré d'espaces)
     * Un tiret collé à des chiffres (ex: -1001506646047) n'est PAS un séparateur
     */
    private fun splitInput(text: String): List<String> {
        // 1. Remplacer " - " (tiret entouré d'espaces) par virgule
        val step1 = text.replace(Regex("\\s+-\\s+"), ",")
        // 2. Découper sur , ; . /
        val parts = step1.split(Regex("[,;/.]+"))
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun removeChannel(index: Int) {
        if (index in channels.indices) {
            channels.removeAt(index)
            rebuildChips()
        }
    }

    private fun rebuildChips() {
        chipContainer.removeAllViews()

        for ((i, channel) in channels.withIndex()) {
            val chip = createChip(channel) {
                removeChannel(i)
            }
            chipContainer.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6)
                bottomMargin = dp(4)
            })
        }
    }

    private fun createChip(text: String, onDelete: () -> Unit): View {
        val chip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A2E"))
                cornerRadius = dp(999).toFloat()
                setStroke(dp(2), Color.parseColor("#2A2A4A"))
            }
            background = bg
            setPadding(dp(10), dp(5), dp(6), dp(5))
        }

        val label = TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
        }
        chip.addView(label)

        val deleteBtn = TextView(context).apply {
            this.text = "×"
            setTextColor(Color.parseColor("#FF5252"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), 0, 0, 0)
            setOnClickListener { onDelete() }
        }
        chip.addView(deleteBtn)

        return chip
    }

    private fun dp(v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}
