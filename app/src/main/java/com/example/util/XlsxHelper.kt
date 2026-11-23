package com.example.util

import android.util.Xml
import com.example.data.local.Gender
import com.example.data.local.SettingsEntity
import com.example.data.local.StudentEntity
import com.example.data.repository.ImportStudentInput
import com.example.data.repository.RecapReportData
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed class CellValue {
    data class Text(val value: String, val bold: Boolean = false) : CellValue()
    data class Number(val value: Double, val bold: Boolean = false) : CellValue()
    data object Blank : CellValue()
}

data class ParsedImportData(
    val rows: List<ImportStudentInput>,
    val parseErrors: List<String>
)

object XlsxHelper {

    fun writeStudentTemplate(out: OutputStream) {
        val rows = listOf(
            listOf(
                CellValue.Text("NIS", bold = true),
                CellValue.Text("Nama Lengkap", bold = true),
                CellValue.Text("Jenis Kelamin (L/P)", bold = true),
                CellValue.Text("Kelas (1-6)", bold = true)
            ),
            listOf(
                CellValue.Text("20260101"),
                CellValue.Text("Contoh: Ahmad Pratama"),
                CellValue.Text("L"),
                CellValue.Number(1.0)
            )
        )
        writeXlsx("Template Siswa", rows, listOf(18.0, 32.0, 22.0, 14.0), out)
    }

    fun writeStudentsExport(students: List<StudentEntity>, out: OutputStream) {
        val rows = mutableListOf<List<CellValue>>()
        rows.add(
            listOf(
                CellValue.Text("No", bold = true),
                CellValue.Text("NIS", bold = true),
                CellValue.Text("Nama Lengkap", bold = true),
                CellValue.Text("Jenis Kelamin", bold = true),
                CellValue.Text("Kelas", bold = true)
            )
        )
        students.forEachIndexed { idx, s ->
            rows.add(
                listOf(
                    CellValue.Number((idx + 1).toDouble()),
                    CellValue.Text(s.nis),
                    CellValue.Text(s.name),
                    CellValue.Text(s.gender),
                    CellValue.Number(s.classId.toDouble())
                )
            )
        }
        writeXlsx("Data Siswa", rows, listOf(8.0, 18.0, 34.0, 16.0, 12.0), out)
    }

    fun writeRecapReport(
        settings: SettingsEntity,
        classLabel: String,
        periodLabel: String,
        report: RecapReportData,
        out: OutputStream
    ) {
        val rows = mutableListOf<List<CellValue>>()
        rows.add(listOf(CellValue.Text("LAPORAN KEHADIRAN SISWA MATA PELAJARAN PJOK", bold = true)))
        rows.add(listOf(CellValue.Text(settings.schoolName.uppercase(), bold = true)))
        rows.add(listOf(CellValue.Blank))
        rows.add(listOf(CellValue.Text("Guru PJOK", bold = true), CellValue.Text(settings.teacherName)))
        rows.add(listOf(CellValue.Text("Mata Pelajaran", bold = true), CellValue.Text("PJOK")))
        rows.add(listOf(CellValue.Text("Kelas", bold = true), CellValue.Text(classLabel)))
        rows.add(listOf(CellValue.Text("Periode", bold = true), CellValue.Text(periodLabel)))
        rows.add(
            listOf(
                CellValue.Text("Semester / Tahun Pelajaran", bold = true),
                CellValue.Text("${settings.semester} / ${settings.academicYear}")
            )
        )
        rows.add(listOf(CellValue.Blank))
        rows.add(
            listOf(
                CellValue.Text("No", bold = true),
                CellValue.Text("NIS", bold = true),
                CellValue.Text("Nama Siswa", bold = true),
                CellValue.Text("Kelas", bold = true),
                CellValue.Text("Hadir", bold = true),
                CellValue.Text("Sakit", bold = true),
                CellValue.Text("Izin", bold = true),
                CellValue.Text("Alpa", bold = true),
                CellValue.Text("Total Pertemuan", bold = true),
                CellValue.Text("Persentase (%)", bold = true)
            )
        )

        report.studentRows.forEach { r ->
            rows.add(
                listOf(
                    CellValue.Number(r.number.toDouble()),
                    CellValue.Text(r.student.nis),
                    CellValue.Text(r.student.name),
                    CellValue.Text(r.className),
                    CellValue.Number(r.hadir.toDouble()),
                    CellValue.Number(r.sakit.toDouble()),
                    CellValue.Number(r.izin.toDouble()),
                    CellValue.Number(r.alpa.toDouble()),
                    CellValue.Number(r.totalMeetings.toDouble()),
                    CellValue.Text(String.format(Locale.US, "%.1f%%", r.percentage))
                )
            )
        }

        rows.add(listOf(CellValue.Blank))
        rows.add(
            listOf(
                CellValue.Text("Total", bold = true),
                CellValue.Blank,
                CellValue.Text("${report.studentRows.size} Siswa", bold = true),
                CellValue.Blank,
                CellValue.Number(report.totalHadir.toDouble(), bold = true),
                CellValue.Number(report.totalSakit.toDouble(), bold = true),
                CellValue.Number(report.totalIzin.toDouble(), bold = true),
                CellValue.Number(report.totalAlpa.toDouble(), bold = true),
                CellValue.Number(report.totalFinalizedMeetings.toDouble(), bold = true),
                CellValue.Text(String.format(Locale.US, "%.1f%%", report.overallPercentage), bold = true)
            )
        )
        rows.add(listOf(CellValue.Blank))
        rows.add(
            listOf(
                CellValue.Blank, CellValue.Blank, CellValue.Blank, CellValue.Blank,
                CellValue.Blank, CellValue.Blank,
                CellValue.Text("Lokojuli, ${DateUtils.formatIndonesianShort(DateUtils.todayIso())}")
            )
        )
        rows.add(
            listOf(
                CellValue.Blank, CellValue.Blank, CellValue.Blank, CellValue.Blank,
                CellValue.Blank, CellValue.Blank,
                CellValue.Text("Guru Mata Pelajaran PJOK")
            )
        )
        rows.add(listOf(CellValue.Blank))
        rows.add(listOf(CellValue.Blank))
        rows.add(
            listOf(
                CellValue.Blank, CellValue.Blank, CellValue.Blank, CellValue.Blank,
                CellValue.Blank, CellValue.Blank,
                CellValue.Text(settings.teacherName, bold = true)
            )
        )

        writeXlsx(
            "Rekap PJOK",
            rows,
            listOf(6.0, 15.0, 30.0, 12.0, 9.0, 9.0, 9.0, 9.0, 16.0, 16.0),
            out
        )
    }

    fun readStudentsFromStream(input: InputStream, fileNameHint: String = ""): ParsedImportData {
        val bytes = input.readBytes()
        val isZip = bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        if (!isZip || fileNameHint.endsWith(".csv", ignoreCase = true)) {
            return parseCsvStudents(bytes.decodeToString())
        }
        return parseXlsxStudents(bytes)
    }

    private fun parseCsvStudents(csvText: String): ParsedImportData {
        val rows = mutableListOf<ImportStudentInput>()
        val errors = mutableListOf<String>()
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParsedImportData(emptyList(), listOf("File kosong."))

        val delimiter = if (lines.first().contains(";")) ';' else ','
        val parsedLines = lines.map { line -> line.split(delimiter).map { it.trim().trim('"') } }
        return mapRawTableToStudents(parsedLines, errors)
    }

    private fun parseXlsxStudents(zipBytes: ByteArray): ParsedImportData {
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                        sharedStrings.addAll(parseSharedStrings(zis.readBytes()))
                    }
                    name.equals("xl/worksheets/sheet1.xml", ignoreCase = true) ||
                        (sheetBytes == null && name.startsWith("xl/worksheets/sheet", ignoreCase = true) && name.endsWith(".xml", ignoreCase = true)) -> {
                        sheetBytes = zis.readBytes()
                    }
                }
                entry = zis.nextEntry
            }
        }

        val targetSheet = sheetBytes
            ?: return ParsedImportData(emptyList(), listOf("Sheet Excel tidak ditemukan di dalam file .xlsx."))

        val table = parseSheetXml(targetSheet, sharedStrings)
        return mapRawTableToStudents(table, mutableListOf())
    }

    private fun mapRawTableToStudents(
        table: List<List<String>>,
        errors: MutableList<String>
    ): ParsedImportData {
        if (table.isEmpty()) return ParsedImportData(emptyList(), listOf("Tidak ada baris data."))

        val header = table.first().map { it.lowercase() }
        val hasHeader = header.any { it.contains("nis") || it.contains("nama") || it.contains("kelas") }
        val startIndex = if (hasHeader) 1 else 0

        // Detect column positions (default template: NIS=0, Nama=1, JK=2, Kelas=3; or export with No=0)
        var colNis = header.indexOfFirst { it.contains("nis") }
        var colName = header.indexOfFirst { it.contains("nama") }
        var colGender = header.indexOfFirst { it.contains("kelamin") || it == "jk" || it == "l/p" || it.contains("gender") }
        var colClass = header.indexOfFirst { it.contains("kelas") || it == "class" }

        if (!hasHeader || colNis < 0 || colName < 0) {
            colNis = 0
            colName = 1
            colGender = 2
            colClass = 3
        } else {
            if (colGender < 0) colGender = colName + 1
            if (colClass < 0) colClass = colGender + 1
        }

        val out = mutableListOf<ImportStudentInput>()
        for (i in startIndex until table.size) {
            val row = table[i]
            if (row.all { it.isBlank() }) continue

            val rawNis = row.getOrNull(colNis)?.trim().orEmpty()
            val rawName = row.getOrNull(colName)?.trim().orEmpty()
            val rawGender = row.getOrNull(colGender)?.trim().orEmpty()
            val rawClass = row.getOrNull(colClass)?.trim().orEmpty()

            // Skip example row if left unchanged from template
            if (rawName.startsWith("Contoh:", ignoreCase = true)) continue

            if (rawNis.isEmpty() || rawName.isEmpty()) {
                errors.add("Baris ${i + 1}: NIS atau Nama kosong.")
                continue
            }

            val normalizedNis = if (rawNis.endsWith(".0")) rawNis.dropLast(2) else rawNis
            val gender = Gender.fromInput(rawGender)
            if (gender == null) {
                errors.add("Baris ${i + 1} ($rawName): Jenis kelamin '$rawGender' harus L atau P.")
                continue
            }

            val classNum = extractClassNumber(rawClass)
            if (classNum !in 1..6) {
                errors.add("Baris ${i + 1} ($rawName): Kelas '$rawClass' harus 1 sampai 6.")
                continue
            }

            out.add(
                ImportStudentInput(
                    nis = normalizedNis,
                    name = rawName,
                    gender = gender.code,
                    classId = classNum
                )
            )
        }
        return ParsedImportData(out, errors)
    }

    private fun extractClassNumber(raw: String): Int {
        val digits = raw.replace(".0", "").filter { it.isDigit() }
        return digits.toIntOrNull() ?: -1
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var inSi = false
        var inT = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            sb.clear()
                        }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inT) sb.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inT = false
                        "si" -> {
                            inSi = false
                            list.add(sb.toString())
                        }
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var currentRow: MutableMap<Int, String>? = null
        var currentColIdx = 0
        var currentCellType = ""
        var inValue = false
        var inInlineText = false
        val textBuf = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableMapOf()
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            currentColIdx = if (ref.isNotEmpty()) colRefToIndex(ref) else (currentRow?.size ?: 0)
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            textBuf.clear()
                        }
                        "v" -> inValue = true
                        "t" -> inInlineText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue || inInlineText) textBuf.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inValue = false
                        "t" -> inInlineText = false
                        "c" -> {
                            val raw = textBuf.toString()
                            val resolved = if (currentCellType == "s") {
                                val idx = raw.toIntOrNull() ?: -1
                                sharedStrings.getOrElse(idx) { "" }
                            } else {
                                raw
                            }
                            currentRow?.set(currentColIdx, resolved)
                        }
                        "row" -> {
                            currentRow?.let { map ->
                                if (map.isNotEmpty()) {
                                    val maxCol = map.keys.maxOrNull() ?: 0
                                    val list = (0..maxCol).map { map[it].orEmpty() }
                                    rows.add(list)
                                }
                            }
                            currentRow = null
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun colRefToIndex(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch.isLetter()) {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun writeXlsx(
        sheetName: String,
        rows: List<List<CellValue>>,
        colWidths: List<Double>,
        out: OutputStream
    ) {
        ZipOutputStream(out).use { zos ->
            fun addEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
            )

            addEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )

            addEntry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(sheetName.take(31))}" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
            )

            addEntry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            )

            addEntry(
                "xl/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
            )

            val sheetXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
                if (colWidths.isNotEmpty()) {
                    append("<cols>")
                    colWidths.forEachIndexed { idx, w ->
                        val c = idx + 1
                        append("""<col min="$c" max="$c" width="$w" customWidth="1"/>""")
                    }
                    append("</cols>")
                }
                append("<sheetData>")
                rows.forEachIndexed { rIdx, row ->
                    val rNum = rIdx + 1
                    append("""<row r="$rNum">""")
                    row.forEachIndexed { cIdx, cell ->
                        val ref = "${indexToColRef(cIdx)}$rNum"
                        when (cell) {
                            CellValue.Blank -> Unit
                            is CellValue.Text -> {
                                val style = if (cell.bold) "1" else "0"
                                append("""<c r="$ref" s="$style" t="inlineStr"><is><t>${escapeXml(cell.value)}</t></is></c>""")
                            }
                            is CellValue.Number -> {
                                val style = if (cell.bold) "1" else "0"
                                val numStr = if (cell.value % 1.0 == 0.0) cell.value.toLong().toString() else cell.value.toString()
                                append("""<c r="$ref" s="$style"><v>$numStr</v></c>""")
                            }
                        }
                    }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            }
            addEntry("xl/worksheets/sheet1.xml", sheetXml)
        }
    }

    private fun indexToColRef(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A' + rem))
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
