package com.diabeticcare.app.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn() = prefs.getBoolean(KEY_LOGGED_IN, false)
    fun setLoggedIn(value: Boolean) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    fun isProfileComplete() = prefs.getBoolean(KEY_PROFILE_COMPLETE, false)
    fun setProfileComplete(value: Boolean) = prefs.edit().putBoolean(KEY_PROFILE_COMPLETE, value).apply()

    fun savePhone(phone: String) = prefs.edit().putString(KEY_PHONE, phone).apply()
    fun getPhone() = prefs.getString(KEY_PHONE, "") ?: ""

    fun savePassword(pw: String) = prefs.edit().putString(KEY_PASSWORD, pw).apply()
    fun getPassword() = prefs.getString(KEY_PASSWORD, "") ?: ""

    fun saveName(name: String) = prefs.edit().putString(KEY_NAME, name).apply()
    fun getName() = prefs.getString(KEY_NAME, "") ?: ""

    fun saveDoctorId(doctorId: String) = prefs.edit().putString(KEY_DOCTOR_ID, doctorId).apply()
    fun getDoctorId() = prefs.getString(KEY_DOCTOR_ID, "MCH GM 001") ?: "MCH GM 001"

    fun setLanguage(lang: String) {
        val supported = if (lang == "kn") "kn" else "en"
        prefs.edit().putString(KEY_LANGUAGE, supported).apply()
    }
    fun getLanguage() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"

    fun setLowThreshold(value: Int) = prefs.edit().putInt(KEY_LOW_THRESHOLD, value).apply()
    fun getLowThreshold() = prefs.getInt(KEY_LOW_THRESHOLD, 70)

    fun setHighThreshold(value: Int) = prefs.edit().putInt(KEY_HIGH_THRESHOLD, value).apply()
    fun getHighThreshold() = prefs.getInt(KEY_HIGH_THRESHOLD, 180)

    fun setRemindersScheduled(value: Boolean) = prefs.edit().putBoolean(KEY_REMINDERS_SET, value).apply()
    fun areRemindersScheduled() = prefs.getBoolean(KEY_REMINDERS_SET, false)

    fun clearAll() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_PROFILE_COMPLETE = "profile_complete"
        private const val KEY_PHONE = "phone"
        private const val KEY_PASSWORD = "password"
        private const val KEY_NAME = "name"
        private const val KEY_DOCTOR_ID = "doctor_id"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_LOW_THRESHOLD = "low_threshold"
        private const val KEY_HIGH_THRESHOLD = "high_threshold"
        private const val KEY_REMINDERS_SET = "reminders_set"
    }
}
