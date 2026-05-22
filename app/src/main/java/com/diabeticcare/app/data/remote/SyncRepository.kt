package com.diabeticcare.app.data.remote

import android.util.Log
import com.diabeticcare.app.data.model.GlucoseReading
import com.diabeticcare.app.data.model.UserProfile

object SyncRepository {
    private const val TAG = "GlydecareSync"

    suspend fun syncPatient(profile: UserProfile): Boolean {
        val api = GlydecareApiClient.api ?: return false
        return runCatching {
            val response = api.upsertPatient(profile.toSyncRequest())
            response.isSuccessful && response.body()?.ok == true
        }.onFailure {
            Log.w(TAG, "Patient sync failed", it)
        }.getOrDefault(false)
    }

    suspend fun syncGlucose(profile: UserProfile, reading: GlucoseReading): Boolean {
        val api = GlydecareApiClient.api ?: return false
        return runCatching {
            val response = api.addGlucose(
                patientId(profile),
                GlucoseSyncRequest(
                    value = reading.value,
                    readingType = reading.readingType,
                    notes = reading.notes,
                    timestamp = reading.timestamp
                )
            )
            response.isSuccessful && response.body()?.ok == true
        }.onFailure {
            Log.w(TAG, "Glucose sync failed", it)
        }.getOrDefault(false)
    }

    fun patientId(profile: UserProfile): String =
        "${profile.doctorId}-${profile.phone}".trim()
            .replace(Regex("\\s+"), "-")
            .replace(Regex("[^A-Za-z0-9_-]"), "")

    private fun UserProfile.toSyncRequest(): PatientSyncRequest =
        PatientSyncRequest(
            id = patientId(this),
            doctorId = doctorId,
            name = name,
            phone = phone,
            age = age,
            gender = gender,
            diabetesType = diabetesType,
            diagnosisYear = diagnosisYear,
            doctorName = doctorName,
            lowGlucoseThreshold = lowGlucoseThreshold,
            highGlucoseThreshold = highGlucoseThreshold
        )
}
