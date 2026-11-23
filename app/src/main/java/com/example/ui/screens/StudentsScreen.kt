package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.ImeAction
import com.example.data.local.StudentEntity
import com.example.data.repository.ImportResult
import com.example.ui.components.SearchInputBar
import com.example.ui.theme.BackgroundLightGray
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusAlpaFg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PjokViewModel
import com.example.util.DateUtils

@Composable
fun StudentsScreen(
    students: List<StudentEntity>,
    viewModel: PjokViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf<Int?>(null) }

    var showStudentForm by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var deletingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var detailStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var importResultDialog by remember { mutableStateOf<ImportResult?>(null) }

    val classCounts = remember(students) {
        students.groupingBy { it.classId }.eachCount()
    }

    val filteredStudents = remember(students, searchQuery, selectedClassFilter) {
        students.filter { s ->
            val matchClass = selectedClassFilter == null || s.classId == selectedClassFilter
            val q = searchQuery.trim().lowercase()
            val matchQuery = q.isEmpty() || s.name.lowercase().contains(q) || s.nis.lowercase().contains(q)
            matchClass && matchQuery
        }
    }

    val maleCount = remember(filteredStudents) { filteredStudents.count { it.gender == "L" } }
    val femaleCount = remember(filteredStudents) { filteredStudents.count { it.gender == "P" } }

    val templateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { out ->
                viewModel.writeStudentTemplateToStream(out) { ok ->
                    viewModel.emitMessage(
                        if (ok) "Template Excel siswa berhasil disimpan." else "Gagal menyimpan template Excel."
                    )
                }
            }
        }
    }

    val exportStudentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { out ->
                viewModel.writeStudentsExportToStream(filteredStudents, out) { ok ->
                    viewModel.emitMessage(
                        if (ok) "Data siswa berhasil diekspor ke Excel." else "Gagal mengekspor data siswa."
                    )
                }
            }
        }
    }

    val importStudentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else ""
                } ?: ""
            }.getOrDefault("")

            context.contentResolver.openInputStream(uri)?.let { input ->
                viewModel.importStudentsFromStream(input, fileName) { result ->
                    importResultDialog = result
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header
            Surface(
                color = NavyPrimary,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Data Siswa",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total ${students.size} siswa terdaftar (Kelas 1–6)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldAccent
                        ) {
                            Text(
                                text = "${filteredStudents.size} tampil",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Excel Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                importStudentsLauncher.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier.height(48.dp).testTag("import_excel_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Impor Excel")
                        }
                        OutlinedButton(
                            onClick = {
                                exportStudentsLauncher.launch("Data_Siswa_PJOK_${DateUtils.timestampFileSuffix()}.xlsx")
                            },
                            modifier = Modifier.height(48.dp).testTag("export_excel_students_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ekspor Excel")
                        }
                        OutlinedButton(
                            onClick = {
                                templateLauncher.launch("Template_Impor_Siswa_PJOK.xlsx")
                            },
                            modifier = Modifier.height(48.dp).testTag("template_excel_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Template Excel")
                        }
                    }
                }
            }

            // Search & Filter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SearchInputBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Cari nama siswa atau NIS...",
                    modifier = Modifier.testTag("student_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedClassFilter == null,
                        onClick = { selectedClassFilter = null },
                        label = { Text("Semua (${students.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_class_all")
                    )
                    (1..6).forEach { cls ->
                        val cnt = classCounts[cls] ?: 0
                        FilterChip(
                            selected = selectedClassFilter == cls,
                            onClick = { selectedClassFilter = cls },
                            label = { Text("Kelas $cls ($cnt)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_class_$cls")
                        )
                    }
                }

                if (filteredStudents.isNotEmpty()) {
                    Text(
                        text = "Menampilkan ${filteredStudents.size} siswa • Laki-laki: $maleCount • Perempuan: $femaleCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            // Student List
            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, OutlineSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = when {
                                    students.isEmpty() -> "Belum ada data siswa"
                                    selectedClassFilter != null && searchQuery.isBlank() -> "Belum ada siswa di Kelas $selectedClassFilter"
                                    else -> "Siswa tidak ditemukan"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = NavyPrimary
                            )
                            Text(
                                text = if (students.isEmpty() || (selectedClassFilter != null && searchQuery.isBlank())) {
                                    "Tambahkan data siswa secara manual atau impor langsung dari file Excel (.xlsx)."
                                } else {
                                    "Ubah kata kunci pencarian atau pilih filter kelas lain."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            if (searchQuery.isBlank()) {
                                Button(
                                    onClick = {
                                        editingStudent = null
                                        showStudentForm = true
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedClassFilter != null) "Tambah ke Kelas $selectedClassFilter" else "Tambah Siswa",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        StudentListCard(
                            student = student,
                            onClick = { detailStudent = student },
                            onEdit = {
                                editingStudent = student
                                showStudentForm = true
                            },
                            onDelete = { deletingStudent = student }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingStudent = null
                showStudentForm = true
            },
            containerColor = EmeraldAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("add_student_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Siswa")
        }
    }

    if (showStudentForm) {
        StudentFormDialog(
            initial = editingStudent,
            defaultClassId = selectedClassFilter ?: 1,
            onDismiss = { showStudentForm = false },
            onSubmit = { nis, name, gender, classId, onError ->
                val existing = editingStudent
                if (existing == null) {
                    viewModel.addStudent(nis, name, gender, classId, onSuccess = { showStudentForm = false }, onError = onError)
                } else {
                    viewModel.updateStudent(
                        existing.copy(nis = nis, name = name, gender = gender, classId = classId),
                        onSuccess = { showStudentForm = false },
                        onError = onError
                    )
                }
            }
        )
    }

    deletingStudent?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingStudent = null },
            title = { Text("Hapus Siswa?") },
            text = {
                Text("Data siswa ${target.name} (NIS ${target.nis}) beserta seluruh riwayat kehadirannya akan dihapus. Lanjutkan?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(target)
                        deletingStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAlpaFg)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingStudent = null }) {
                    Text("Batal")
                }
            }
        )
    }

    detailStudent?.let { st ->
        AlertDialog(
            onDismissRequest = { detailStudent = null },
            title = { Text(st.name, color = NavyPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NIS: ${st.nis}", style = MaterialTheme.typography.bodyMedium, color = NavyPrimary)
                    Text("Kelas: Kelas ${st.classId}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        "Jenis Kelamin: ${if (st.gender == "L") "Laki-laki (L)" else "Perempuan (P)"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        "Status Penyimpanan: Tersimpan permanen di SQLite perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnEmeraldContainer
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        detailStudent = null
                        editingStudent = st
                        showStudentForm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        detailStudent = null
                        deletingStudent = st
                    }
                ) {
                    Text("Hapus", color = StatusAlpaFg)
                }
            }
        )
    }

    importResultDialog?.let { res ->
        AlertDialog(
            onDismissRequest = { importResultDialog = null },
            title = { Text("Hasil Impor Excel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Berhasil diimpor: ${res.importedCount} siswa", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    if (res.duplicateNisList.isNotEmpty()) {
                        Text(
                            "Dilewati (NIS ganda): ${res.duplicateNisList.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (res.errorRows.isNotEmpty()) {
                        Text(
                            "Baris tidak valid:\n" + res.errorRows.take(5).joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusAlpaFg
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { importResultDialog = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun StudentListCard(
    student: StudentEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("student_card_${student.nis}"),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (student.gender == "L") NavyContainer else EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.trim().take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (student.gender == "L") NavyPrimary else OnEmeraldContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = NavyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NIS: ${student.nis}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NavyContainer
                    ) {
                        Text(
                            text = "Kelas ${student.classId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (student.gender == "L") Color(0xFFE0F2FE) else Color(0xFFFCE7F3)
                    ) {
                        Text(
                            text = if (student.gender == "L") "L" else "P",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (student.gender == "L") Color(0xFF0369A1) else Color(0xFFBE185D),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${student.name}", tint = NavyPrimary)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus ${student.name}", tint = StatusAlpaFg)
            }
        }
    }
}

@Composable
private fun StudentFormDialog(
    initial: StudentEntity?,
    defaultClassId: Int,
    onDismiss: () -> Unit,
    onSubmit: (nis: String, name: String, gender: String, classId: Int, onError: (String) -> Unit) -> Unit
) {
    var nis by remember { mutableStateOf(initial?.nis.orEmpty()) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var gender by remember { mutableStateOf(initial?.gender ?: "L") }
    var classId by remember { mutableIntStateOf(initial?.classId ?: defaultClassId) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = if (initial == null) "Tambah Siswa Baru" else "Edit Data Siswa",
                color = NavyPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nis,
                    onValueChange = {
                        nis = it
                        errorMessage = null
                    },
                    label = { Text("NIS (Wajib & Unik)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("student_nis_input")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nama Lengkap Siswa") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag("student_name_input")
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Jenis Kelamin", style = MaterialTheme.typography.labelMedium, color = NavyPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = gender == "L",
                            onClick = { gender = "L" },
                            label = { Text("Laki-laki (L)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = gender == "P",
                            onClick = { gender = "P" },
                            label = { Text("Perempuan (P)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Kelas (1 – 6)", style = MaterialTheme.typography.labelMedium, color = NavyPrimary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..6).forEach { cls ->
                            FilterChip(
                                selected = classId == cls,
                                onClick = { classId = cls },
                                label = { Text("Kelas $cls") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldAccent,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusAlpaFg,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    isSaving = true
                    onSubmit(nis, name, gender, classId) { err ->
                        isSaving = false
                        errorMessage = err
                    }
                },
                modifier = Modifier.testTag("save_student_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text(if (isSaving) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
