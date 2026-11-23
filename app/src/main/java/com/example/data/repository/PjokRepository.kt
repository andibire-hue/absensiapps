package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceEntity
import com.example.data.local.AttendanceStatus
import com.example.data.local.Gender
import com.example.data.local.MeetingEntity
import com.example.data.local.MeetingStatus
import com.example.data.local.MeetingWithCounts
import com.example.data.local.SchoolClassEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.StudentEntity
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

enum class RecapPeriod(val label: String) {
    HARIAN("Harian"),
    MINGGUAN("Mingguan"),
    BULANAN("Bulanan"),
    SEMESTER("Semester")
}

data class StudentRecapRow(
    val number: Int,
    val student: StudentEntity,
    val className: String,
    val hadir: Int,
    val sakit: Int,
    val izin: Int,
    val alpa: Int,
    val totalMeetings: Int,
    val percentage: Double
)

data class ClassRecapSummary(
    val schoolClass: SchoolClassEntity,
    val studentCount: Int,
    val finalizedMeetingsCount: Int,
    val totalHadir: Int,
    val totalSakit: Int,
    val totalIzin: Int,
    val totalAlpa: Int,
    val percentage: Double
)

data class RecapReportData(
    val studentRows: List<StudentRecapRow>,
    val classSummaries: List<ClassRecapSummary>,
    val totalFinalizedMeetings: Int,
    val totalHadir: Int,
    val totalSakit: Int,
    val totalIzin: Int,
    val totalAlpa: Int,
    val overallPercentage: Double
)

data class ImportStudentInput(
    val nis: String,
    val name: String,
    val gender: String,
    val classId: Int
)

data class ImportResult(
    val importedCount: Int,
    val duplicateNisList: List<String>,
    val errorRows: List<String>
)

data class AttendanceDraftItem(
    val student: StudentEntity,
    val status: AttendanceStatus,
    val note: String
)

sealed class SaveResult {
    data class Success(val message: String, val meetingId: Long = 0) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class PjokRepository(private val db: AppDatabase) {
    private val classDao = db.classDao()
    private val studentDao = db.studentDao()
    private val meetingDao = db.meetingDao()
    private val attendanceDao = db.attendanceDao()
    private val settingsDao = db.settingsDao()

    val classesFlow: Flow<List<SchoolClassEntity>> = classDao.observeAllClasses()
    val studentsFlow: Flow<List<StudentEntity>> = studentDao.observeAllStudents()
    val meetingsWithCountsFlow: Flow<List<MeetingWithCounts>> = meetingDao.observeAllMeetingsWithCounts()
    val meetingsFlow: Flow<List<MeetingEntity>> = meetingDao.observeAllMeetings()
    val attendanceFlow: Flow<List<AttendanceEntity>> = attendanceDao.observeAllAttendance()

    val settingsFlow: Flow<SettingsEntity> = settingsDao.observeSettings().map {
        it ?: defaultSettings()
    }

    suspend fun ensureSeedData() {
        val existingClasses = classDao.getAllClassesSync()
        if (existingClasses.isEmpty()) {
            val defaultClasses = (1..6).map { SchoolClassEntity(id = it, className = "Kelas $it") }
            classDao.insertAll(defaultClasses)
        }
        if (settingsDao.getSettingsSync() == null) {
            settingsDao.upsertSettings(defaultSettings())
        }
    }

    private fun defaultSettings() = SettingsEntity(
        id = 1,
        teacherName = "Andi Mozes Leavingstone Bire, S.Pd.",
        schoolName = "UPTD SDN LOKOJULI",
        academicYear = DateUtils.defaultAcademicYear(),
        semester = DateUtils.defaultSemester()
    )

    suspend fun updateSettings(settings: SettingsEntity) {
        settingsDao.upsertSettings(settings.copy(id = 1))
    }

    suspend fun addStudent(nis: String, name: String, gender: String, classId: Int): SaveResult {
        val cleanNis = nis.trim()
        val cleanName = name.trim()
        if (cleanNis.isEmpty()) return SaveResult.Error("NIS wajib diisi.")
        if (cleanName.isEmpty()) return SaveResult.Error("Nama lengkap siswa wajib diisi.")
        if (classId !in 1..6) return SaveResult.Error("Kelas harus antara Kelas 1 sampai Kelas 6.")
        val parsedGender = Gender.fromInput(gender)
            ?: return SaveResult.Error("Jenis kelamin harus L (Laki-laki) atau P (Perempuan).")

        return try {
            ensureSeedData()
            if (studentDao.getStudentByNis(cleanNis) != null) {
                return SaveResult.Error("NIS '$cleanNis' sudah terdaftar! NIS tidak boleh ganda.")
            }

            val id = studentDao.insertStudent(
                StudentEntity(
                    nis = cleanNis,
                    name = cleanName,
                    gender = parsedGender.code,
                    classId = classId
                )
            )
            SaveResult.Success("Siswa '$cleanName' berhasil disimpan ke database.", id)
        } catch (e: Exception) {
            SaveResult.Error("Gagal menyimpan siswa: ${e.message ?: "kesalahan database"}")
        }
    }

    suspend fun updateStudent(student: StudentEntity): SaveResult {
        val cleanNis = student.nis.trim()
        val cleanName = student.name.trim()
        if (cleanNis.isEmpty()) return SaveResult.Error("NIS wajib diisi.")
        if (cleanName.isEmpty()) return SaveResult.Error("Nama lengkap siswa wajib diisi.")
        if (student.classId !in 1..6) return SaveResult.Error("Kelas harus antara 1 sampai 6.")
        val parsedGender = Gender.fromInput(student.gender)
            ?: return SaveResult.Error("Jenis kelamin harus L atau P.")

        return try {
            ensureSeedData()
            val existingWithNis = studentDao.getStudentByNis(cleanNis)
            if (existingWithNis != null && existingWithNis.id != student.id) {
                return SaveResult.Error("NIS '$cleanNis' sudah digunakan siswa lain (${existingWithNis.name}).")
            }

            studentDao.updateStudent(
                student.copy(
                    nis = cleanNis,
                    name = cleanName,
                    gender = parsedGender.code
                )
            )
            SaveResult.Success("Data siswa '$cleanName' berhasil diperbarui.")
        } catch (e: Exception) {
            SaveResult.Error("Gagal memperbarui siswa: ${e.message ?: "kesalahan database"}")
        }
    }

    suspend fun deleteStudent(studentId: Long) {
        studentDao.deleteStudentById(studentId)
    }

    suspend fun importStudents(rows: List<ImportStudentInput>, preErrors: List<String> = emptyList()): ImportResult {
        ensureSeedData()
        var imported = 0
        val duplicates = mutableListOf<String>()
        val errors = preErrors.toMutableList()
        val seenNisInBatch = mutableSetOf<String>()

        db.withTransaction {
            for (row in rows) {
                val cleanNis = row.nis.trim()
                val cleanName = row.name.trim()
                if (cleanNis.isEmpty() || cleanName.isEmpty()) {
                    errors.add("Baris tanpa NIS atau Nama dilewati.")
                    continue
                }
                if (row.classId !in 1..6) {
                    errors.add("NIS $cleanNis: kelas tidak valid (${row.classId}).")
                    continue
                }
                val parsedGender = Gender.fromInput(row.gender)
                if (parsedGender == null) {
                    errors.add("NIS $cleanNis: jenis kelamin '${row.gender}' tidak valid.")
                    continue
                }
                if (!seenNisInBatch.add(cleanNis) || studentDao.getStudentByNis(cleanNis) != null) {
                    duplicates.add(cleanNis)
                    continue
                }
                studentDao.insertStudent(
                    StudentEntity(
                        nis = cleanNis,
                        name = cleanName,
                        gender = parsedGender.code,
                        classId = row.classId
                    )
                )
                imported++
            }
        }
        return ImportResult(imported, duplicates, errors)
    }

    suspend fun getNextMeetingNumber(classId: Int, semester: String, academicYear: String): Int {
        return meetingDao.getMaxMeetingNumber(classId, semester, academicYear) + 1
    }

    suspend fun getMeetingsForClassTerm(
        classId: Int,
        semester: String,
        academicYear: String
    ): List<MeetingEntity> {
        return meetingDao.getMeetingsForClassTerm(classId, semester, academicYear)
    }

    suspend fun loadAttendanceDraftForMeeting(
        meetingId: Long?,
        classId: Int
    ): Pair<MeetingEntity?, List<AttendanceDraftItem>> {
        val students = studentDao.getStudentsByClassSync(classId)
        val meeting = meetingId?.let { meetingDao.getMeetingById(it) }
        val existingRecords = if (meeting != null) {
            attendanceDao.getAttendanceByMeetingSync(meeting.id).associateBy { it.studentId }
        } else {
            emptyMap()
        }

        val items = students.map { student ->
            val rec = existingRecords[student.id]
            AttendanceDraftItem(
                student = student,
                status = AttendanceStatus.fromCode(rec?.attendanceStatus), // defaults to UNMARKED ("BELUM DIABSEN")
                note = rec?.note.orEmpty()
            )
        }
        return meeting to items
    }

    suspend fun saveAttendanceSession(
        editingMeetingId: Long?,
        classId: Int,
        meetingNumber: Int,
        date: String,
        semester: String,
        academicYear: String,
        topic: String,
        notes: String,
        finalize: Boolean,
        items: List<AttendanceDraftItem>
    ): SaveResult {
        if (classId !in 1..6) return SaveResult.Error("Pilih kelas terlebih dahulu.")
        if (meetingNumber <= 0) return SaveResult.Error("Nomor pertemuan harus minimal 1.")
        if (date.isBlank()) return SaveResult.Error("Tanggal pertemuan wajib diisi.")
        if (items.isEmpty()) return SaveResult.Error("Tidak ada siswa di kelas ini untuk diabsen.")

        // Check duplicate meeting (same class, meetingNumber, semester, academicYear)
        val dup = meetingDao.findDuplicateMeeting(classId, meetingNumber, semester, academicYear)
        if (dup != null && dup.id != editingMeetingId) {
            return SaveResult.Error(
                "Pertemuan ke-$meetingNumber untuk Kelas $classId ($semester $academicYear) sudah ada. Silakan pilih edit atau gunakan nomor pertemuan lain."
            )
        }

        val statusCode = if (finalize) MeetingStatus.FINALIZED.code else MeetingStatus.DRAFT.code
        val cleanTopic = topic.trim().ifEmpty { "Pembelajaran PJOK" }

        return try {
            var savedId = 0L
            db.withTransaction {
                if (editingMeetingId != null) {
                    val updated = MeetingEntity(
                        id = editingMeetingId,
                        classId = classId,
                        meetingNumber = meetingNumber,
                        date = date,
                        semester = semester,
                        academicYear = academicYear,
                        topic = cleanTopic,
                        notes = notes.trim(),
                        status = statusCode
                    )
                    meetingDao.updateMeeting(updated)
                    savedId = editingMeetingId
                    attendanceDao.deleteByMeetingId(savedId)
                } else {
                    val inserted = MeetingEntity(
                        classId = classId,
                        meetingNumber = meetingNumber,
                        date = date,
                        semester = semester,
                        academicYear = academicYear,
                        topic = cleanTopic,
                        notes = notes.trim(),
                        status = statusCode
                    )
                    savedId = meetingDao.insertMeeting(inserted)
                }

                val attendanceRecords = items.map { item ->
                    AttendanceEntity(
                        meetingId = savedId,
                        studentId = item.student.id,
                        attendanceStatus = item.status.code,
                        note = item.note.trim()
                    )
                }
                attendanceDao.upsertAll(attendanceRecords)
            }
            val msg = if (finalize) {
                "Absensi Pertemuan ke-$meetingNumber berhasil disimpan & difinalisasi."
            } else {
                "Draft absensi Pertemuan ke-$meetingNumber berhasil disimpan."
            }
            SaveResult.Success(msg, savedId)
        } catch (e: Exception) {
            SaveResult.Error("Gagal menyimpan absensi: ${e.message ?: "kesalahan database"}")
        }
    }

    suspend fun deleteMeeting(meetingId: Long) {
        meetingDao.deleteMeetingById(meetingId)
    }

    fun computeRecap(
        classes: List<SchoolClassEntity>,
        students: List<StudentEntity>,
        meetings: List<MeetingEntity>,
        attendanceRecords: List<AttendanceEntity>,
        period: RecapPeriod,
        referenceDateIso: String,
        selectedYearMonth: String,
        selectedSemester: String,
        selectedAcademicYear: String,
        selectedClassId: Int? // null = all classes
    ): RecapReportData {
        val weekRange = DateUtils.weekRangeFor(referenceDateIso)
        val filteredMeetings = meetings.filter { m ->
            m.status == MeetingStatus.FINALIZED.code &&
                (selectedClassId == null || m.classId == selectedClassId) &&
                m.academicYear == selectedAcademicYear &&
                when (period) {
                    RecapPeriod.HARIAN -> m.date == referenceDateIso
                    RecapPeriod.MINGGUAN -> m.date >= weekRange.first && m.date <= weekRange.second
                    RecapPeriod.BULANAN -> DateUtils.isIsoInMonth(m.date, selectedYearMonth)
                    RecapPeriod.SEMESTER -> m.semester == selectedSemester
                }
        }

        val meetingsByClass = filteredMeetings.groupBy { it.classId }
        val attendanceByMeeting = attendanceRecords.groupBy { it.meetingId }
        val classMap = classes.associateBy { it.id }

        val targetStudents = students.filter { selectedClassId == null || it.classId == selectedClassId }
        val studentRows = targetStudents.mapIndexed { idx, student ->
            val classMeetings = meetingsByClass[student.classId].orEmpty()
            val finalizedCount = classMeetings.size
            var h = 0
            var s = 0
            var i = 0
            var a = 0
            for (meeting in classMeetings) {
                val rec = attendanceByMeeting[meeting.id]?.firstOrNull { it.studentId == student.id }
                when (AttendanceStatus.fromCode(rec?.attendanceStatus)) {
                    AttendanceStatus.H -> h++
                    AttendanceStatus.S -> s++
                    AttendanceStatus.I -> i++
                    AttendanceStatus.A -> a++
                    AttendanceStatus.UNMARKED -> Unit
                }
            }
            val pct = if (finalizedCount > 0) (h * 100.0) / finalizedCount else 0.0
            StudentRecapRow(
                number = idx + 1,
                student = student,
                className = classMap[student.classId]?.className ?: "Kelas ${student.classId}",
                hadir = h,
                sakit = s,
                izin = i,
                alpa = a,
                totalMeetings = finalizedCount,
                percentage = pct
            )
        }

        val targetClasses = classes.filter { selectedClassId == null || it.id == selectedClassId }
        val classSummaries = targetClasses.map { sc ->
            val classStudents = students.filter { it.classId == sc.id }
            val classMeetings = meetingsByClass[sc.id].orEmpty()
            val classRows = studentRows.filter { it.student.classId == sc.id }
            val sumH = classRows.sumOf { it.hadir }
            val sumS = classRows.sumOf { it.sakit }
            val sumI = classRows.sumOf { it.izin }
            val sumA = classRows.sumOf { it.alpa }
            val denom = classStudents.size * classMeetings.size
            val pct = if (denom > 0) (sumH * 100.0) / denom else 0.0
            ClassRecapSummary(
                schoolClass = sc,
                studentCount = classStudents.size,
                finalizedMeetingsCount = classMeetings.size,
                totalHadir = sumH,
                totalSakit = sumS,
                totalIzin = sumI,
                totalAlpa = sumA,
                percentage = pct
            )
        }

        val totalH = studentRows.sumOf { it.hadir }
        val totalS = studentRows.sumOf { it.sakit }
        val totalI = studentRows.sumOf { it.izin }
        val totalA = studentRows.sumOf { it.alpa }
        val totalPossible = studentRows.sumOf { it.totalMeetings }
        val overallPct = if (totalPossible > 0) (totalH * 100.0) / totalPossible else 0.0

        return RecapReportData(
            studentRows = studentRows,
            classSummaries = classSummaries,
            totalFinalizedMeetings = filteredMeetings.size,
            totalHadir = totalH,
            totalSakit = totalS,
            totalIzin = totalI,
            totalAlpa = totalA,
            overallPercentage = overallPct
        )
    }

    suspend fun exportBackupJson(): String {
        ensureSeedData()
        val settings = settingsDao.getSettingsSync() ?: defaultSettings()
        val classes = classDao.getAllClassesSync()
        val students = studentDao.getAllStudentsSync()
        val meetings = meetingDao.getAllMeetingsSync()
        val attendances = attendanceDao.getAllAttendanceSync()

        val root = JSONObject().apply {
            put("app", "ABSENSI PJOK - UPTD SDN LOKOJULI")
            put("schema_version", 1)
            put("exported_at", DateUtils.todayIso())
            put(
                "settings",
                JSONObject().apply {
                    put("id", settings.id)
                    put("teacher_name", settings.teacherName)
                    put("school_name", settings.schoolName)
                    put("academic_year", settings.academicYear)
                    put("semester", settings.semester)
                }
            )
            put(
                "classes",
                JSONArray().apply {
                    classes.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("class_name", c.className)
                        })
                    }
                }
            )
            put(
                "students",
                JSONArray().apply {
                    students.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("nis", s.nis)
                            put("name", s.name)
                            put("gender", s.gender)
                            put("class_id", s.classId)
                        })
                    }
                }
            )
            put(
                "meetings",
                JSONArray().apply {
                    meetings.forEach { m ->
                        put(JSONObject().apply {
                            put("id", m.id)
                            put("class_id", m.classId)
                            put("meeting_number", m.meetingNumber)
                            put("date", m.date)
                            put("semester", m.semester)
                            put("academic_year", m.academicYear)
                            put("topic", m.topic)
                            put("notes", m.notes)
                            put("status", m.status)
                        })
                    }
                }
            )
            put(
                "attendance",
                JSONArray().apply {
                    attendances.forEach { a ->
                        put(JSONObject().apply {
                            put("id", a.id)
                            put("meeting_id", a.meetingId)
                            put("student_id", a.studentId)
                            put("attendance_status", a.attendanceStatus)
                            put("note", a.note)
                        })
                    }
                }
            )
        }
        return root.toString(2)
    }

    suspend fun restoreFromBackupJson(rawJson: String): SaveResult {
        return try {
            val root = JSONObject(rawJson)
            if (!root.has("students") || !root.has("meetings") || !root.has("attendance")) {
                return SaveResult.Error("Format file backup JSON tidak valid.")
            }

            val settingsObj = root.optJSONObject("settings")
            val restoredSettings = if (settingsObj != null) {
                SettingsEntity(
                    id = 1,
                    teacherName = settingsObj.optString("teacher_name", "Andi Mozes Leavingstone Bire, S.Pd."),
                    schoolName = settingsObj.optString("school_name", "UPTD SDN LOKOJULI"),
                    academicYear = settingsObj.optString("academic_year", DateUtils.defaultAcademicYear()),
                    semester = settingsObj.optString("semester", DateUtils.defaultSemester())
                )
            } else {
                defaultSettings()
            }

            val classesArr = root.optJSONArray("classes")
            val restoredClasses = if (classesArr != null && classesArr.length() > 0) {
                (0 until classesArr.length()).map { idx ->
                    val o = classesArr.getJSONObject(idx)
                    SchoolClassEntity(o.getInt("id"), o.getString("class_name"))
                }
            } else {
                (1..6).map { SchoolClassEntity(it, "Kelas $it") }
            }

            val studentsArr = root.getJSONArray("students")
            val restoredStudents = (0 until studentsArr.length()).map { idx ->
                val o = studentsArr.getJSONObject(idx)
                StudentEntity(
                    id = o.getLong("id"),
                    nis = o.getString("nis"),
                    name = o.getString("name"),
                    gender = o.getString("gender"),
                    classId = o.getInt("class_id")
                )
            }

            val meetingsArr = root.getJSONArray("meetings")
            val restoredMeetings = (0 until meetingsArr.length()).map { idx ->
                val o = meetingsArr.getJSONObject(idx)
                MeetingEntity(
                    id = o.getLong("id"),
                    classId = o.getInt("class_id"),
                    meetingNumber = o.getInt("meeting_number"),
                    date = o.getString("date"),
                    semester = o.getString("semester"),
                    academicYear = o.getString("academic_year"),
                    topic = o.optString("topic", ""),
                    notes = o.optString("notes", ""),
                    status = o.optString("status", MeetingStatus.FINALIZED.code)
                )
            }

            val attArr = root.getJSONArray("attendance")
            val restoredAttendances = (0 until attArr.length()).map { idx ->
                val o = attArr.getJSONObject(idx)
                AttendanceEntity(
                    id = o.getLong("id"),
                    meetingId = o.getLong("meeting_id"),
                    studentId = o.getLong("student_id"),
                    attendanceStatus = o.getString("attendance_status"),
                    note = o.optString("note", "")
                )
            }

            db.withTransaction {
                attendanceDao.deleteAllAttendance()
                meetingDao.deleteAllMeetings()
                studentDao.deleteAllStudents()
                classDao.insertAll(restoredClasses)
                settingsDao.upsertSettings(restoredSettings)
                if (restoredStudents.isNotEmpty()) studentDao.insertStudentsReplace(restoredStudents)
                if (restoredMeetings.isNotEmpty()) meetingDao.insertMeetingsReplace(restoredMeetings)
                if (restoredAttendances.isNotEmpty()) attendanceDao.upsertAll(restoredAttendances)
            }

            SaveResult.Success(
                "Restore berhasil: ${restoredStudents.size} siswa, ${restoredMeetings.size} pertemuan, dan ${restoredAttendances.size} catatan kehadiran dipulihkan."
            )
        } catch (e: Exception) {
            SaveResult.Error("Gagal membaca file backup JSON: ${e.message ?: "format rusak"}")
        }
    }
}
