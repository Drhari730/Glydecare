package com.diabeticcare.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.diabeticcare.app.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!PreferenceManager(context).isLoggedIn()) return

        val type = intent.getStringExtra("type") ?: return
        when (type) {
            "GLUCOSE_REMINDER" -> NotificationHelper.showGlucoseReminder(context)
            "MED_REMINDER" -> {
                val medName = intent.getStringExtra("med_name") ?: "Medication"
                val exists = runBlocking(Dispatchers.IO) {
                    AppDatabase.getInstance(context).medicationDao().countActiveByName(medName) > 0
                }
                if (!exists) return
                val notifId = intent.getIntExtra("notif_id", 3000)
                NotificationHelper.showMedicationReminder(context, medName, notifId)
            }
            "VISIT_REMINDER" -> {
                val hasUpcomingVisit = runBlocking(Dispatchers.IO) {
                    AppDatabase.getInstance(context).hospitalVisitDao().getNextUpcoming() != null
                }
                if (!hasUpcomingVisit) return
                val message = intent.getStringExtra("message") ?: "Check your upcoming hospital visits and tests."
                val notifId = intent.getIntExtra("notif_id", 7001)
                NotificationHelper.showHospitalVisitReminder(context, message, notifId)
            }
            "LIFESTYLE_REMINDER" -> {
                val message = intent.getStringExtra("message") ?: "Time for a healthy habit check."
                val notifId = intent.getIntExtra("notif_id", 8100)
                NotificationHelper.showLifestyleReminder(context, message, notifId)
            }
        }
    }
}
