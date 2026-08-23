package com.copytrading.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class DateRangePickerDialog(
    context: Context,
    private val onDateSelected: (startDate: String, endDate: String) -> Unit
) : Dialog(context) {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val cal = Calendar.getInstance()
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val dayButtons = mutableListOf<TextView>()
    private val monthLabel: TextView
    private val daysGrid: GridLayout

    init {
        val dp = context.resources.displayMetrics.density

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            setPadding((24 * dp).toInt(), (16 * dp).toInt(), (24 * dp).toInt(), (16 * dp).toInt())
        }

        // Title
        root.addView(TextView(context).apply {
            text = "Selectionner un jour ou une periode"
            setTextColor(Color.parseColor("#B8B6D4"))
            textSize = 12f
            setPadding(0, 0, 0, (12 * dp).toInt())
        })

        // Month navigation
        val navRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (12 * dp).toInt())
        }

        navRow.addView(TextView(context).apply {
            text = "\u25C0"
            setTextColor(Color.parseColor("#8B83FF"))
            textSize = 18f
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            setOnClickListener { cal.add(Calendar.MONTH, -1); rebuildDays() }
        })

        monthLabel = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        navRow.addView(monthLabel)

        navRow.addView(TextView(context).apply {
            text = "\u25B6"
            setTextColor(Color.parseColor("#8B83FF"))
            textSize = 18f
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            setOnClickListener { cal.add(Calendar.MONTH, 1); rebuildDays() }
        })
        root.addView(navRow)

        // Day of week headers
        val dowRow = GridLayout(context).apply {
            columnCount = 7
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        for (d in arrayOf("Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di")) {
            dowRow.addView(TextView(context).apply {
                text = d
                setTextColor(Color.parseColor("#666680"))
                textSize = 11f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    height = (32 * dp).toInt()
                }
            })
        }
        root.addView(dowRow)

        // Days grid
        daysGrid = GridLayout(context).apply {
            columnCount = 7
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
        }
        root.addView(daysGrid)

        // Buttons
        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, (16 * dp).toInt(), 0, 0)
        }

        btnRow.addView(TextView(context).apply {
            text = "ANNULER"
            setTextColor(Color.parseColor("#8B83FF"))
            textSize = 14f
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            setOnClickListener { dismiss() }
        })

        btnRow.addView(TextView(context).apply {
            text = "OK"
            setTextColor(Color.parseColor("#8B83FF"))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            setOnClickListener {
                if (startDate != null) {
                    val end = endDate ?: startDate!!
                    val s = if (startDate!!.before(end)) startDate!! else end
                    val e = if (startDate!!.before(end)) end else startDate!!
                    onDateSelected(sdf.format(s.time), sdf.format(e.time))
                }
                dismiss()
            }
        })
        root.addView(btnRow)

        setContentView(root)
        window?.setLayout((300 * dp).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        rebuildDays()
    }

    private fun rebuildDays() {
        val dp = context.resources.displayMetrics.density
        val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)
        monthLabel.text = monthFmt.format(cal.time).replaceFirstChar { it.uppercase() }

        daysGrid.removeAllViews()
        dayButtons.clear()

        val tempCal = Calendar.getInstance()
        tempCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1)
        val firstDow = tempCal.get(Calendar.DAY_OF_WEEK)
        val offset = (firstDow + 5) % 7 // Monday=0
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()

        // Empty cells for offset
        for (i in 0 until offset) {
            daysGrid.addView(TextView(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    height = (40 * dp).toInt()
                }
            })
        }

        for (day in 1..daysInMonth) {
            val dayCal = Calendar.getInstance().apply { set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), day) }
            val tv = TextView(context).apply {
                text = day.toString()
                textSize = 14f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    height = (40 * dp).toInt()
                }
            }

            tv.setOnClickListener {
                if (startDate == null || (startDate != null && endDate != null)) {
                    startDate = dayCal
                    endDate = null
                } else {
                    endDate = dayCal
                }
                updateAllDayStyles(today)
            }

            tv.tag = dayCal
            dayButtons.add(tv)
            daysGrid.addView(tv)
        }

        updateAllDayStyles(today)
    }

    private fun updateAllDayStyles(today: Calendar) {
        for (btn in dayButtons) {
            val tag = btn.tag as? Calendar ?: continue
            val isStart = startDate != null && isSameDay(tag, startDate!!)
            val isEnd = endDate != null && isSameDay(tag, endDate!!)
            val isInRange = startDate != null && endDate != null &&
                tag.after(minOf(startDate!!, endDate!!)) && tag.before(maxOf(startDate!!, endDate!!))
            val isToday = isSameDay(tag, today)

            when {
                isStart || isEnd -> {
                    btn.setTextColor(Color.WHITE)
                    btn.setBackgroundColor(Color.parseColor("#6C63FF"))
                }
                isInRange -> {
                    btn.setTextColor(Color.WHITE)
                    btn.setBackgroundColor(Color.parseColor("#2A2A5A"))
                }
                isToday -> {
                    btn.setTextColor(Color.parseColor("#8B83FF"))
                    btn.background = null
                }
                else -> {
                    btn.setTextColor(Color.parseColor("#E0DEF0"))
                    btn.background = null
                }
            }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
        a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
}
