package com.diabeticcare.app.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.diabeticcare.app.data.database.AppDatabase
import java.util.Calendar

class GlucoseReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val message = when {
            hour in 6..9 -> "Good morning! Time to check your fasting blood glucose."
            hour in 11..13 -> "Post-lunch glucose check — log your reading now."
            hour in 19..21 -> "Evening glucose check — how are your levels today?"
            else -> "Time to check your blood glucose!"
        }
        NotificationHelper.showGlucoseAlert(applicationContext, message, 5001)
        return Result.success()
    }
}

class MedicationReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val meds = db.medicationDao().getActiveMedicationsSnapshot()
        meds.forEach { med ->
            NotificationHelper.showMedicationReminder(applicationContext, med.name, med.id + 6000)
        }
        return Result.success()
    }
}

class VisitReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val nextVisit = db.hospitalVisitDao().getNextUpcoming() ?: return Result.success()
        val daysUntil = ((nextVisit.scheduledDate - System.currentTimeMillis()) /
                (1000L * 60 * 60 * 24)).toInt()
        if (daysUntil in 0..3) {
            NotificationHelper.showGlucoseAlert(
                applicationContext,
                "Hospital visit in $daysUntil day(s): ${nextVisit.visitType.replace("_", " ")} at ${nextVisit.hospitalName}",
                7001
            )
        }
        return Result.success()
    }
}
