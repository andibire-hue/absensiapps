package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SettingsEntity
import com.example.data.repository.SaveResult
import com.example.ui.theme.BackgroundLightGray
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusAlpaFg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PjokViewModel
import com.example.util.DateUtils
import com.example.util.FileExportHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SettingsEntity,
    viewModel: PjokViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var schoolName by remember(settings.schoolName) { mutableStateOf(settings.schoolName) }
    var teacherName by remember(settings.teacherName) { mutableStateOf(settings.teacherName) }
    var academicYear by remember(settings.academicYear) { mutableStateOf(settings.academicYear) }
    var semester by remember(settings.semester) { mutableStateOf(settings.semester) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { out ->
                viewModel.exportBackupToStream(out) { ok ->
                    viewModel.emitMessage(
                        if (ok) "Backup JSON berhasil disimpan." else "Gagal mengekspor backup JSON."
                    )
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
    ) {
        Surface(color = NavyPrimary, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Beranda",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Pengaturan & Backup",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Identitas sekolah, guru, dan cadangan database",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, OutlineSubtle),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Identitas Sekolah & Guru PJOK",
                            style = MaterialTheme.typography.titleMedium,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = schoolName,
                            onValueChange = { schoolName = it },
                            label = { Text("Nama Sekolah") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { teacherName = it },
                            label = { Text("Nama Guru PJOK") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = academicYear,
                            onValueChange = { academicYear = it },
                            label = { Text("Tahun Pelajaran") },
                            placeholder = { Text("2026/2027") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Semester Aktif", style = MaterialTheme.typography.labelMedium, color = NavyPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Ganjil", "Genap").forEach { sem ->
                                    FilterChip(
                                        selected = semester == sem,
                                        onClick = { semester = sem },
                                        label = { Text("Semester $sem") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NavyPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                viewModel.saveSettings(teacherName, schoolName, academicYear, semester)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_settings_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan Pengaturan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, OutlineSubtle),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = NavyPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Backup & Restore Data (JSON)",
                                style = MaterialTheme.typography.titleMedium,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Seluruh data siswa, kelas, pertemuan PJOK, catatan kehadiran, dan pengaturan tersimpan secara offline di SQLite perangkat ini. Lakukan backup berkala untuk keamanan data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Button(
                            onClick = {
                                exportBackupLauncher.launch("Backup_Absensi_PJOK_${DateUtils.timestampFileSuffix()}.json")
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("export_backup_json_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ekspor Backup JSON ke Perangkat", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = viewModel.generateBackupJsonString()
                                    FileExportHelper.shareGeneratedFile(
                                        context = context,
                                        fileName = "Backup_Absensi_PJOK_${DateUtils.timestampFileSuffix()}.json",
                                        mimeType = "application/json"
                                    ) { file ->
                                        file.writeText(json, Charsets.UTF_8)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NavyPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bagikan Backup JSON", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("restore_backup_json_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StatusAlpaFg),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlpaFg)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Impor & Restore dari Backup JSON", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Tentang Aplikasi", style = MaterialTheme.typography.titleSmall, color = NavyPrimary)
                        Text(
                            text = "ABSENSI PJOK — UPTD SDN LOKOJULI\nPenyimpanan Offline SQLite (Room)\nPackage ID: id.sch.lokojuli.absensipjok",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Konfirmasi Restore Data?", color = StatusAlpaFg, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Proses Restore akan MENIMPA seluruh data siswa, pertemuan, dan absensi yang ada saat ini dengan data dari file backup JSON. Pastikan Anda sudah membuat backup data terbaru sebelum melanjutkan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri = null
                        context.contentResolver.openInputStream(uri)?.let { input ->
                            viewModel.restoreBackupFromStream(input) { res ->
                                when (res) {
                                    is SaveResult.Success -> viewModel.emitMessage(res.message)
                                    is SaveResult.Error -> viewModel.emitMessage(res.message)
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAlpaFg)
                ) {
                    Text("Ya, Timpa & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
