package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SettingsEntity
import com.example.data.repository.ClassRecapSummary
import com.example.data.repository.RecapPeriod
import com.example.data.repository.RecapReportData
import com.example.data.repository.StudentRecapRow
import com.example.ui.components.AttendanceDonutRing
import com.example.ui.components.ClassAttendanceBarChart
import com.example.ui.components.IndonesianDatePickerModal
import com.example.ui.theme.BackgroundLightGray
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusAlpaFg
import com.example.ui.theme.StatusHadirFg
import com.example.ui.theme.StatusIzinFg
import com.example.ui.theme.StatusSakitFg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PjokViewModel
import com.example.ui.viewmodel.RecapFilterState
import com.example.util.DateUtils
import com.example.util.PdfReportHelper
import java.util.Calendar
import java.util.Locale

@Composable
fun RecapScreen(
    filter: RecapFilterState,
    report: RecapReportData,
    settings: SettingsEntity,
    viewModel: PjokViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var viewTab by remember { mutableIntStateOf(0) } // 0 = Per Siswa, 1 = Per Kelas
    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val excelReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { out ->
                viewModel.exportRecapExcelToStream(out) { ok ->
                    viewModel.emitMessage(
                        if (ok) "Laporan Excel berhasil disimpan." else "Gagal menyimpan laporan Excel."
                    )
                }
            }
        }
    }

    val pdfReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { out ->
                viewModel.exportRecapPdfToStream(out) { ok ->
                    viewModel.emitMessage(
                        if (ok) "Laporan PDF A4 berhasil disimpan." else "Gagal menyimpan laporan PDF."
                    )
                }
            }
        }
    }

    val activeSemester = filter.selectedSemester ?: settings.semester
    val activeYear = filter.selectedAcademicYear ?: settings.academicYear
    val (classLabel, periodLabel) = viewModel.currentRecapLabels()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLightGray),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Filters
        item {
            Surface(color = NavyPrimary, shadowElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Rekap & Laporan PJOK",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$classLabel • $periodLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Period Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecapPeriod.entries.forEach { p ->
                            FilterChip(
                                selected = filter.period == p,
                                onClick = { viewModel.updateRecapPeriod(p) },
                                label = { Text(p.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    labelColor = Color.White,
                                    selectedContainerColor = EmeraldAccent,
                                    selectedLabelColor = Color.White
                                ),
                                border = null
                            )
                        }
                    }

                    // Secondary Period Selector (Date / Month / Semester)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (filter.period) {
                            RecapPeriod.HARIAN, RecapPeriod.MINGGUAN -> {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.height(44.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (filter.period == RecapPeriod.HARIAN) {
                                            "Tanggal: ${DateUtils.formatIndonesianShort(filter.referenceDateIso)}"
                                        } else {
                                            val (s, e) = DateUtils.weekRangeFor(filter.referenceDateIso)
                                            "Minggu: ${DateUtils.formatIndonesianShort(s)} - ${DateUtils.formatIndonesianShort(e)}"
                                        }
                                    )
                                }
                            }
                            RecapPeriod.BULANAN -> {
                                OutlinedButton(
                                    onClick = { showMonthPicker = true },
                                    modifier = Modifier.height(44.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Bulan: ${DateUtils.formatMonthYear(filter.selectedYearMonth)}")
                                }
                            }
                            RecapPeriod.SEMESTER -> {
                                listOf("Ganjil", "Genap").forEach { sem ->
                                    FilterChip(
                                        selected = activeSemester == sem,
                                        onClick = { viewModel.updateRecapSemester(sem) },
                                        label = { Text("Semester $sem") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color.White.copy(alpha = 0.12f),
                                            labelColor = Color.White,
                                            selectedContainerColor = Color.White,
                                            selectedLabelColor = NavyPrimary
                                        ),
                                        border = null
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = activeYear,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Class Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filter.selectedClassId == null,
                            onClick = { viewModel.updateRecapClassFilter(null) },
                            label = { Text("Semua Kelas") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                labelColor = Color.White,
                                selectedContainerColor = Color.White,
                                selectedLabelColor = NavyPrimary
                            ),
                            border = null
                        )
                        (1..6).forEach { cls ->
                            FilterChip(
                                selected = filter.selectedClassId == cls,
                                onClick = { viewModel.updateRecapClassFilter(cls) },
                                label = { Text("Kelas $cls") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    labelColor = Color.White,
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor = NavyPrimary
                                ),
                                border = null
                            )
                        }
                    }
                }
            }
        }

        // Report Export Buttons (Excel, PDF A4, Print)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        excelReportLauncher.launch("Laporan_Absensi_PJOK_${DateUtils.timestampFileSuffix()}.xlsx")
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("export_recap_excel_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Excel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        pdfReportLauncher.launch("Laporan_Absensi_PJOK_${DateUtils.timestampFileSuffix()}.pdf")
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("export_recap_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF A4", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        PdfReportHelper.printReport(
                            context = context,
                            jobName = "Laporan_PJOK_${DateUtils.timestampFileSuffix()}",
                            settings = settings,
                            classLabel = classLabel,
                            periodLabel = periodLabel,
                            report = report
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("print_recap_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NavyPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Overall Attendance Summary Card with Donut Ring
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, OutlineSubtle),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttendanceDonutRing(percentage = report.overallPercentage)
                    Spacer(Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ringkasan Kehadiran",
                            style = MaterialTheme.typography.titleSmall,
                            color = NavyPrimary
                        )
                        Text(
                            text = "${report.totalFinalizedMeetings} pertemuan terfinalisasi • ${report.studentRows.size} siswa",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("H: ${report.totalHadir}", style = MaterialTheme.typography.labelMedium, color = StatusHadirFg, fontWeight = FontWeight.Bold)
                            Text("S: ${report.totalSakit}", style = MaterialTheme.typography.labelMedium, color = StatusSakitFg, fontWeight = FontWeight.Bold)
                            Text("I: ${report.totalIzin}", style = MaterialTheme.typography.labelMedium, color = StatusIzinFg, fontWeight = FontWeight.Bold)
                            Text("A: ${report.totalAlpa}", style = MaterialTheme.typography.labelMedium, color = StatusAlpaFg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Modern Canvas Bar Chart
        item {
            ClassAttendanceBarChart(
                summaries = report.classSummaries,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Mode Switch: Per Siswa vs Per Kelas
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = viewTab == 0,
                    onClick = { viewTab = 0 },
                    label = { Text("Tabel Per Siswa (${report.studentRows.size})") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = viewTab == 1,
                    onClick = { viewTab = 1 },
                    label = { Text("Rekap Per Kelas (${report.classSummaries.size})") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (viewTab == 0) {
            item {
                StudentRecapTableCard(
                    rows = report.studentRows,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(report.classSummaries, key = { it.schoolClass.id }) { summary ->
                ClassSummaryRecapCard(
                    summary = summary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        IndonesianDatePickerModal(
            initialIsoDate = filter.referenceDateIso,
            onDismiss = { showDatePicker = false },
            onDateSelected = { viewModel.updateRecapDate(it) }
        )
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentYearMonth = filter.selectedYearMonth,
            onDismiss = { showMonthPicker = false },
            onSelect = { ym ->
                viewModel.updateRecapMonth(ym)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun StudentRecapTableCard(
    rows: List<StudentRecapRow>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle),
        shadowElevation = 2.dp
    ) {
        if (rows.isEmpty()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Belum ada data rekap siswa", style = MaterialTheme.typography.titleSmall, color = NavyPrimary)
                Text(
                    "Pastikan siswa sudah terdaftar dan pertemuan telah difinalisasi pada periode ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val hScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .background(NavyPrimary)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeaderCell("No", 34.dp)
                    TableHeaderCell("NIS", 88.dp)
                    TableHeaderCell("Nama", 160.dp, TextAlign.Start)
                    TableHeaderCell("Hadir", 52.dp)
                    TableHeaderCell("Sakit", 52.dp)
                    TableHeaderCell("Izin", 52.dp)
                    TableHeaderCell("Alpa", 52.dp)
                    TableHeaderCell("Total Pertemuan", 104.dp)
                    TableHeaderCell("Persentase", 82.dp)
                }

                rows.forEachIndexed { idx, row ->
                    Row(
                        modifier = Modifier
                            .background(if (idx % 2 == 1) BackgroundLightGray else SurfaceCard)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableBodyCell("${row.number}", 34.dp, NavyPrimary)
                        TableBodyCell(row.student.nis, 88.dp, TextSecondary)
                        Text(
                            text = row.student.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NavyPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(160.dp)
                        )
                        TableBodyCell("${row.hadir}", 52.dp, StatusHadirFg, bold = true)
                        TableBodyCell("${row.sakit}", 52.dp, StatusSakitFg, bold = true)
                        TableBodyCell("${row.izin}", 52.dp, StatusIzinFg, bold = true)
                        TableBodyCell("${row.alpa}", 52.dp, StatusAlpaFg, bold = true)
                        TableBodyCell("${row.totalMeetings}", 104.dp, NavyPrimary)
                        TableBodyCell(
                            String.format(Locale.US, "%.1f%%", row.percentage),
                            82.dp,
                            if (row.percentage >= 75.0) StatusHadirFg else StatusAlpaFg,
                            bold = true
                        )
                    }
                    HorizontalDivider(color = OutlineSubtle, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    align: TextAlign = TextAlign.Center
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = align,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun TableBodyCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color,
    bold: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun ClassSummaryRecapCard(
    summary: ClassRecapSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = summary.schoolClass.className,
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.studentCount} siswa • ${summary.finalizedMeetingsCount} pertemuan final",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", summary.percentage),
                        style = MaterialTheme.typography.titleSmall,
                        color = OnEmeraldContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (summary.percentage.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = EmeraldAccent,
                trackColor = NavyContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Hadir: ${summary.totalHadir}", style = MaterialTheme.typography.labelMedium, color = StatusHadirFg)
                Text("Sakit: ${summary.totalSakit}", style = MaterialTheme.typography.labelMedium, color = StatusSakitFg)
                Text("Izin: ${summary.totalIzin}", style = MaterialTheme.typography.labelMedium, color = StatusIzinFg)
                Text("Alpa: ${summary.totalAlpa}", style = MaterialTheme.typography.labelMedium, color = StatusAlpaFg)
            }
        }
    }
}

@Composable
private fun MonthYearPickerDialog(
    currentYearMonth: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedYear by remember {
        mutableIntStateOf(currentYearMonth.substringBefore("-").toIntOrNull() ?: currentYear)
    }
    val months = DateUtils.getAllMonthNames()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Bulan Rekap", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { selectedYear-- }) { Text("< ${selectedYear - 1}") }
                    Text(
                        text = "$selectedYear",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    TextButton(onClick = { selectedYear++ }) { Text("${selectedYear + 1} >") }
                }
                months.chunked(3).forEachIndexed { rowIdx, triplet ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        triplet.forEachIndexed { colIdx, mName ->
                            val monthNum = rowIdx * 3 + colIdx + 1
                            val ym = String.format(Locale.US, "%04d-%02d", selectedYear, monthNum)
                            val isSelected = ym == currentYearMonth
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable { onSelect(ym) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NavyPrimary else BackgroundLightGray,
                                border = BorderStroke(1.dp, OutlineSubtle)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = mName.take(3),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) Color.White else NavyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
