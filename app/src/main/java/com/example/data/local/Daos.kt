package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY id ASC")
    fun observeAllClasses(): Flow<List<SchoolClassEntity>>

    @Query("SELECT * FROM classes ORDER BY id ASC")
    suspend fun getAllClassesSync(): List<SchoolClassEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(classes: List<SchoolClassEntity>)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY class_id ASC, name ASC")
    fun observeAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE class_id = :classId ORDER BY name ASC")
    fun observeStudentsByClass(classId: Int): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE class_id = :classId ORDER BY name ASC")
    suspend fun getStudentsByClassSync(classId: Int): List<StudentEntity>

    @Query("SELECT * FROM students ORDER BY class_id ASC, name ASC")
    suspend fun getAllStudentsSync(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE nis = :nis LIMIT 1")
    suspend fun getStudentByNis(nis: String): StudentEntity?

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Long): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentsReplace(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: Long)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}

@Dao
interface MeetingDao {
    @Query("""
        SELECT m.*, c.class_name AS class_name,
            COALESCE(SUM(CASE WHEN a.attendance_status = 'H' THEN 1 ELSE 0 END), 0) AS hadir_count,
            COALESCE(SUM(CASE WHEN a.attendance_status = 'S' THEN 1 ELSE 0 END), 0) AS sakit_count,
            COALESCE(SUM(CASE WHEN a.attendance_status = 'I' THEN 1 ELSE 0 END), 0) AS izin_count,
            COALESCE(SUM(CASE WHEN a.attendance_status = 'A' THEN 1 ELSE 0 END), 0) AS alpa_count,
            COALESCE(SUM(CASE WHEN a.attendance_status = 'UNMARKED' THEN 1 ELSE 0 END), 0) AS unmarked_count
        FROM meetings m
        INNER JOIN classes c ON m.class_id = c.id
        LEFT JOIN attendance a ON a.meeting_id = m.id
        GROUP BY m.id
        ORDER BY m.date DESC, m.id DESC
    """)
    fun observeAllMeetingsWithCounts(): Flow<List<MeetingWithCounts>>

    @Query("SELECT * FROM meetings ORDER BY date DESC, meeting_number DESC")
    fun observeAllMeetings(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings ORDER BY id ASC")
    suspend fun getAllMeetingsSync(): List<MeetingEntity>

    @Query("""
        SELECT * FROM meetings
        WHERE class_id = :classId AND semester = :semester AND academic_year = :academicYear
        ORDER BY meeting_number DESC
    """)
    suspend fun getMeetingsForClassTerm(
        classId: Int,
        semester: String,
        academicYear: String
    ): List<MeetingEntity>

    @Query("""
        SELECT COALESCE(MAX(meeting_number), 0) FROM meetings
        WHERE class_id = :classId AND semester = :semester AND academic_year = :academicYear
    """)
    suspend fun getMaxMeetingNumber(
        classId: Int,
        semester: String,
        academicYear: String
    ): Int

    @Query("""
        SELECT * FROM meetings
        WHERE class_id = :classId AND meeting_number = :meetingNumber
          AND semester = :semester AND academic_year = :academicYear
        LIMIT 1
    """)
    suspend fun findDuplicateMeeting(
        classId: Int,
        meetingNumber: Int,
        semester: String,
        academicYear: String
    ): MeetingEntity?

    @Query("""
        SELECT * FROM meetings
        WHERE class_id = :classId AND date = :date
        ORDER BY meeting_number ASC
        LIMIT 1
    """)
    suspend fun findMeetingByClassAndDate(classId: Int, date: String): MeetingEntity?

    @Query("SELECT * FROM meetings WHERE id = :id LIMIT 1")
    suspend fun getMeetingById(id: Long): MeetingEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetingsReplace(meetings: List<MeetingEntity>)

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)

    @Query("DELETE FROM meetings")
    suspend fun deleteAllMeetings()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance")
    fun observeAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance ORDER BY id ASC")
    suspend fun getAllAttendanceSync(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE meeting_id = :meetingId")
    suspend fun getAttendanceByMeetingSync(meetingId: Long): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE meeting_id = :meetingId")
    suspend fun deleteByMeetingId(meetingId: Long)

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)
}
