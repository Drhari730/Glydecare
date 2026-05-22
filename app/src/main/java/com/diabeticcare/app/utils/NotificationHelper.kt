package com.diabeticcare.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.diabeticcare.app.DiabeticCareApp
import com.diabeticcare.app.MainActivity
import com.diabeticcare.app.R

object NotificationHelper {

    private const val CHANNEL_REMINDERS = "channel_reminders"
    private const val CHANNEL_ALERTS = "channel_alerts"

    private fun localizedContext(context: Context): Context {
        val lang = context.getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        return DiabeticCareApp.applyLocale(context.applicationContext, lang)
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = localizedContext(context)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    ctx.getString(R.string.notif_channel_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Medication, visit, hydration, activity, and sleep reminders" }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    ctx.getString(R.string.notif_channel_alerts),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "High/low glucose alerts" }
            )
        }
    }

    fun showMedicationReminder(context: Context, medName: String, notifId: Int) {
        val ctx = localizedContext(context)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.medication))
            .setContentText(ctx.getString(R.string.notif_med_reminder, medName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(notifId, notification) }
        catch (e: SecurityException) { /* no permission */ }
    }

    fun showGlucoseReminder(context: Context, notifId: Int = 1001) {
        val ctx = localizedContext(context)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.glucose))
            .setContentText(ctx.getString(R.string.notif_glucose_reminder))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(notifId, notification) }
        catch (e: SecurityException) { /* no permission */ }
    }

    fun showGlucoseAlert(context: Context, message: String, notifId: Int = 2001) {
        val ctx = localizedContext(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.glucose))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(notifId, notification) }
        catch (e: SecurityException) { /* no permission */ }
    }

    fun showHospitalVisitReminder(
        context: Context,
        message: String = "Check your upcoming hospital visits and tests.",
        notifId: Int = 7001
    ) {
        val ctx = localizedContext(context)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.hospital_visits))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(notifId, notification) }
        catch (e: SecurityException) { /* no permission */ }
    }

    fun showLifestyleReminder(context: Context, message: String, notifId: Int = 8100) {
        val ctx = localizedContext(context)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.lifestyle_reminder))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(notifId, notification) }
        catch (e: SecurityException) { /* no permission */ }
    }
}
