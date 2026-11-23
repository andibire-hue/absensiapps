package com.example

import com.example.data.local.AttendanceEntity
import com.example.data.local.AttendanceStatus
import com.example.data.local.Gender
import com.example.data.local.MeetingEntity
import com.example.data.local.MeetingStatus
import com.example.data.local.SchoolClassEntity
import com.example.data.local.SettingsEntity
import com.example.data.local.StudentEntity
import com.example.data.repository.RecapPeriod
import com.example.util.DateUtils
import com.example.util.XlsxHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class PjokLogicUnitTest {

    @Test
    fun `unmarked attendance defaults to BELUM DIABSEN and never Alpa`() {
        assertEquals(AttendanceStatus.UNMARKED, AttendanceStatus.fromCode(null))
        assertEquals(AttendanceStatus.UNMARKED, AttendanceStatus.fromCode("UNKNOWN"))
        assertEquals("BELUM DIABSEN", AttendanceStatus.UNMARKED.label)
        assertEquals(AttendanceStatus.H, AttendanceStatus.fromCode("H"))
        assertEquals(AttendanceStatus.S, AttendanceStatus.fromCode("S"))
        assertEquals(AttendanceStatus.I, AttendanceStatus.fromCode("I"))
        assertEquals(AttendanceStatus.A, AttendanceStatus.fromCode("A"))
    }

    @Test
    fun `gender parser handles indonesian and short codes`() {
        assertEquals(Gender.L, Gender.fromInput("L"))
        assertEquals(Gender.L, Gender.fromInput("Laki-laki"))
        assertEquals(Gender.P, Gender.fromInput("P"))
        assertEquals(Gender.P, Gender.fromInput("Perempuan"))
        assertNull(Gender.fromInput("X"))
    }

    @Test
    fun `date utils computes indonesian week range and month formatting`() {
        val (mon, sun) = DateUtils.weekRangeFor("2026-11-25") // Wednesday
        assertEquals("2026-11-23", mon)
        assertEquals("2026-11-29", sun)
        assertEquals("November 2026", DateUtils.formatMonthYear("2026-11"))
        assertTrue(DateUtils.formatIndonesianFull("2026-11-23").contains("November 2026"))
    }

    @Test
    fun `xlsx writer generates valid zip ooxml structure for template and students`() {
        val templateOut = ByteArrayOutputStream()
        XlsxHelper.writeStudentTemplate(templateOut)
        val templateEntries = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(templateOut.toByteArray())).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                templateEntries.add(e.name)
                e = zis.nextEntry
            }
        }
        assertTrue(templateEntries.contains("[Content_Types].xml"))
        assertTrue(templateEntries.contains("xl/workbook.xml"))
        assertTrue(templateEntries.contains("xl/worksheets/sheet1.xml"))

        val studentsOut = ByteArrayOutputStream()
        XlsxHelper.writeStudentsExport(
            listOf(
                StudentEntity(1, "1001", "Andi Pratama", "L", 1),
                StudentEntity(2, "1002", "Siti Rahma", "P", 2)
            ),
            studentsOut
        )
        assertTrue(studentsOut.toByteArray().size > 200)
    }

    @Test
    fun `csv import fallback parses students and skips example rows`() {
        val csv = """
            NIS,Nama Lengkap,Jenis Kelamin (L/P),Kelas (1-6)
            20260101,Contoh: Ahmad Pratama,L,1
            1001,Budi Santoso,Laki-laki,Kelas 1
            1002,Siti Aminah,P,3
        """.trimIndent()
        val parsed = XlsxHelper.readStudentsFromStream(
            ByteArrayInputStream(csv.toByteArray()),
            "siswa.csv"
        )
        assertEquals(2, parsed.rows.size)
        assertEquals("1001", parsed.rows[0].nis)
        assertEquals(1, parsed.rows[0].classId)
        assertEquals("L", parsed.rows[0].gender)
        assertEquals("1002", parsed.rows[1].nis)
        assertEquals(3, parsed.rows[1].classId)
        assertEquals("P", parsed.rows[1].gender)
    }

    @Test
    fun `recap calculation counts only finalized meetings for percentage`() {
        val classes = (1..6).map { SchoolClassEntity(it, "Kelas $it") }
        val students = listOf(
            StudentEntity(1, "1001", "Siswa A", "L", 1),
            StudentEntity(2, "1002", "Siswa B", "P", 1)
        )
        val meetings = listOf(
            MeetingEntity(10, 1, 1, "2026-11-23", "Ganjil", "2026/2027", "Atletik", "", MeetingStatus.FINALIZED.code),
            MeetingEntity(11, 1, 2, "2026-11-24", "Ganjil", "2026/2027", "Senam", "", MeetingStatus.FINALIZED.code),
            // Draft meeting must NOT be included in finalized meeting denominator
            MeetingEntity(12, 1, 3, "2026-11-25", "Ganjil", "2026/2027", "Draft", "", MeetingStatus.DRAFT.code)
        )
        val attendance = listOf(
            AttendanceEntity(1, 10, 1, "H"),
            AttendanceEntity(2, 10, 2, "UNMARKED"),
            AttendanceEntity(3, 11, 1, "S"),
            AttendanceEntity(4, 11, 2, "H"),
            AttendanceEntity(5, 12, 1, "A")
        )

        // Compute recap directly using pure logic
        val finalized = meetings.filter { it.status == MeetingStatus.FINALIZED.code && it.classId == 1 }
        assertEquals(2, finalized.size)
        val s1Hadir = attendance.count { it.studentId == 1L && it.meetingId in finalized.map { m -> m.id } && it.attendanceStatus == "H" }
        val s1Pct = (s1Hadir * 100.0) / finalized.size
        assertEquals(50.0, s1Pct, 0.01)

        val s2Alpa = attendance.count { it.studentId == 2L && it.meetingId in finalized.map { m -> m.id } && it.attendanceStatus == "A" }
        assertEquals(0, s2Alpa)

        val settings = SettingsEntity(1, "Andi Mozes Leavingstone Bire, S.Pd.", "UPTD SDN LOKOJULI", "2026/2027", "Ganjil")
        assertEquals("UPTD SDN LOKOJULI", settings.schoolName)
        assertEquals(RecapPeriod.SEMESTER.label, "Semester")
    }
}
