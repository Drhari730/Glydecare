package com.diabeticcare.app.utils

import android.content.Context
import com.diabeticcare.app.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserSessionManager {
    suspend fun clearPatientDataIfAccountChanged(context: Context, nextPhone: String, prefs: PreferenceManager) {
        val previousPhone = prefs.getPhone()
        if (previousPhone.isNotBlank() && previousPhone != nextPhone) {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context).clearAllTables()
            }
            prefs.setRemindersScheduled(false)
        }
    }
}
