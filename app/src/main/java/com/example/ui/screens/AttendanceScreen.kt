package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AttendanceStatus
import com.example.data.local.MeetingEntity
import com.example.data.local.MeetingStatus
import com.example.data.local.SettingsEntity
import com.example.data.repository.AttendanceDraftItem
import com.example.data.repository.SaveResult
import com.example.ui.components.IndonesianDatePickerModal
import com.example.ui.components.SearchInputBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.statusColors
import com.example.ui.theme.BackgroundLightGray
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusAlpaBg
import com.example.ui.theme.StatusAlpaFg
import com.example.ui.theme.StatusHadirBg
import com.example.ui.theme.StatusHadirFg
import com.example.ui.theme.StatusIzinBg
import com.example.ui.theme.StatusIzinFg
import com.example.ui.theme.StatusSakitBg
import com.example.ui.theme.StatusSakitFg
import com.example.ui.theme.StatusUnmarkedBg
import com.example.ui.theme.StatusUnmarkedFg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AttendanceEditorState
import com.example.ui.viewmodel.PjokViewModel
import com.example.util.DateUtils

@Composable
fun AttendanceScreen(
    state: AttendanceEditorState,
    settings: SettingsEntity,
    viewModel: PjokViewModel,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    var finalizeToggle by remember(state.editingMeetingId, state.isFinalized) {
        mutableStateOf(state.isFinalized)
    }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }
    var noteEditingItem by remember { mutableStateOf<AttendanceDraftItem?>(null) }

    val quickTopics = listOf(
        "Permainan Bola Besar",
        "Atletik & Lari",
        "Senam Lantai",
        "Kebugaran Jasmani",
        "Permainan Bola Kecil"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
    ) {
        // Header + Class Tabs
        Surface(color = NavyPrimary, shadowElevation = 4.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (state.editingMeetingId != null) {
                                "Edit Absensi Kelas ${state.selectedClassId}"
                            } else {
                                "Absensi PJOK"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${settings.semester} • ${settings.academicYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.editingMeetingId != null) {
                            OutlinedButton(
                                onClick = { viewModel.loadAttendanceForClass(state.selectedClassId, null) },
                                modifier = Modifier.height(44.dp),
                                border = BorderStroke(1.dp, EmeraldAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("Baru")
                            }
                        }
                        OutlinedButton(
                            onClick = { showHistoryDialog = true },
                            modifier = Modifier.height(44.dp).testTag("meeting_history_button"),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Riwayat (${state.classMeetingsHistory.size})")
                        }
                    }
                }

                // Class Selector (Kelas 1 - 6)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..6).forEach { cls ->
                        FilterChip(
                            selected = state.selectedClassId == cls,
                            onClick = { viewModel.loadAttendanceForClass(cls, null) },
                            label = { Text("Kelas $cls", fontWeight = FontWeight.SemiBold) },
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
            }
        }

        // Main Scrollable Body
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Meeting Details Card (Pertemuan PJOK)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, OutlineSubtle),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date Picker Trigger
                            Surface(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(52.dp)
                                    .clickable { showDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                color = BackgroundLightGray,
                                border = BorderStroke(1.dp, OutlineSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Pilih tanggal",
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Tanggal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = DateUtils.formatIndonesianShort(state.selectedDate),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = NavyPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Meeting Number Stepper
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = BackgroundLightGray,
                                border = BorderStroke(1.dp, OutlineSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { viewModel.setMeetingNumber(state.meetingNumber - 1) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kurangi pertemuan", tint = NavyPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Pertemuan", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(
                                            text = "Ke-${state.meetingNumber}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = NavyPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.setMeetingNumber(state.meetingNumber + 1) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah pertemuan", tint = NavyPrimary)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.topic,
                            onValueChange = { viewModel.setMeetingTopic(it) },
                            label = { Text("Materi PJOK") },
                            placeholder = { Text("Contoh: Sepak Bola / Senam Lantai") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("meeting_topic_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickTopics.forEach { t ->
                                Surface(
                                    modifier = Modifier.clickable { viewModel.setMeetingTopic(t) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (state.topic == t) NavyContainer else BackgroundLightGray,
                                    border = BorderStroke(1.dp, OutlineSubtle)
                                ) {
                                    Text(
                                        text = t,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NavyPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { viewModel.setMeetingNotes(it) },
                            label = { Text("Catatan Pertemuan (Opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Live Counter Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveCountChip("Hadir", state.hadirCount, StatusHadirBg, StatusHadirFg, Modifier.weight(1f))
                    LiveCountChip("Sakit", state.sakitCount, StatusSakitBg, StatusSakitFg, Modifier.weight(1f))
                    LiveCountChip("Izin", state.izinCount, StatusIzinBg, StatusIzinFg, Modifier.weight(1f))
                    LiveCountChip("Alpa", state.alpaCount, StatusAlpaBg, StatusAlpaFg, Modifier.weight(1f))
                    LiveCountChip("Belum", state.unmarkedCount, StatusUnmarkedBg, StatusUnmarkedFg, Modifier.weight(1f))
                }
            }

            // Quick Actions + Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.markAllHadir() },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("hadir_semua_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hadir Semua", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetAllUnmarked() },
                            modifier = Modifier.weight(0.75f).height(48.dp).testTag("reset_attendance_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OutlineSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }

                    SearchInputBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.setAttendanceSearch(it) },
                        placeholder = "Cari nama siswa di Kelas ${state.selectedClassId}..."
                    )
                }
            }

            // Student Attendance List
            if (state.items.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, OutlineSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Belum ada siswa di Kelas ${state.selectedClassId}",
                                style = MaterialTheme.typography.titleSmall,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Silakan tambahkan atau impor data siswa di tab Siswa terlebih dahulu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(state.filteredItems, key = { it.student.id }) { item ->
                    StudentAttendanceCard(
                        item = item,
                        onSelectStatus = { newStatus ->
                            viewModel.setStudentStatus(item.student.id, newStatus)
                        },
                        onEditNote = { noteEditingItem = item }
                    )
                }
            }
        }

        // Sticky Save Bottom Bar
        Surface(
            color = SurfaceCard,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, OutlineSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${state.items.size - state.unmarkedCount}/${state.items.size} siswa ditandai",
                        style = MaterialTheme.typography.titleSmall,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.unmarkedCount > 0) {
                            "${state.unmarkedCount} siswa masih BELUM DIABSEN"
                        } else {
                            "Semua siswa sudah ditandai"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.unmarkedCount > 0) TextMuted else StatusHadirFg
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        saveErrorMessage = null
                        showSaveConfirm = true
                    },
                    enabled = state.items.isNotEmpty(),
                    modifier = Modifier.height(50.dp).testTag("save_attendance_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (state.editingMeetingId != null) "Perbarui" else "Simpan Absensi",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        IndonesianDatePickerModal(
            initialIsoDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { viewModel.setAttendanceDate(it) }
        )
    }

    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = {
                Text(
                    text = if (state.editingMeetingId != null) "Konfirmasi Perbarui Absensi" else "Konfirmasi Simpan Absensi",
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kelas ${state.selectedClassId} • Pertemuan ke-${state.meetingNumber}")
                    Text("Tanggal: ${DateUtils.formatIndonesianFull(state.selectedDate)}")
                    Text("Materi: ${state.topic.ifBlank { "Pembelajaran PJOK" }}")
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BackgroundLightGray
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Hadir: ${state.hadirCount}  |  Sakit: ${state.sakitCount}  |  Izin: ${state.izinCount}  |  Alpa: ${state.alpaCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NavyPrimary
                            )
                            if (state.unmarkedCount > 0) {
                                Text(
                                    text = "Belum Diabsen: ${state.unmarkedCount} siswa (tetap tersimpan sebagai BELUM DIABSEN)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Finalisasi Pertemuan", style = MaterialTheme.typography.labelLarge, color = NavyPrimary)
                            Text(
                                "Pertemuan terfinalisasi dihitung dalam persentase Rekap Kehadiran.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = finalizeToggle,
                            onCheckedChange = { finalizeToggle = it }
                        )
                    }
                    saveErrorMessage?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = StatusAlpaFg, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveAttendance(finalizeToggle) { res ->
                            when (res) {
                                is SaveResult.Success -> showSaveConfirm = false
                                is SaveResult.Error -> saveErrorMessage = res.message
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_attendance_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ya, Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    noteEditingItem?.let { item ->
        var noteText by remember(item.student.id) { mutableStateOf(item.note) }
        AlertDialog(
            onDismissRequest = { noteEditingItem = null },
            title = { Text("Catatan Siswa: ${item.student.name}") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Keterangan / Catatan") },
                    placeholder = { Text("Misal: Surat izin orang tua, cedera ringan") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setStudentNote(item.student.id, noteText)
                        noteEditingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Simpan Catatan")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteEditingItem = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showHistoryDialog) {
        MeetingHistoryDialog(
            classId = state.selectedClassId,
            meetings = state.classMeetingsHistory,
            onDismiss = { showHistoryDialog = false },
            onSelectMeeting = { m ->
                showHistoryDialog = false
                viewModel.loadAttendanceForClass(m.classId, m.id)
            },
            onDeleteMeeting = { m ->
                viewModel.deleteMeeting(m.id)
            }
        )
    }
}

@Composable
private fun LiveCountChip(
    label: String,
    count: Int,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                color = fgColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StudentAttendanceCard(
    item: AttendanceDraftItem,
    onSelectStatus: (AttendanceStatus) -> Unit,
    onEditNote: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.student.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = NavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "NIS: ${item.student.nis} • ${if (item.student.gender == "L") "Laki-laki" else "Perempuan"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(status = item.status)
            }

            if (item.note.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundLightGray
                ) {
                    Text(
                        text = "Catatan: ${item.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // One-tap 4 status buttons + Note button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val options = listOf(
                    AttendanceStatus.H,
                    AttendanceStatus.S,
                    AttendanceStatus.I,
                    AttendanceStatus.A
                )
                options.forEach { st ->
                    val selected = item.status == st
                    val (bg, fg, border) = statusColors(st)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable {
                                // Tap active status again to toggle back to UNMARKED if needed
                                onSelectStatus(if (selected) AttendanceStatus.UNMARKED else st)
                            }
                            .testTag("att_btn_${item.student.id}_${st.code}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) fg else bg.copy(alpha = 0.6f),
                        border = BorderStroke(1.2.dp, if (selected) fg else border)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = st.code,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else fg
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onEditNote),
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.note.isNotBlank()) EmeraldContainer else BackgroundLightGray,
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = "Catatan untuk ${item.student.name}",
                            tint = if (item.note.isNotBlank()) OnEmeraldContainer else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetingHistoryDialog(
    classId: Int,
    meetings: List<MeetingEntity>,
    onDismiss: () -> Unit,
    onSelectMeeting: (MeetingEntity) -> Unit,
    onDeleteMeeting: (MeetingEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Riwayat Pertemuan Kelas $classId", color = NavyPrimary, fontWeight = FontWeight.Bold) },
        text = {
            if (meetings.isEmpty()) {
                Text("Belum ada pertemuan tersimpan untuk Kelas $classId pada semester aktif.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(meetings, key = { it.id }) { m ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectMeeting(m) },
                            shape = RoundedCornerShape(12.dp),
                            color = BackgroundLightGray,
                            border = BorderStroke(1.dp, OutlineSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pertemuan ke-${m.meetingNumber} (${if (m.status == MeetingStatus.FINALIZED.code) "Final" else "Draft"})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "${DateUtils.formatIndonesianShort(m.date)} • ${m.topic.ifBlank { "PJOK" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onSelectMeeting(m) }) {
                                    Icon(Icons.Default.EditNote, contentDescription = "Edit pertemuan", tint = NavyPrimary)
                                }
                                IconButton(onClick = { onDeleteMeeting(m) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus pertemuan", tint = StatusAlpaFg)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
