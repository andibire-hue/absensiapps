package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.local.SettingsEntity
import com.example.data.repository.RecapReportData
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Locale

object PdfReportHelper {
    private const val PAGE_WIDTH = 595 // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN_X = 36f
    private const val MARGIN_TOP = 38f
    private const val MARGIN_BOTTOM = 44f

    fun generateA4Pdf(
        settings: SettingsEntity,
        classLabel: String,
        periodLabel: String,
        report: RecapReportData,
        out: OutputStream
    ) {
        val document = PdfDocument()
        try {
            val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 41, 74)
            }
            val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(16, 185, 129)
            }
            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 41, 74)
            }
            val zebraBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(245, 248, 252)
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 41, 74)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 41, 74)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 9.5f
            }
            val metaBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 8.5f
            }
            val tdBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 41, 74)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Columns sum = 595 - 72 = 523 pt
            // No(24) | NIS(64) | Nama(160) | Kelas(45) | H(32) | S(32) | I(32) | A(32) | Pert(46) | %(56)
            val colWidths = floatArrayOf(24f, 64f, 160f, 45f, 32f, 32f, 32f, 32f, 46f, 56f)
            val headers = arrayOf("No", "NIS", "Nama Lengkap", "Kelas", "H", "S", "I", "A", "Pert.", "Persentase")
            val rowHeight = 20f
            val tableWidth = colWidths.sum()

            var pageNum = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            var canvas = page.canvas
            var y = drawHeader(canvas, settings, classLabel, periodLabel, titlePaint, subtitlePaint, metaPaint, metaBoldPaint, navyPaint, greenPaint)
            y = drawTableHeader(canvas, y, colWidths, headers, rowHeight, headerBgPaint, thPaint)

            val rows = report.studentRows
            if (rows.isEmpty()) {
                canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + 28f, borderPaint)
                canvas.drawText("Belum ada data siswa pada filter yang dipilih.", MARGIN_X + 10f, y + 18f, metaPaint)
                y += 28f
            } else {
                rows.forEachIndexed { idx, row ->
                    if (y + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM - 95f && idx == rows.lastIndex) {
                        // Make sure summary & signature fit or move smoothly
                    }
                    if (y + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                        document.finishPage(page)
                        pageNum++
                        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                        canvas = page.canvas
                        y = MARGIN_TOP
                        y = drawTableHeader(canvas, y, colWidths, headers, rowHeight, headerBgPaint, thPaint)
                    }

                    if (idx % 2 == 1) {
                        canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + rowHeight, zebraBgPaint)
                    }
                    canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + rowHeight, borderPaint)

                    val vals = arrayOf(
                        row.number.toString(),
                        row.student.nis,
                        truncateText(row.student.name, tdPaint, colWidths[2] - 8f),
                        row.className.replace("Kelas ", "Kls "),
                        row.hadir.toString(),
                        row.sakit.toString(),
                        row.izin.toString(),
                        row.alpa.toString(),
                        row.totalMeetings.toString(),
                        String.format(Locale.US, "%.1f%%", row.percentage)
                    )

                    var x = MARGIN_X
                    vals.forEachIndexed { cIdx, text ->
                        if (cIdx > 0) canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                        val p = if (cIdx == 9) tdBoldPaint else tdPaint
                        val textY = y + 13.5f
                        if (cIdx in listOf(0, 3, 4, 5, 6, 7, 8, 9)) {
                            val tw = p.measureText(text)
                            canvas.drawText(text, x + (colWidths[cIdx] - tw) / 2f, textY, p)
                        } else {
                            canvas.drawText(text, x + 5f, textY, p)
                        }
                        x += colWidths[cIdx]
                    }
                    y += rowHeight
                }

                // Summary row
                if (y + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    document.finishPage(page)
                    pageNum++
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                    canvas = page.canvas
                    y = MARGIN_TOP
                }
                canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + rowHeight, zebraBgPaint)
                canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + rowHeight, borderPaint)
                val summaryVals = arrayOf(
                    "", "", "TOTAL / RATA-RATA", "",
                    report.totalHadir.toString(),
                    report.totalSakit.toString(),
                    report.totalIzin.toString(),
                    report.totalAlpa.toString(),
                    report.totalFinalizedMeetings.toString(),
                    String.format(Locale.US, "%.1f%%", report.overallPercentage)
                )
                var sx = MARGIN_X
                summaryVals.forEachIndexed { cIdx, text ->
                    val textY = y + 13.5f
                    if (text.isNotEmpty()) {
                        if (cIdx == 2) {
                            canvas.drawText(text, sx + 5f, textY, tdBoldPaint)
                        } else {
                            val tw = tdBoldPaint.measureText(text)
                            canvas.drawText(text, sx + (colWidths[cIdx] - tw) / 2f, textY, tdBoldPaint)
                        }
                    }
                    sx += colWidths[cIdx]
                }
                y += rowHeight
            }

            // Signature block
            if (y + 105f > PAGE_HEIGHT - MARGIN_BOTTOM) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                y = MARGIN_TOP
            }

            y += 26f
            val sigX = PAGE_WIDTH - MARGIN_X - 190f
            canvas.drawText("Lokojuli, ${DateUtils.formatIndonesianShort(DateUtils.todayIso())}", sigX, y, metaPaint)
            y += 14f
            canvas.drawText("Guru Mata Pelajaran PJOK,", sigX, y, metaPaint)
            y += 52f
            canvas.drawText(settings.teacherName, sigX, y, metaBoldPaint)
            canvas.drawLine(sigX, y + 3f, sigX + 185f, y + 3f, borderPaint)

            document.finishPage(page)
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        settings: SettingsEntity,
        classLabel: String,
        periodLabel: String,
        titlePaint: Paint,
        subtitlePaint: Paint,
        metaPaint: Paint,
        metaBoldPaint: Paint,
        navyPaint: Paint,
        greenPaint: Paint
    ): Float {
        var y = MARGIN_TOP
        canvas.drawText(settings.schoolName.uppercase(), MARGIN_X, y, titlePaint)
        y += 16f
        canvas.drawText("LAPORAN REKAPITULASI KEHADIRAN SISWA (PJOK)", MARGIN_X, y, subtitlePaint)
        y += 8f
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 2.5f, navyPaint)
        canvas.drawRect(MARGIN_X, y, MARGIN_X + 110f, y + 2.5f, greenPaint)
        y += 16f

        val leftX = MARGIN_X
        val rightX = MARGIN_X + 275f
        canvas.drawText("Guru PJOK", leftX, y, metaPaint)
        canvas.drawText(": ${settings.teacherName}", leftX + 78f, y, metaBoldPaint)
        canvas.drawText("Kelas", rightX, y, metaPaint)
        canvas.drawText(": $classLabel", rightX + 68f, y, metaBoldPaint)
        y += 14f

        canvas.drawText("Mata Pelajaran", leftX, y, metaPaint)
        canvas.drawText(": PJOK", leftX + 78f, y, metaBoldPaint)
        canvas.drawText("Periode", rightX, y, metaPaint)
        canvas.drawText(": $periodLabel", rightX + 68f, y, metaBoldPaint)
        y += 14f

        canvas.drawText("Tahun Pelajaran", leftX, y, metaPaint)
        canvas.drawText(": ${settings.academicYear} (${settings.semester})", leftX + 78f, y, metaBoldPaint)
        canvas.drawText("Tanggal Cetak", rightX, y, metaPaint)
        canvas.drawText(": ${DateUtils.formatIndonesianShort(DateUtils.todayIso())}", rightX + 68f, y, metaPaint)
        y += 16f
        return y
    }

    private fun drawTableHeader(
        canvas: Canvas,
        y: Float,
        colWidths: FloatArray,
        headers: Array<String>,
        rowHeight: Float,
        headerBgPaint: Paint,
        thPaint: Paint
    ): Float {
        val tableWidth = colWidths.sum()
        canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + rowHeight, headerBgPaint)
        var x = MARGIN_X
        headers.forEachIndexed { idx, h ->
            val tw = thPaint.measureText(h)
            val tx = if (idx == 1 || idx == 2) x + 5f else x + (colWidths[idx] - tw) / 2f
            canvas.drawText(h, tx, y + 13.5f, thPaint)
            x += colWidths[idx]
        }
        return y + rowHeight
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.length > 3 && paint.measureText("$trimmed...") > maxWidth) {
            trimmed = trimmed.dropLast(1)
        }
        return "$trimmed..."
    }

    fun printReport(
        context: Context,
        jobName: String,
        settings: SettingsEntity,
        classLabel: String,
        periodLabel: String,
        report: RecapReportData
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true || destination == null) {
                    callback?.onWriteCancelled()
                    return
                }
                try {
                    FileOutputStream(destination.fileDescriptor).use { fos ->
                        generateA4Pdf(settings, classLabel, periodLabel, report, fos)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build()
        printManager.print(jobName, adapter, attrs)
    }
}
