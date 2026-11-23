package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceEntity
import com.example.data.local.AttendanceStatus
import com.example.data.local.MeetingEntity
import com.example.data.local.MeetingStatus
import com.example.data.local.MeetingWithCounts
import com.example.data.local.SchoolClassEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.StudentEntity
import com.example.data.repository.AttendanceDraftItem
import com.example.data.repository.ImportResult
import com.example.data.repository.PjokRepository
import com.example.data.repository.RecapPeriod
import com.example.data.repository.RecapReportData
import com.example.data.repository.SaveResult
import com.example.util.DateUtils
import com.example.util.ParsedImportData
import com.example.util.PdfReportHelper
import com.example.util.XlsxHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class AttendanceEditorState(
    val selectedClassId: Int = 1,
    val selectedDate: String = DateUtils.todayIso(),
    val meetingNumber: Int = 1,
    val topic: String = "",
    val notes: String = "",
    val editingMeetingId: Long? = null,
    val isFinalized: Boolean = true,
    val searchQuery: String = "",
    val items: List<AttendanceDraftItem> = emptyList(),
    val classMeetingsHistory: List<MeetingEntity> = emptyList(),
    val isLoading: Boolean = false
) {
    val hadirCount: Int get() = items.count { it.status == AttendanceStatus.H }
    val sakitCount: Int get() = items.count { it.status == AttendanceStatus.S }
    val izinCount: Int get() = items.count { it.status == AttendanceStatus.I }
    val alpaCount: Int get() = items.count { it.status == AttendanceStatus.A }
    val unmarkedCount: Int get() = items.count { it.status == AttendanceStatus.UNMARKED }

    val filteredItems: List<AttendanceDraftItem>
        get() = if (searchQuery.isBlank()) items else {
            val q = searchQuery.trim().lowercase()
            items.filter {
                it.student.name.lowercase().contains(q) || it.student.nis.lowercase().contains(q)
            }
        }
}

data class RecapFilterState(
    val period: RecapPeriod = RecapPeriod.SEMESTER,
    val referenceDateIso: String = DateUtils.todayIso(),
    val selectedYearMonth: String = DateUtils.currentYearMonth(),
    val selectedClassId: Int? = null, // null = Semua Kelas
    val selectedSemester: String? = null, // null = follow active settings
    val selectedAcademicYear: String? = null // null = follow active settings
)

data class DashboardSummary(
    val totalStudents: Int = 0,
    val totalHadir: Int = 0,
    val totalSakit: Int = 0,
    val totalIzin: Int = 0,
    val totalAlpa: Int = 0,
    val studentCountByClass: Map<Int, Int> = emptyMap(),
    val recentMeetings: List<MeetingWithCounts> = emptyList()
)

class PjokViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repository = PjokRepository(db)

    val classes: StateFlow<List<SchoolClassEntity>> = repository.classesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<StudentEntity>> = repository.studentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meetingsWithCounts: StateFlow<List<MeetingWithCounts>> = repository.meetingsWithCountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meetings: StateFlow<List<MeetingEntity>> = repository.meetingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceEntity>> = repository.attendanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity> = repository.settingsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsEntity(
                id = 1,
                teacherName = "Andi Mozes Leavingstone Bire, S.Pd.",
                schoolName = "UPTD SDN LOKOJULI",
                academicYear = DateUtils.defaultAcademicYear(),
                semester = DateUtils.defaultSemester()
            )
        )

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        students,
        meetingsWithCounts
    ) { studentList, meetingList ->
        val finalizedMeetings = meetingList.filter { it.meeting.status == MeetingStatus.FINALIZED.code }
        DashboardSummary(
            totalStudents = studentList.size,
            totalHadir = finalizedMeetings.sumOf { it.hadirCount },
            totalSakit = finalizedMeetings.sumOf { it.sakitCount },
            totalIzin = finalizedMeetings.sumOf { it.izinCount },
            totalAlpa = finalizedMeetings.sumOf { it.alpaCount },
            studentCountByClass = studentList.groupingBy { it.classId }.eachCount(),
            recentMeetings = meetingList.take(8)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    private val _attendanceState = MutableStateFlow(AttendanceEditorState())
    val attendanceState: StateFlow<AttendanceEditorState> = _attendanceState.asStateFlow()

    private val _recapFilter = MutableStateFlow(RecapFilterState())
    val recapFilter: StateFlow<RecapFilterState> = _recapFilter.asStateFlow()

    val recapReport: StateFlow<RecapReportData> = combine(
        classes,
        students,
        meetings,
        attendanceRecords,
        combine(_recapFilter, settings) { f, s -> f to s }
    ) { classList, studentList, meetingList, attList, (filter, activeSettings) ->
        repository.computeRecap(
            classes = classList,
            students = studentList,
            meetings = meetingList,
            attendanceRecords = attList,
            period = filter.period,
            referenceDateIso = filter.referenceDateIso,
            selectedYearMonth = filter.selectedYearMonth,
            selectedSemester = filter.selectedSemester ?: activeSettings.semester,
            selectedAcademicYear = filter.selectedAcademicYear ?: activeSettings.academicYear,
            selectedClassId = filter.selectedClassId
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RecapReportData(emptyList(), emptyList(), 0, 0, 0, 0, 0, 0.0)
    )

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            loadAttendanceForClass(1, meetingId = null)
        }
        // Keep attendance draft student list synced if students are added/edited while no meeting is edited
        viewModelScope.launch {
            students.collect {
                val cur = _attendanceState.value
                if (cur.editingMeetingId == null) {
                    refreshStudentsInCurrentDraft(cur.selectedClassId)
                }
            }
        }
    }

    private suspend fun refreshStudentsInCurrentDraft(classId: Int) {
        val currentMap = _attendanceState.value.items.associateBy { it.student.id }
        val (_, fresh) = repository.loadAttendanceDraftForMeeting(null, classId)
        val merged = fresh.map { item ->
            val prev = currentMap[item.student.id]
            if (prev != null) item.copy(status = prev.status, note = prev.note) else item
        }
        _attendanceState.update { it.copy(items = merged) }
    }

    fun emitMessage(msg: String) {
        _snackbarMessage.tryEmit(msg)
    }

    // --- Student CRUD & Excel ---
    fun addStudent(nis: String, name: String, gender: String, classId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.addStudent(nis, name, gender, classId)) {
                is SaveResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is SaveResult.Error -> onError(res.message)
            }
        }
    }

    fun updateStudent(student: StudentEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.updateStudent(student)) {
                is SaveResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is SaveResult.Error -> onError(res.message)
            }
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student.id)
            emitMessage("Siswa '${student.name}' dihapus.")
        }
    }

    fun importStudentsFromStream(inputStream: InputStream, fileNameHint: String, onDone: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val parsed: ParsedImportData = withContext(Dispatchers.IO) {
                inputStream.use { XlsxHelper.readStudentsFromStream(it, fileNameHint) }
            }
            val result = repository.importStudents(parsed.rows, parsed.parseErrors)
            onDone(result)
        }
    }

    fun writeStudentTemplateToStream(out: OutputStream, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { out.use { XlsxHelper.writeStudentTemplate(it) } }.isSuccess
            }
            onDone(ok)
        }
    }

    fun writeStudentsExportToStream(filteredStudents: List<StudentEntity>, out: OutputStream, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { out.use { XlsxHelper.writeStudentsExport(filteredStudents, it) } }.isSuccess
            }
            onDone(ok)
        }
    }

    // --- Attendance & Meetings ---
    fun loadAttendanceForClass(classId: Int, meetingId: Long? = null) {
        viewModelScope.launch {
            _attendanceState.update { it.copy(isLoading = true, selectedClassId = classId, searchQuery = "") }
            val activeSettings = settings.first()
            val history = repository.getMeetingsForClassTerm(
                classId = classId,
                semester = activeSettings.semester,
                academicYear = activeSettings.academicYear
            )
            val (meeting, items) = repository.loadAttendanceDraftForMeeting(meetingId, classId)
            val nextNum = repository.getNextMeetingNumber(
                classId = classId,
                semester = activeSettings.semester,
                academicYear = activeSettings.academicYear
            )

            _attendanceState.update {
                if (meeting != null) {
                    it.copy(
                        selectedClassId = meeting.classId,
                        selectedDate = meeting.date,
                        meetingNumber = meeting.meetingNumber,
                        topic = meeting.topic,
                        notes = meeting.notes,
                        editingMeetingId = meeting.id,
                        isFinalized = meeting.status == MeetingStatus.FINALIZED.code,
                        items = items,
                        classMeetingsHistory = history,
                        isLoading = false
                    )
                } else {
                    it.copy(
                        selectedClassId = classId,
                        selectedDate = DateUtils.todayIso(),
                        meetingNumber = nextNum,
                        topic = "",
                        notes = "",
                        editingMeetingId = null,
                        isFinalized = true,
                        items = items,
                        classMeetingsHistory = history,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setAttendanceDate(isoDate: String) {
        _attendanceState.update { it.copy(selectedDate = isoDate) }
    }

    fun setMeetingNumber(num: Int) {
        _attendanceState.update { it.copy(meetingNumber = num.coerceAtLeast(1)) }
    }

    fun setMeetingTopic(topic: String) {
        _attendanceState.update { it.copy(topic = topic) }
    }

    fun setMeetingNotes(notes: String) {
        _attendanceState.update { it.copy(notes = notes) }
    }

    fun setAttendanceSearch(query: String) {
        _attendanceState.update { it.copy(searchQuery = query) }
    }

    fun setStudentStatus(studentId: Long, status: AttendanceStatus) {
        _attendanceState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.student.id == studentId) item.copy(status = status) else item
                }
            )
        }
    }

    fun setStudentNote(studentId: Long, note: String) {
        _attendanceState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.student.id == studentId) item.copy(note = note) else item
                }
            )
        }
    }

    fun markAllHadir() {
        _attendanceState.update { state ->
            state.copy(items = state.items.map { it.copy(status = AttendanceStatus.H) })
        }
    }

    fun resetAllUnmarked() {
        _attendanceState.update { state ->
            state.copy(items = state.items.map { it.copy(status = AttendanceStatus.UNMARKED, note = "") })
        }
    }

    fun saveAttendance(finalize: Boolean, onResult: (SaveResult) -> Unit) {
        viewModelScope.launch {
            val st = _attendanceState.value
            val activeSettings = settings.value
            val result = repository.saveAttendanceSession(
                editingMeetingId = st.editingMeetingId,
                classId = st.selectedClassId,
                meetingNumber = st.meetingNumber,
                date = st.selectedDate,
                semester = activeSettings.semester,
                academicYear = activeSettings.academicYear,
                topic = st.topic,
                notes = st.notes,
                finalize = finalize,
                items = st.items
            )
            if (result is SaveResult.Success) {
                emitMessage(result.message)
                loadAttendanceForClass(st.selectedClassId, meetingId = result.meetingId)
            }
            onResult(result)
        }
    }

    fun deleteMeeting(meetingId: Long) {
        viewModelScope.launch {
            val currentClass = _attendanceState.value.selectedClassId
            repository.deleteMeeting(meetingId)
            emitMessage("Pertemuan berhasil dihapus.")
            loadAttendanceForClass(currentClass, meetingId = null)
        }
    }

    // --- Recap & Exports ---
    fun updateRecapPeriod(period: RecapPeriod) {
        _recapFilter.update { it.copy(period = period) }
    }

    fun updateRecapClassFilter(classId: Int?) {
        _recapFilter.update { it.copy(selectedClassId = classId) }
    }

    fun updateRecapDate(iso: String) {
        _recapFilter.update { it.copy(referenceDateIso = iso) }
    }

    fun updateRecapMonth(yearMonth: String) {
        _recapFilter.update { it.copy(selectedYearMonth = yearMonth) }
    }

    fun updateRecapSemester(semester: String?) {
        _recapFilter.update { it.copy(selectedSemester = semester) }
    }

    fun updateRecapAcademicYear(year: String?) {
        _recapFilter.update { it.copy(selectedAcademicYear = year) }
    }

    fun currentRecapLabels(): Pair<String, String> {
        val f = _recapFilter.value
        val s = settings.value
        val classLabel = f.selectedClassId?.let { "Kelas $it" } ?: "Semua Kelas (1 - 6)"
        val sem = f.selectedSemester ?: s.semester
        val ay = f.selectedAcademicYear ?: s.academicYear
        val periodLabel = when (f.period) {
            RecapPeriod.HARIAN -> "Harian (${DateUtils.formatIndonesianShort(f.referenceDateIso)})"
            RecapPeriod.MINGGUAN -> {
                val (start, end) = DateUtils.weekRangeFor(f.referenceDateIso)
                "Mingguan (${DateUtils.formatIndonesianShort(start)} - ${DateUtils.formatIndonesianShort(end)})"
            }
            RecapPeriod.BULANAN -> "Bulanan (${DateUtils.formatMonthYear(f.selectedYearMonth)})"
            RecapPeriod.SEMESTER -> "Semester $sem $ay"
        }
        return classLabel to periodLabel
    }

    fun exportRecapExcelToStream(out: OutputStream, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val (classLabel, periodLabel) = currentRecapLabels()
            val curSettings = settings.value
            val report = recapReport.value
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    out.use { XlsxHelper.writeRecapReport(curSettings, classLabel, periodLabel, report, it) }
                }.isSuccess
            }
            onDone(ok)
        }
    }

    fun exportRecapPdfToStream(out: OutputStream, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val (classLabel, periodLabel) = currentRecapLabels()
            val curSettings = settings.value
            val report = recapReport.value
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    out.use { PdfReportHelper.generateA4Pdf(curSettings, classLabel, periodLabel, report, it) }
                }.isSuccess
            }
            onDone(ok)
        }
    }

    // --- Settings & Backup / Restore ---
    fun saveSettings(teacherName: String, schoolName: String, academicYear: String, semester: String) {
        viewModelScope.launch {
            repository.updateSettings(
                SettingsEntity(
                    id = 1,
                    teacherName = teacherName.trim().ifEmpty { "Andi Mozes Leavingstone Bire, S.Pd." },
                    schoolName = schoolName.trim().ifEmpty { "UPTD SDN LOKOJULI" },
                    academicYear = academicYear.trim().ifEmpty { DateUtils.defaultAcademicYear() },
                    semester = semester
                )
            )
            emitMessage("Pengaturan berhasil disimpan.")
        }
    }

    fun exportBackupToStream(out: OutputStream, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = repository.exportBackupJson()
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            onDone(ok)
        }
    }

    suspend fun generateBackupJsonString(): String = withContext(Dispatchers.IO) {
        repository.exportBackupJson()
    }

    fun restoreBackupFromStream(input: InputStream, onResult: (SaveResult) -> Unit) {
        viewModelScope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching { input.use { it.readBytes().decodeToString() } }.getOrNull()
            }
            if (raw == null) {
                onResult(SaveResult.Error("Gagal membaca file backup."))
                return@launch
            }
            val result = repository.restoreFromBackupJson(raw)
            if (result is SaveResult.Success) {
                loadAttendanceForClass(1, meetingId = null)
            }
            onResult(result)
        }
    }
}
