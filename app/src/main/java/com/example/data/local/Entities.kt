package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus(val code: String, val label: String, val shortLabel: String) {
    UNMARKED("UNMARKED", "BELUM DIABSEN", "-"),
    H("H", "Hadir", "H"),
    S("S", "Sakit", "S"),
    I("I", "Izin", "I"),
    A("A", "Alpa", "A");

    companion object {
        fun fromCode(code: String?): AttendanceStatus =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: UNMARKED
    }
}

enum class MeetingStatus(val code: String, val label: String) {
    DRAFT("DRAFT", "Draft"),
    FINALIZED("FINALIZED", "Selesai / Final");

    companion object {
        fun fromCode(code: String?): MeetingStatus =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DRAFT
    }
}

enum class Gender(val code: String, val label: String) {
    L("L", "Laki-laki"),
    P("P", "Perempuan");

    companion object {
        fun fromInput(raw: String): Gender? {
            val v = raw.trim().lowercase()
            return when (v) {
                "l", "laki-laki", "laki", "lk", "male", "m" -> L
                "p", "perempuan", "pr", "wanita", "female", "f" -> P
                else -> null
            }
        }
    }
}

@Entity(tableName = "classes")
data class SchoolClassEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "class_name")
    val className: String
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["nis"], unique = true),
        Index(value = ["class_id"])
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "nis")
    val nis: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "gender")
    val gender: String, // "L" or "P"
    @ColumnInfo(name = "class_id")
    val classId: Int
)

@Entity(
    tableName = "meetings",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["class_id"]),
        Index(value = ["class_id", "meeting_number", "semester", "academic_year"], unique = true),
        Index(value = ["date"])
    ]
)
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "class_id")
    val classId: Int,
    @ColumnInfo(name = "meeting_number")
    val meetingNumber: Int,
    @ColumnInfo(name = "date")
    val date: String, // yyyy-MM-dd
    @ColumnInfo(name = "semester")
    val semester: String, // "Ganjil" / "Genap"
    @ColumnInfo(name = "academic_year")
    val academicYear: String, // e.g. "2026/2027"
    @ColumnInfo(name = "topic")
    val topic: String,
    @ColumnInfo(name = "notes")
    val notes: String,
    @ColumnInfo(name = "status")
    val status: String // "DRAFT" or "FINALIZED"
)

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meeting_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["meeting_id"]),
        Index(value = ["student_id"]),
        Index(value = ["meeting_id", "student_id"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "meeting_id")
    val meetingId: Long,
    @ColumnInfo(name = "student_id")
    val studentId: Long,
    @ColumnInfo(name = "attendance_status")
    val attendanceStatus: String, // "UNMARKED", "H", "S", "I", "A"
    @ColumnInfo(name = "note")
    val note: String = ""
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,
    @ColumnInfo(name = "teacher_name")
    val teacherName: String,
    @ColumnInfo(name = "school_name")
    val schoolName: String,
    @ColumnInfo(name = "academic_year")
    val academicYear: String,
    @ColumnInfo(name = "semester")
    val semester: String
)

data class AttendanceRecordWithStudent(
    @Embedded val attendance: AttendanceEntity,
    @ColumnInfo(name = "student_nis") val studentNis: String,
    @ColumnInfo(name = "student_name") val studentName: String,
    @ColumnInfo(name = "student_gender") val studentGender: String
)

data class MeetingWithCounts(
    @Embedded val meeting: MeetingEntity,
    @ColumnInfo(name = "class_name") val className: String,
    @ColumnInfo(name = "hadir_count") val hadirCount: Int,
    @ColumnInfo(name = "sakit_count") val sakitCount: Int,
    @ColumnInfo(name = "izin_count") val izinCount: Int,
    @ColumnInfo(name = "alpa_count") val alpaCount: Int,
    @ColumnInfo(name = "unmarked_count") val unmarkedCount: Int
)
