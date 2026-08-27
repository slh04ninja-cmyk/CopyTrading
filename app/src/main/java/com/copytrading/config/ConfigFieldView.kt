package com.copytrading.config

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Génère les vues pour chaque type de champ selon la spec CONFIG_SCREEN_SPEC §5
 */
object ConfigFieldView {

    private fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()

    /**
     * Crée la vue complète pour un champ donné
     * Retourne une View à ajouter au conteneur de groupe
     */
    fun create(context: Context, field: ConfigParser.Field): View {
        return when (field.type) {
            ConfigParser.FieldType.CHANNEL_LIST -> createChannelList(context, field)
            ConfigParser.FieldType.SELECT_INLINE -> createSelectInline(context, field)
            ConfigParser.FieldType.TEXT_INLINE -> createTextInline(context, field)
            ConfigParser.FieldType.BOOL -> createBool(context, field)
            ConfigParser.FieldType.PASSWORD -> createPassword(context, field)
            ConfigParser.FieldType.NUMBER_INLINE -> createNumberInline(context, field)
            ConfigParser.FieldType.FLOAT_INLINE -> createFloatInline(context, field)
            ConfigParser.FieldType.NUMBER -> createStackedNumber(context, field)
            ConfigParser.FieldType.TEXT -> createStackedText(context, field)
        }
    }

    // ===== §5.1 Champs empilés (text, number, password) =====

    private fun createStackedText(context: Context, field: ConfigParser.Field): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 6), 0, dp(context, 6))
        }

        container.addView(createLabel(context, field))
        container.addView(createEditText(context, field.value, false))
        if (field.help.isNotEmpty()) container.addView(createHelp(context, field.help))

        return container
    }

    private fun createStackedNumber(context: Context, field: ConfigParser.Field): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 6), 0, dp(context, 6))
        }

        container.addView(createLabel(context, field))
        val et = createEditText(context, field.value, false).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        container.addView(et)
        if (field.help.isNotEmpty()) container.addView(createHelp(context, field.help))

        return container
    }

    private fun createPassword(context: Context, field: ConfigParser.Field): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 6), 0, dp(context, 6))
        }

        container.addView(createLabel(context, field))

        val wrapper = FrameLayout(context)
        val et = EditText(context).apply {
            setText(field.value)
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(dp(context, 10), dp(context, 9), dp(context, 40), dp(context, 9))
            transformationMethod = PasswordTransformationMethod.getInstance()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            // Bordure
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(context, 10).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#2A2A4A"))
            }
            background = bg
            id = View.generateViewId()
            tag = field.key
        }

        val eyeBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setColorFilter(Color.parseColor("#555577"))
            setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8))
            setOnClickListener {
                if (et.transformationMethod == null) {
                    et.transformationMethod = PasswordTransformationMethod.getInstance()
                    setColorFilter(Color.parseColor("#555577"))
                } else {
                    et.transformationMethod = null
                    setColorFilter(Color.parseColor("#8B83FF"))
                }
                et.setSelection(et.text.length)
            }
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        wrapper.addView(et, lp)

        val eyeLp = FrameLayout.LayoutParams(
            dp(context, 36), dp(context, 36)
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            marginEnd = dp(context, 4)
        }
        wrapper.addView(eyeBtn, eyeLp)

        container.addView(wrapper)
        if (field.help.isNotEmpty()) container.addView(createHelp(context, field.help))

        return container
    }

    // ===== §5.2 Champs inline =====

    private fun createNumberInline(context: Context, field: ConfigParser.Field): View {
        val row = createInlineRow(context)
        row.addView(createInlineLabel(context, field))

        val et = EditText(context).apply {
            setText(field.value)
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(dp(context, 10), dp(context, 9), dp(context, 10), dp(context, 9))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(context, 10).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#2A2A4A"))
            }
            background = bg
            tag = field.key
            filters = arrayOf(InputFilter.LengthFilter(5))
        }
        val lp = LinearLayout.LayoutParams(dp(context, 72), LinearLayout.LayoutParams.WRAP_CONTENT)
        row.addView(et, lp)

        return row
    }

    private fun createFloatInline(context: Context, field: ConfigParser.Field): View {
        val row = createInlineRow(context)
        row.addView(createInlineLabel(context, field))

        val et = EditText(context).apply {
            setText(field.value)
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(dp(context, 10), dp(context, 9), dp(context, 10), dp(context, 9))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(context, 10).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#2A2A4A"))
            }
            background = bg
            tag = field.key
            filters = arrayOf(InputFilter.LengthFilter(6))
        }
        val lp = LinearLayout.LayoutParams(dp(context, 72), LinearLayout.LayoutParams.WRAP_CONTENT)
        row.addView(et, lp)

        return row
    }

    private fun createTextInline(context: Context, field: ConfigParser.Field): View {
        val row = createInlineRow(context)
        row.addView(createInlineLabel(context, field))

        val et = EditText(context).apply {
            setText(field.value)
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            inputType = InputType.TYPE_CLASS_TEXT
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(dp(context, 10), dp(context, 9), dp(context, 10), dp(context, 9))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(context, 10).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#2A2A4A"))
            }
            background = bg
            tag = field.key
            // Largeur adaptative basée sur la longueur de la valeur
            val charWidth = (13f * context.resources.displayMetrics.scaledDensity * 0.6f).toInt()
            val width = (field.value.length + 1) * charWidth + dp(context, 20)
            minWidth = dp(context, 60)
            this.width = width.coerceAtLeast(dp(context, 60))
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        row.addView(et, lp)

        return row
    }

    // ===== §5.3 Toggle (bool) =====

    private fun createBool(context: Context, field: ConfigParser.Field): View {
        val row = createInlineRow(context)
        row.addView(createInlineLabel(context, field))

        val switch = SwitchMaterial(context).apply {
            isChecked = field.value.lowercase() == "true"
            tag = field.key
            // Style
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8E8F0"))
            trackTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2A4A"))
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.addView(switch, lp)

        return row
    }

    // ===== §3.1 Select inline =====

    private fun createSelectInline(context: Context, field: ConfigParser.Field): View {
        val row = createInlineRow(context)
        row.addView(createInlineLabel(context, field))

        val options = ConfigParser.getSelectOptions(field.key)
        val spinner = Spinner(context, Spinner.MODE_DROPDOWN).apply {
            tag = field.key

            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, options.map {
                it.replaceFirstChar { c -> c.uppercase() }
            })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter

            // Pré-sélection
            val idx = options.indexOfFirst { it.equals(field.value, ignoreCase = true) }
            if (idx >= 0) setSelection(idx)

            // Style
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(context, 10).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#2A2A4A"))
            }
            background = bg
        }

        // Largeur adaptative
        val maxLen = options.maxOfOrNull { it.length } ?: 6
        val width = ((maxLen + 2) * 13f * context.resources.displayMetrics.scaledDensity * 0.6f).toInt() + dp(context, 30)
        val lp = LinearLayout.LayoutParams(width.coerceAtLeast(dp(context, 80)), LinearLayout.LayoutParams.WRAP_CONTENT)
        row.addView(spinner, lp)

        return row
    }

    // ===== §5.4 Channel list (tags/chips) =====

    private fun createChannelList(context: Context, field: ConfigParser.Field): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(context, 6), 0, dp(context, 6))
        }

        container.addView(createLabel(context, field))

        val tagView = ChannelTagView(context)
        tagView.tag = field.key

        // Initialiser avec les valeurs existantes
        val initialChannels = field.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        tagView.setChannels(initialChannels)

        container.addView(tagView)
        if (field.help.isNotEmpty()) container.addView(createHelp(context, field.help))

        return container
    }

    // ===== Helpers =====

    private fun createLabel(context: Context, field: ConfigParser.Field): TextView {
        return TextView(context).apply {
            text = ConfigParser.fieldLabel(field.key)
            setTextColor(Color.parseColor("#8888AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(context, 4))
        }
    }

    private fun createInlineLabel(context: Context, field: ConfigParser.Field): TextView {
        return TextView(context).apply {
            text = ConfigParser.fieldLabel(field.key)
            setTextColor(Color.parseColor("#8888AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT
            setPadding(0, 0, dp(context, 8), 0)
        }
    }

    private fun createInlineRow(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(context, 2), 0, dp(context, 2))
        }
    }

    private fun createEditText(context: Context, value: String, isInline: Boolean): EditText {
        return EditText(context).apply {
            setText(value)
            setTextColor(Color.parseColor("#E8E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(dp(context, 10), dp(context, 9), dp(context, 10), dp(context, 9))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F1A"))
                cornerRadius = dp(context, 10).toFloat()
                setStroke(dp(context, 2), Color.parseColor("#2A2A4A"))
            }
            background = bg
            tag = value // stocker la clé via tag après
        }
    }

    private fun createHelp(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#555577"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setPadding(0, dp(context, 2), 0, 0)
        }
    }

    /**
     * Récupère la valeur actuelle d'un champ depuis sa vue
     */
    fun getValue(view: View, field: ConfigParser.Field): String {
        return when (field.type) {
            ConfigParser.FieldType.BOOL -> {
                val switch = findViewByTag(view, field.key) as? SwitchMaterial
                if (switch?.isChecked == true) "true" else "false"
            }
            ConfigParser.FieldType.CHANNEL_LIST -> {
                // Géré séparément via getChannelList
                ""
            }
            else -> {
                val et = findViewByTag(view, field.key) as? EditText
                et?.text?.toString() ?: field.value
            }
        }
    }

    /**
     * Récupère la liste des canaux depuis un ChannelTagView
     */
    fun getChannelList(view: View, field: ConfigParser.Field): List<String> {
        val tagView = findChannelTagView(view, field.key)
        return tagView?.getChannels() ?: emptyList()
    }

    private fun findViewByTag(root: View, tag: String): View? {
        if (root.tag == tag) return root
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findViewByTag(root.getChildAt(i), tag)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findChannelTagView(root: View, tag: String): ChannelTagView? {
        if (root is ChannelTagView && root.tag == tag) return root
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findChannelTagView(root.getChildAt(i), tag)
                if (found != null) return found
            }
        }
        return null
    }
}
