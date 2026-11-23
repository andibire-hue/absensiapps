package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.util.DateUtils

@Database(
    entities = [
        SchoolClassEntity::class,
        StudentEntity::class,
        MeetingEntity::class,
        AttendanceEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun meetingDao(): MeetingDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "absensi_pjok_lokojuli.db"
                )
                    .addCallback(SeedCallback)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private object SeedCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedReferenceRows(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                seedReferenceRows(db)
            }

            private fun seedReferenceRows(db: SupportSQLiteDatabase) {
                for (cls in 1..6) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO classes (id, class_name) VALUES (?, ?)",
                        arrayOf<Any>(cls, "Kelas $cls")
                    )
                }
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO settings (id, teacher_name, school_name, academic_year, semester)
                    VALUES (1, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(
                        "Andi Mozes Leavingstone Bire, S.Pd.",
                        "UPTD SDN LOKOJULI",
                        DateUtils.defaultAcademicYear(),
                        DateUtils.defaultSemester()
                    )
                )
            }
        }
    }
}
