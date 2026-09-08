package com.gestionrh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun refresh() {
        val ctx = context ?: return
        val view = view ?: return
        val schools = DataStore.getSchools(ctx)
        val data = DataStore.getData(ctx)

        var pos = 0; var neg = 0
        data.values.forEach { schoolData ->
            schoolData.values.forEach { v ->
                if (v > 0) pos += v
                if (v < 0) neg += v
            }
        }
        val net = pos + neg

        view.findViewById<TextView>(R.id.statSchools)?.text = schools.size.toString()
        view.findViewById<TextView>(R.id.statSubjects)?.text = DataStore.SUBJECTS.size.toString()
        view.findViewById<TextView>(R.id.statPositive)?.text = "+$pos"
        view.findViewById<TextView>(R.id.statNegative)?.text = neg.toString()

        val netTv = view.findViewById<TextView>(R.id.statNet)
        netTv?.text = if (net >= 0) "+$net" else net.toString()
        netTv?.setTextColor(resources.getColor(if (net > 0) R.color.profit else if (net < 0) R.color.loss else R.color.primary_light, null))

        // Mini table preview
        val sb = StringBuilder()
        sb.append(buildTableString(data, schools))
        view.findViewById<TextView>(R.id.dashboardPreview)?.text = sb.toString()
    }

    private fun buildTableString(data: Map<String, Map<String, Int>>, schools: List<School>): String {
        val sb = StringBuilder()
        // Header
        sb.append("المؤسسة".padEnd(14))
        DataStore.SUBJECTS.forEach { sb.append(it.take(6).padEnd(8)) }
        sb.append("المجموع\n")
        sb.append("─".repeat(14 + DataStore.SUBJECTS.size * 8 + 8)).append("\n")

        // Rows
        schools.forEach { school ->
            val schoolData = data[school.name] ?: emptyMap()
            var total = 0
            sb.append(school.name.padEnd(14))
            DataStore.SUBJECTS.forEach { subj ->
                val v = schoolData[subj] ?: 0
                total += v
                val txt = if (v > 0) "+$v" else v.toString()
                sb.append(txt.padEnd(8))
            }
            val totalTxt = if (total > 0) "+$total" else total.toString()
            sb.append(totalTxt).append("\n")
        }

        // Totals
        sb.append("─".repeat(14 + DataStore.SUBJECTS.size * 8 + 8)).append("\n")
        sb.append("المجموع".padEnd(14))
        var grandTotal = 0
        DataStore.SUBJECTS.forEach { subj ->
            var colTotal = 0
            schools.forEach { s -> colTotal += (data[s.name] ?: emptyMap())[subj] ?: 0 }
            grandTotal += colTotal
            val txt = if (colTotal > 0) "+$colTotal" else colTotal.toString()
            sb.append(txt.padEnd(8))
        }
        val gtTxt = if (grandTotal > 0) "+$grandTotal" else grandTotal.toString()
        sb.append(gtTxt)

        return sb.toString()
    }
}
