package com.diabeticcare.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.diabeticcare.app.data.database.dao.*
import com.diabeticcare.app.data.model.*

@Database(
    entities = [
        GlucoseReading::class,
        Medication::class,
        MedicationLog::class,
        MealLog::class,
        VitalsRecord::class,
        UserProfile::class,
        HbA1cRecord::class,
        HospitalVisit::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun glucoseDao(): GlucoseDao
    abstract fun medicationDao(): MedicationDao
    abstract fun mealDao(): MealDao
    abstract fun vitalsDao(): VitalsDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun hba1cDao(): HbA1cDao
    abstract fun hospitalVisitDao(): HospitalVisitDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diabeticcare_db"
                ).addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN doctorId TEXT NOT NULL DEFAULT 'MCH GM 001'")
            }
        }
    }
}
