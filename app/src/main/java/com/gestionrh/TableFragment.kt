package com.gestionrh

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream

class TableFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_table, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnGeneratePdf).setOnClickListener { generatePdf() }
        refresh()
    }

    fun refresh() {
        val ctx = context ?: return
        val view = view ?: return
        val schools = DataStore.getSchools(ctx)
        val data = DataStore.getData(ctx)

        val sb = StringBuilder()
        // Header
        sb.append("المؤسسة".padEnd(16))
        DataStore.SUBJECTS.forEach { sb.append(it.take(7).padEnd(10)) }
        sb.append("المجموع\n")
        sb.append("─".repeat(16 + DataStore.SUBJECTS.size * 10 + 10)).append("\n")

        // Data rows
        schools.forEach { school ->
            val schoolData = data[school.name] ?: emptyMap()
            var total = 0
            sb.append(school.name.padEnd(16))
            DataStore.SUBJECTS.forEach { subj ->
                val v = schoolData[subj] ?: 0
                total += v
                val txt = if (v > 0) "+$v" else v.toString()
                sb.append(txt.padEnd(10))
            }
            val totalTxt = if (total > 0) "+$total" else total.toString()
            sb.append(totalTxt).append("\n")
        }

        // Totals
        sb.append("─".repeat(16 + DataStore.SUBJECTS.size * 10 + 10)).append("\n")
        sb.append("المجموع".padEnd(16))
        var grandTotal = 0
        DataStore.SUBJECTS.forEach { subj ->
            var colTotal = 0
            schools.forEach { s -> colTotal += (data[s.name] ?: emptyMap())[subj] ?: 0 }
            grandTotal += colTotal
            val txt = if (colTotal > 0) "+$colTotal" else colTotal.toString()
            sb.append(txt.padEnd(10))
        }
        sb.append(if (grandTotal > 0) "+$grandTotal" else grandTotal.toString())

        view.findViewById<TextView>(R.id.tableContent)?.text = sb.toString()
    }

    fun generatePdf() {
        val ctx = context ?: return
        val config = DataStore.getConfig(ctx)
        val schools = DataStore.getSchools(ctx)
        val data = DataStore.getData(ctx)

        val doc = PdfDocument()
        val pageWidth = 842  // A4 landscape
        val pageHeight = 595
        val margin = 30f

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val headerBgPaint = Paint().apply { color = Color.parseColor("#1F3864") }
        val totalBgPaint = Paint().apply { color = Color.parseColor("#D9E2F3") }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        var y = margin

        // Header text
        textPaint.textSize = 11f
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(config["orgName"] ?: "الكونفدرالية الديموقراطية للشغل", pageWidth - margin, y + 12, textPaint)
        y += 18
        textPaint.textSize = 9f
        canvas.drawText(config["unionName"] ?: "النقابة الوطنية للتعليم", pageWidth - margin, y + 10, textPaint)
        y += 15
        canvas.drawText("الإقليم - جرادة", pageWidth - margin, y + 10, textPaint)
        y += 15
        canvas.drawText("المكتب الإقليمي", pageWidth - margin, y + 10, textPaint)
        y += 25

        // Title
        textPaint.textSize = 14f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("وضعية الموارد البشرية", pageWidth / 2f, y + 14, textPaint)
        y += 22

        textPaint.textSize = 10f
        val year = config["schoolYear"] ?: "2027 – 2026"
        canvas.drawText("موسم $year", pageWidth / 2f, y + 12, textPaint)
        y += 18
        canvas.drawText(config["eduLevel"] ?: "تأهيلي", pageWidth / 2f, y + 12, textPaint)
        y += 18
        canvas.drawText(config["section"] ?: "تربية بدنية", pageWidth / 2f, y + 12, textPaint)
        y += 25

        // Table dimensions
        val schoolColW = 80f
        val subjColW = (pageWidth - 2 * margin - schoolColW) / DataStore.SUBJECTS.size
        val rowH = 22f
        val headerH = 28f
        val tableX = margin

        // Header row
        var x = tableX
        headerPaint.textAlign = Paint.Align.CENTER
        // School header
        canvas.drawRect(x, y, x + schoolColW, y + headerH, headerBgPaint)
        canvas.drawText("المؤسسة", x + schoolColW / 2, y + 18, headerPaint)
        x += schoolColW
        // Subject headers
        DataStore.SUBJECTS.forEach { subj ->
            canvas.drawRect(x, y, x + subjColW, y + headerH, headerBgPaint)
            canvas.drawText(subj, x + subjColW / 2, y + 18, headerPaint)
            x += subjColW
        }
        y += headerH

        // Data rows
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 9f
        schools.forEach { school ->
            x = tableX
            val schoolData = data[school.name] ?: emptyMap()
            var total = 0

            // School name
            canvas.drawRect(x, y, x + schoolColW, y + rowH, borderPaint)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(school.name, x + schoolColW / 2, y + 15, textPaint)
            x += schoolColW

            // Subject values
            DataStore.SUBJECTS.forEach { subj ->
                val v = schoolData[subj] ?: 0
                total += v
                canvas.drawRect(x, y, x + subjColW, y + rowH, borderPaint)
                val txt = if (v > 0) "+$v" else v.toString()
                canvas.drawText(txt, x + subjColW / 2, y + 15, textPaint)
                x += subjColW
            }
            y += rowH
        }

        // Totals row
        x = tableX
        canvas.drawRect(x, y, x + schoolColW + DataStore.SUBJECTS.size * subjColW, y + rowH, totalBgPaint)
        canvas.drawRect(x, y, x + schoolColW + DataStore.SUBJECTS.size * subjColW, y + rowH, borderPaint)

        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("المجموع", x + schoolColW / 2, y + 15, textPaint)
        x += schoolColW

        var grandTotal = 0
        DataStore.SUBJECTS.forEach { subj ->
            var colTotal = 0
            schools.forEach { s -> colTotal += (data[s.name] ?: emptyMap())[subj] ?: 0 }
            grandTotal += colTotal
            canvas.drawRect(x, y, x + subjColW, y + rowH, borderPaint)
            val txt = if (colTotal > 0) "+$colTotal" else colTotal.toString()
            canvas.drawText(txt, x + subjColW / 2, y + 15, textPaint)
            x += subjColW
        }

        doc.finishPage(page)

        // Save
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "وضعية_الموارد_البشرية.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        Toast.makeText(context, "تم توليد PDF: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
