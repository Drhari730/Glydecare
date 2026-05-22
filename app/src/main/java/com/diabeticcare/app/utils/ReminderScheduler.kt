package com.diabeticcare.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReminderScheduler {

    private val lifestyleRequestCodes = listOf(8101, 8102, 8103, 8104, 8105)

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("glucose_reminder")
        workManager.cancelUniqueWork("medication_reminder")
        workManager.cancelUniqueWork("visit_reminder")

        scheduleDailyVisitAlarm(context)
        scheduleLifestyleAlarms(context)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        (listOf(7001) + lifestyleRequestCodes).forEach { requestCode ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun testFireNow(context: Context) {
        NotificationHelper.showHospitalVisitReminder(
            context,
            "Test visit reminder: notification sound and vibration should fire now.",
            7999
        )
        NotificationHelper.showLifestyleReminder(
            context,
            "Test lifestyle reminder: water, walk, sleep, or yoga alert is active.",
            8999
        )
    }

    fun scheduleMedicationAlarms(context: Context, medName: String, reminderTimes: String, seedId: Int) {
        reminderTimes.split(",")
            .map { it.trim() }
            .filter { it.matches(Regex("\\d{2}:\\d{2}")) }
            .forEachIndexed { index, time ->
                val parts = time.split(":")
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(Calendar.MINUTE, parts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                val requestCode = seedId * 10 + index
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("type", "MED_REMINDER")
                    putExtra("med_name", medName)
                    putExtra("notif_id", 6000 + requestCode)
                }
                scheduleRepeating(context, requestCode, cal.timeInMillis, intent)
            }
    }

    fun scheduleVisitAlarms(
        context: Context,
        visitIdSeed: Int,
        visitType: String,
        hospitalName: String,
        scheduledDate: Long
    ) {
        val visitLabel = visitType.replace("_", " ").lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val hospitalLabel = hospitalName.ifBlank { "your hospital" }
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateText = sdf.format(Date(scheduledDate))

        val dayBefore = Calendar.getInstance().apply {
            timeInMillis = scheduledDate
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sameDay = Calendar.getInstance().apply {
            timeInMillis = scheduledDate
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val reminders = listOf(
            Triple(0, dayBefore.timeInMillis, "$visitLabel tomorrow at $hospitalLabel. Please keep reports, medicines, and questions ready."),
            Triple(1, sameDay.timeInMillis, "$visitLabel today at $hospitalLabel ($dateText). Please leave on time and carry your glucose log.")
        )

        reminders
            .filter { it.second > System.currentTimeMillis() }
            .forEach { (offset, triggerAt, message) ->
                val requestCode = 7200 + kotlin.math.abs(visitIdSeed % 100000) * 10 + offset
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("type", "VISIT_REMINDER")
                    putExtra("message", message)
                    putExtra("notif_id", requestCode)
                }
                scheduleOneShot(context, requestCode, triggerAt, intent)
            }
    }

    private fun scheduleDailyVisitAlarm(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("type", "VISIT_REMINDER")
            putExtra("message", "Check your upcoming hospital visits, lab tests, and follow-up appointments.")
            putExtra("notif_id", 7001)
        }
        scheduleRepeating(context, 7001, cal.timeInMillis, intent)
    }

    private fun scheduleLifestyleAlarms(context: Context) {
        val items = listOf(
            Triple(8101, "06:30", "Morning routine: drink water, take a gentle walk or yoga if you feel well."),
            Triple(8102, "10:30", "Hydration check: drink water if your doctor has not restricted fluids."),
            Triple(8103, "14:30", "After-lunch activity: walk 10 to 15 minutes if safe."),
            Triple(8104, "18:30", "Evening routine: light activity, hydration, and foot check."),
            Triple(8105, "21:30", "Sleep routine: reduce screens and prepare for 7 to 8 hours of rest.")
        )
        items.forEach { (requestCode, time, message) ->
            val parts = time.split(":")
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("type", "LIFESTYLE_REMINDER")
                putExtra("message", message)
                putExtra("notif_id", requestCode)
            }
            scheduleRepeating(context, requestCode, cal.timeInMillis, intent)
        }
    }

    private fun scheduleRepeating(context: Context, requestCode: Int, firstAtMillis: Long, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            firstAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun scheduleOneShot(context: Context, requestCode: Int, triggerAtMillis: Long, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (security: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
