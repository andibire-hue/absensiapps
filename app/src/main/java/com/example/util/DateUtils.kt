package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val indonesianLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()

    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    private val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private val dayNames = mapOf(
        Calendar.SUNDAY to "Minggu",
        Calendar.MONDAY to "Senin",
        Calendar.TUESDAY to "Selasa",
        Calendar.WEDNESDAY to "Rabu",
        Calendar.THURSDAY to "Kamis",
        Calendar.FRIDAY to "Jumat",
        Calendar.SATURDAY to "Sabtu"
    )

    fun todayIso(): String = isoFormat.format(Date())

    fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", y, m)
    }

    fun formatMillisToIso(millis: Long): String = isoFormat.format(Date(millis))

    fun parseIsoToMillis(iso: String): Long {
        return try {
            isoFormat.parse(iso)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun formatIndonesianFull(iso: String): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = parseIsoToMillis(iso)
        val dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = monthNames.getOrElse(cal.get(Calendar.MONTH)) { "" }
        val year = cal.get(Calendar.YEAR)
        return "$dayName, $day $month $year"
    }

    fun formatIndonesianShort(iso: String): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = parseIsoToMillis(iso)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = monthNames.getOrElse(cal.get(Calendar.MONTH)) { "" }.take(3)
        val year = cal.get(Calendar.YEAR)
        return "$day $month $year"
    }

    fun formatMonthYear(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return yearMonth
        val year = parts[0]
        val mIdx = (parts[1].toIntOrNull() ?: 1) - 1
        val month = monthNames.getOrElse(mIdx) { parts[1] }
        return "$month $year"
    }

    fun getMonthName(monthIndexZeroBased: Int): String =
        monthNames.getOrElse(monthIndexZeroBased) { "" }

    fun getAllMonthNames(): List<String> = monthNames

    /** Returns Monday..Sunday ISO date pair for the week containing [referenceIso]. */
    fun weekRangeFor(referenceIso: String): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = parseIsoToMillis(referenceIso)
        cal.firstDayOfWeek = Calendar.MONDAY
        val diffFromMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -diffFromMonday)
        val start = isoFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 6)
        val end = isoFormat.format(cal.time)
        return start to end
    }

    fun isIsoInMonth(iso: String, yearMonth: String): Boolean =
        iso.startsWith(yearMonth)

    fun defaultAcademicYear(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return if (month >= 7) "$year/${year + 1}" else "${year - 1}/$year"
    }

    fun defaultSemester(): String {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        return if (month >= 7) "Ganjil" else "Genap"
    }

    fun timestampFileSuffix(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", indonesianLocale).format(Date())
}
